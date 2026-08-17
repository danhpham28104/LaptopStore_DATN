package com.techstore.techstore.dto;

import java.time.LocalDateTime;

/**
 * DTO trả về từng bản ghi lịch sử chat cho frontend.
 * Không lộ clientIp ra ngoài (chỉ trả role, message, responseJson, createdAt).
 */
public class ChatHistoryItemDto {

    private Long id;
    private String role;       // "user" hoặc "assistant"
    private String message;    // câu hỏi hoặc câu trả lời text
    private String responseJson; // JSON đầy đủ (chỉ có với role=assistant)
    private LocalDateTime createdAt;

    public ChatHistoryItemDto() {}

    public ChatHistoryItemDto(Long id, String role, String message,
                               String responseJson, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.message = message;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
