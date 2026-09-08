package com.fastcam.spserver.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ChatList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String sessionId;
}
