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
		TIMEOUT_GUEST
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


    public Room() {
        this.turn = Turn.HOST;
        this.status = Status.WAITING;
        this.fen = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
        this.guestName = "BOT";

        // 15 + 30 time control defaults (clock starts when room becomes PLAYING).
        this.clockBaseMs = 15L * 60L * 1000L;
        this.clockIncrementMs = 30L * 1000L;
        this.hostRemainingMs = this.clockBaseMs;
        this.guestRemainingMs = this.clockBaseMs;
        this.turnStartedAtEpochMs = 0L;
        this.endReason = null;
    }

    public boolean canJoin() {
        return guestName.equals("BOT");
    }
}
