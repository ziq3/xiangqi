package com.xiangqi.game.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class EngineService {

    private static final Logger logger = LoggerFactory.getLogger(EngineService.class);

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public EngineService() {
        File exeFile = new File("engine/pikafish-bmi2.exe");
        if (!exeFile.exists()) {
            throw new RuntimeException("Could not find executable at: " + exeFile.getAbsolutePath());
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(exeFile.getAbsolutePath());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

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
