package com.techstore.techstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu lịch sử chat AI theo conversationKey.
 * conversationKey = "USER:{userId}" nếu đã đăng nhập, hoặc "IP:{clientIp}" nếu chưa login.
 */
@Entity
@Table(name = "ai_chat_history", indexes = {
        @Index(name = "idx_ai_chat_conv_key", columnList = "conversation_key"),
        @Index(name = "idx_ai_chat_created", columnList = "created_at")
})
public class AiChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Key định danh cuộc hội thoại: "USER:{userId}" hoặc "IP:{clientIp}" */
    @Column(name = "conversation_key", nullable = false, length = 100)
    private String conversationKey;

    /** IP của client (nullable – chỉ điền khi chưa login) */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /** userId của user (nullable – chỉ điền khi đã login) */
    @Column(name = "user_id")
    private Long userId;

    /** "user" hoặc "assistant" */
    @Column(nullable = false, length = 20)
    private String role;

    /** Nội dung tin nhắn (câu hỏi hoặc câu trả lời text) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** JSON đầy đủ của response từ RAG (chỉ dùng cho role=assistant) */
    @Column(name = "response_json", columnDefinition = "LONGTEXT")
    private String responseJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiChatHistory() {}

    // ─── Getters & Setters ─────────────────────────────────────────────
    public Long getId() { return id; }

    public String getConversationKey() { return conversationKey; }
    public void setConversationKey(String conversationKey) { this.conversationKey = conversationKey; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
