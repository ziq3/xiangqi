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
    private String moveHistory;

    public Room() {
        this.turn = Turn.HOST;
        this.status = Status.WAITING;
        this.moveHistory = "";
    }

    public boolean canJoin() {
        return guestName == null || guestName.isBlank();
    }
}
