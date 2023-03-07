package com.ruslanburduzhan.catsbot.entity;

import com.ruslanburduzhan.catsbot.service.TelegrambotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

//@Configuration
//@EnableScheduling
public class Scheduler implements SchedulingConfigurer {
    @Autowired
    private TelegrambotService telegrambotService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
       taskRegistrar.addTriggerTask(new Runnable() {
           @Override
           public void run() {
               System.out.println(Instant.now());
           }
       }, triggerContext -> {
           Optional<Date> lastCompletionTime =
                   Optional.ofNullable(triggerContext.lastCompletionTime());
           Instant nextExecutionTime =
                   lastCompletionTime.orElseGet(Date::new).toInstant()
                           .plusMillis(3000);
           return nextExecutionTime;
       });
    }
}
