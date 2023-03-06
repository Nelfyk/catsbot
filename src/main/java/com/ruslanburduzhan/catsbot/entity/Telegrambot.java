package com.ruslanburduzhan.catsbot.entity;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class Telegrambot {
    @Value("${bot.name}")
    private String name;
    @Value("${bot.key}")
    private String key;
    @Value("${bot.landlord}")
    private String landlord;
}
