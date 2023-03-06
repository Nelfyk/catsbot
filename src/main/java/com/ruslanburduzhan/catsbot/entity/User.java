package com.ruslanburduzhan.catsbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Time;

@Entity(name = "users")
@Data
public class User {
    @Id
    private long chatId;
    private Time schedulerTime;
    private String text;
    private Filter filter;

    public User() {

    }

    public User(long chatId) {
        this.chatId = chatId;
    }
}
