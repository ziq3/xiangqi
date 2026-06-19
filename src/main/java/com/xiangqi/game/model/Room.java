package com.xiangqi.game.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room {

    public enum Turn {
        HOST,
        GUEST
    }

    public enum Status {
        WAITING,
        PLAYING,
        FINISHED
    }

    public enum EndReason {
        TIMEOUT_HOST,
        TIMEOUT_GUEST,
        CHECKMATE_HOST,
        CHECKMATE_GUEST
    }

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String roomId;

    @Column(nullable = false, length = 100)
    private String hostName;

    @Column(length = 100)
    private String guestName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Turn turn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false, length = 4096)
    private String fen;

    @Column(length = 4096)
    private String moveHistory;

    @Column(nullable = false)
    private long hostRemainingMs;

    @Column(nullable = false)
    private long guestRemainingMs;

    @Column(nullable = false)
    private long clockBaseMs;

    @Column(nullable = false)
    private long clockIncrementMs;

    /**
     * Epoch millis when the current player's turn started.
     * 0 means the clock hasn't started yet (e.g. WAITING).
     */
    @Column(nullable = false)
    private long turnStartedAtEpochMs;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private EndReason endReason;

    @Column(updatable = false, length = 36)
    private String hostId; // Supabase UUID (sub claim)
    @Column(length = 36)
    private String guestId; // Supabase UUID (sub claim)

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean botGame;

    /** True once this player has clicked "Sẵn sàng" (PvP rooms only). */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hostReady;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean guestReady;

    public Room() {
        this.turn = Turn.HOST;
        this.status = Status.WAITING;
        this.fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
        this.guestName = null;
        this.botGame = false;
        this.hostReady = false;
        this.guestReady = false;

        // 15 + 30 time control defaults (clock starts when room becomes PLAYING).
        this.clockBaseMs = 15L * 60L * 1000L;
        this.clockIncrementMs = 30L * 1000L;
        this.hostRemainingMs = this.clockBaseMs;
        this.guestRemainingMs = this.clockBaseMs;
        this.turnStartedAtEpochMs = 0L;
        this.endReason = null;
        this.moveHistory = "";
    }

    public boolean canJoin() {
        return this.status == Status.WAITING && this.guestName == null;
    }
}
