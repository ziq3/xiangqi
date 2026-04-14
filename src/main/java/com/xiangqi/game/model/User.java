package com.xiangqi.game.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String playerId;
    @Column(nullable = false, unique = true, length = 100)
    private String userName;
    @Column(nullable = false, length = 100)
    private String password;
}
