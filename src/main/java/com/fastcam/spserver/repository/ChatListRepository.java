package com.fastcam.spserver.repository;

import com.fastcam.spserver.entity.ChatList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatListRepository extends JpaRepository<ChatList, Integer> {
    ChatList findBySessionId(String sessionId);
}
