package com.fastcam.spserver.dto;

import lombok.Data;

@Data
public class RequestDto {
    private String sessionId;
    private String message;
}
