package com.xiangqi.game.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.xiangqi.game.dto.EngineAnalysis;

import java.io.*;

@Service
public class EngineService {

    private static final Logger logger = LoggerFactory.getLogger(EngineService.class);

    private final String enginePath;

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
                }
            }
        } catch (IOException e) {
            logger.error("Error communicating with engine for FEN {}", fen, e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    public SseEmitter streamAnalysis(String fen) {
        fen = normalizeFen(fen);
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
        SseEmitter emitter = new SseEmitter(0L);

        Runnable cleanup = () -> {
            try {
                writer.write("quit\n");
                writer.flush();
            } catch (Exception ignored) {
            }
            try {
                writer.close();
            } catch (Exception ignored) {
            }
            try {
                reader.close();
            } catch (Exception ignored) {
            }
            process.destroy();
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            writer.write("position fen " + fen + "\n");
            writer.write("go infinite\n");
            writer.flush();
        } catch (IOException e) {
            cleanup.run();
            emitter.completeWithError(e);
            return emitter;
        }

        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
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
                            try {
                                emitter.send(SseEmitter.event().data(analysisObject));
                            } catch (Exception e) {
                                logger.warn("Failed to send analysis data to emitter: {}", e.getMessage());
                                cleanup.run();
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Engine read error in streamAnalysis thread", e);
            } finally {
                cleanup.run();
            }
        }).start();

        return emitter;
    }
}
