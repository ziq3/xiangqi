package com.xiangqi.game.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.xiangqi.game.dto.EngineAnalysis;

import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class EngineService {

    private static final Logger logger = LoggerFactory.getLogger(EngineService.class);

    private final String enginePath;
    private final Map<String, AnalysisSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();

    private static class AnalysisSession {
        final Process process;
        final BufferedReader reader;
        final BufferedWriter writer;
        volatile SseEmitter currentEmitter;

        AnalysisSession(Process process, BufferedReader reader, BufferedWriter writer) {
            this.process = process;
            this.reader = reader;
            this.writer = writer;
        }
    }

    public EngineService() {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        String exePath = isWindows ? "engine/pikafish-bmi2.exe" : "engine/pikafish";
        File exeFile = new File(exePath);

        if (!exeFile.exists()) {
            throw new RuntimeException("Could not find executable at: " + exeFile.getAbsolutePath());
        }

        // Ensure the binary has execute permissions on Linux
        if (!isWindows) {
            try {
                exeFile.setExecutable(true);
            } catch (Exception e) {
                logger.warn("Could not set executable flag on {}", exeFile.getAbsolutePath(), e);
            }
        }

        this.enginePath = exeFile.getAbsolutePath();
    }

    private String normalizeFen(String fen) {
        if (fen != null) {
            String[] tokens = fen.split("\\s+");
            if (tokens.length >= 2 && tokens[1].equals("r")) {
                tokens[1] = "w";
                return String.join(" ", tokens);
            }
        }
        return fen;
    }

    public record EngineMoveResult(String fen, String move) {
    }

    public EngineMoveResult getFenAfterBestMove(String fen) {
        fen = normalizeFen(fen);
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(enginePath);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {

                writer.write("position fen " + fen + "\n");
                writer.write("go movetime 500\n");
                writer.flush();

                String bestMove = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("bestmove")) {
                        String[] parts = line.split(" ");
                        if (parts.length >= 2) {
                            bestMove = parts[1];
                        }
                        break;
                    }
                }

                if (bestMove != null && !bestMove.equals("(none)")) {
                    writer.write("position fen " + fen + " moves " + bestMove + "\n");
                    writer.write("d\n");
                    writer.write("quit\n");
                    writer.flush();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Fen: ")) {
                            return new EngineMoveResult(line.substring(5).trim(), bestMove);
                        }
                    }
                } else {
                    writer.write("quit\n");
                    writer.flush();
                    if ("(none)".equals(bestMove)) {
                        return new EngineMoveResult(fen, "(none)");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error communicating with engine for FEN {}", fen, e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
        return null;
    }

    public SseEmitter streamAnalysis(String fen, String sessionId) {
        fen = normalizeFen(fen);

        // Cancel any pending cleanup for this session
        ScheduledFuture<?> pendingCleanup = cleanupTasks.remove(sessionId);
        if (pendingCleanup != null) {
            pendingCleanup.cancel(false);
        }

        AnalysisSession session = sessions.get(sessionId);
        SseEmitter emitter = new SseEmitter(0L);

        if (session == null) {
            // Start a new process
            Process process;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(enginePath);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start engine process", e);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            final AnalysisSession newSession = new AnalysisSession(process, reader, writer);
            newSession.currentEmitter = emitter;

            new Thread(() -> {
                try {
                    while (true) {
                        // Non-blocking check: is there engine output to read?
                        if (reader.ready()) {
                            String line = reader.readLine();
                            if (line == null) break; // process ended

                            if (line.startsWith("info ")) {
                                String[] parts = line.split(" ");
                                Integer scoreCp = null;
                                Integer mate = null;
                                String bestMove = null;

                                for (int i = 0; i < parts.length; i++) {
                                    if (parts[i].equals("score") && i + 2 < parts.length) {
                                        if (parts[i + 1].equals("cp")) {
                                            try {
                                                scoreCp = Integer.parseInt(parts[i + 2]);
                                            } catch (Exception ignored) {
                                            }
                                        } else if (parts[i + 1].equals("mate")) {
                                            try {
                                                mate = Integer.parseInt(parts[i + 2]);
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    } else if (parts[i].equals("pv") && i + 1 < parts.length) {
                                        bestMove = parts[i + 1];
                                        break;
                                    }
                                }

                                if (scoreCp != null || mate != null) {
                                    EngineAnalysis analysisObject = new EngineAnalysis(scoreCp, mate, bestMove);
                                    SseEmitter activeEmitter = newSession.currentEmitter;
                                    if (activeEmitter != null) {
                                        try {
                                            activeEmitter.send(SseEmitter.event().data(analysisObject));
                                        } catch (Exception e) {
                                            newSession.currentEmitter = null;
                                            cleanupSession(sessionId);
                                            return;
                                        }
                                    }
                                }
                            }
                        } else {
                            // Engine idle (e.g. stalemate — no output after bestmove).
                            // Ping the SSE connection to detect closed tabs.
                            SseEmitter activeEmitter = newSession.currentEmitter;
                            if (activeEmitter != null) {
                                try {
                                    activeEmitter.send(SseEmitter.event().comment("ping"));
                                } catch (Exception e) {
                                    newSession.currentEmitter = null;
                                    cleanupSession(sessionId);
                                    return;
                                }
                            }
                            Thread.sleep(2000);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Engine read error in streamAnalysis thread for session {}", sessionId, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    cleanupSession(sessionId);
                }
            }).start();

            sessions.put(sessionId, newSession);
            session = newSession;
        } else {
            // Re-use existing process, complete old emitter
            SseEmitter oldEmitter = session.currentEmitter;
            if (oldEmitter != null) {
                try {
                    oldEmitter.complete();
                } catch (Exception ignored) {
                }
            }
            session.currentEmitter = emitter;
        }

        // Setup emitter cleanup callback (on timeout, error, completion)
        final AnalysisSession activeSession = session;
        Runnable onDisconnect = () -> {
            if (activeSession.currentEmitter == emitter) {
                activeSession.currentEmitter = null;
                // Schedule process cleanup in 5 seconds
                ScheduledFuture<?> future = scheduler.schedule(() -> {
                    cleanupSession(sessionId);
                }, 5, TimeUnit.SECONDS);
                cleanupTasks.put(sessionId, future);
            }
        };

        emitter.onCompletion(onDisconnect);
        emitter.onTimeout(onDisconnect);
        emitter.onError(e -> onDisconnect.run());

        // Send commands to engine
        synchronized (session) {
            try {
                session.writer.write("stop\n");
                session.writer.write("setoption name Hash value 16\n");
                session.writer.write("setoption name Threads value 1\n");
                session.writer.write("position fen " + fen + "\n");
                session.writer.write("go infinite\n");
                session.writer.flush();
            } catch (IOException e) {
                cleanupSession(sessionId);
                emitter.completeWithError(e);
            }
        }

        return emitter;
    }

    private void cleanupSession(String sessionId) {
        AnalysisSession session = sessions.remove(sessionId);
        cleanupTasks.remove(sessionId);
        if (session != null) {
            try {
                session.writer.write("quit\n");
                session.writer.flush();
            } catch (Exception ignored) {
            }
            try {
                session.writer.close();
            } catch (Exception ignored) {
            }
            try {
                session.reader.close();
            } catch (Exception ignored) {
            }
            session.process.destroyForcibly();
        }
    }

    @PreDestroy
    public void cleanup() {
        for (String sessionId : sessions.keySet()) {
            cleanupSession(sessionId);
        }
        scheduler.shutdownNow();
    }
}
