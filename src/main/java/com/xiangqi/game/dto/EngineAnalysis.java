package com.xiangqi.game.dto;

public record EngineAnalysis(
        Integer scoreCp,
        Integer mate,
        String bestMove) {
}
