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

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    SseEmitter currentEmitter;

    public EngineService() {
        String osName = System.getProperty("os.name").toLowerCase();
        String exePath = osName.contains("win") ? "engine/pikafish-bmi2.exe" : "engine/pikafish";
        File exeFile = new File(exePath);

        if (!exeFile.exists()) {
            throw new RuntimeException("Could not find executable at: " + exeFile.getAbsolutePath());
        }
        // Ensure the binary has execute permissions on Linux
        if (!osName.contains("win")) {
            exeFile.setExecutable(true);
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(exeFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("info ")) {
                            String[] parts = line.split(" ");
                            Integer scoreCp = null;
                            Integer mate = null;
                            int depth = 0;
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
                                    bestMove = parts[i + 1]; // Just grab the very first move!
                                    break; // We don't care about the rest of the line
                                }
                            }

                            // Only send if we found the data we need
                            if ((scoreCp != null || mate != null) && bestMove != null) {
                                EngineAnalysis analysisObject = new EngineAnalysis(scoreCp, mate, bestMove);

                                if (currentEmitter != null) {
                                    try {
                                        currentEmitter.send(SseEmitter.event().data(analysisObject));
                                    } catch (IOException e) {
                                        currentEmitter = null;
                                        sendCommand("stop");
                                    }
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    logger.error("Engine read error", e);
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start engine process", e);
        }

    }

    private void sendCommand(String command) throws IOException {
        writer.write(command + "\n");
        writer.flush();
    }

    public String getFenAfterBestMove(String fen) {
        try {
            sendCommand("position fen " + fen);
            sendCommand("go movetime " + 500);

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
                sendCommand("position fen " + fen + " moves " + bestMove);
                sendCommand("d");

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Fen: ")) {
                        return line.substring(5).trim();
                    }
                    if (line.startsWith("Checkers:")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error communicating with engine for FEN {}", fen, e);
        }
        return null;
    }

    public SseEmitter streamAnalysis(String fen) {
        if (this.currentEmitter != null) {
            this.currentEmitter.complete(); // close old connection
        }

        // 2. Create a new emitter (e.g., 0 means no timeout)
        SseEmitter emitter = new SseEmitter(0L);
        this.currentEmitter = emitter;

        // 3. Handle client disconnects (when they close the browser or toggle off)
        Runnable onDisconnect = () -> {
            if (this.currentEmitter == emitter) {
                this.currentEmitter = null;
                try {
                    sendCommand("stop");
                } catch (Exception e) {
                }
            }
        };

        emitter.onCompletion(onDisconnect);
        emitter.onTimeout(onDisconnect);
        emitter.onError(e -> onDisconnect.run());
        // 4. Send commands to engine
        try {
            sendCommand("stop"); // Ensure engine is idle
            sendCommand("position fen " + fen);
            sendCommand("go infinite"); // Tell engine to think forever
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PreDestroy
    public void cleanup() {
        if (process != null) {
            try {
                sendCommand("quit");
            } catch (IOException e) {
            }
            process.destroy();
        }
    }
}
