package com.ruslanburduzhan.catsbot.repository;

import com.ruslanburduzhan.catsbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    @Query(value = "select text from users where chat_id=:chatId",nativeQuery = true)
    Optional<String> findTextByChatId(long chatId);
}
