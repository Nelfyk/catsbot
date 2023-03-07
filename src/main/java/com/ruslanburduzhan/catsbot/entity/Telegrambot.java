package com.ruslanburduzhan.catsbot.entity;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Data
public class Telegrambot {
    @Value("${bot.name}")
    private String name;
    @Value("${bot.key}")
    private String key;
    @Value("${bot.landlord}")
    private String landlord;
}
