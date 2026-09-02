package com.laptopstore.laptopstore.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstore.laptopstore.Service.AiChatHistoryService;
import com.laptopstore.laptopstore.Service.RagIntegrationService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.dto.ChatHistoryItemDto;
import com.laptopstore.laptopstore.dto.ChatRequestDto;
import com.laptopstore.laptopstore.dto.ChatResponseDto;
import com.laptopstore.laptopstore.entity.AiChatHistory;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller xử lý yêu cầu chat AI từ frontend.
 * <p>
 * Endpoint chính: POST /api/ai/chat
 * Endpoint history: GET /api/ai/history | DELETE /api/ai/history
 * <p>
 * CSRF đã bỏ qua cho /api/** trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/ai")
public class RagChatController {

    private static final Logger log = LoggerFactory.getLogger(RagChatController.class);

    private final RagIntegrationService ragIntegrationService;
    private final AiChatHistoryService aiChatHistoryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final com.laptopstore.laptopstore.Service.AnalyticsEventService analyticsEventService;

    public RagChatController(RagIntegrationService ragIntegrationService,
                              AiChatHistoryService aiChatHistoryService,
                              UserService userService,
                              ObjectMapper objectMapper,
                              com.laptopstore.laptopstore.Service.AnalyticsEventService analyticsEventService) {
        this.ragIntegrationService = ragIntegrationService;
        this.aiChatHistoryService = aiChatHistoryService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.analyticsEventService = analyticsEventService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/ai/chat
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Nhận câu hỏi từ frontend, lưu lịch sử, gọi RAG, lưu response, trả về frontend.
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequestDto request,
                                  HttpServletRequest httpRequest,
                                  Principal principal) {

        // Validate message
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            log.warn("[RAG] Yêu cầu bị từ chối: message rỗng.");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tin nhắn không được để trống."));
        }

        // Lấy conversationKey + metadata
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        Long userId = null;
        String conversationKey;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                userId = user.getId();
                conversationKey = "USER:" + userId;
            } else {
                conversationKey = "IP:" + clientIp;
            }
        } else {
            conversationKey = "IP:" + clientIp;
        }

        // Đảm bảo topK hợp lệ
        if (request.getTopK() == null || request.getTopK() <= 0) {
            request.setTopK(5);
        }

        // Dùng conversationKey làm sessionId cho RAG (giúp RAG giữ context theo user/IP)
        request.setSessionId(conversationKey);

        log.info("[RAG] Chat | key={} | message={}", conversationKey, request.getMessage());

        // 🔹 Track AI_CHAT event
        String httpSessionId = com.laptopstore.laptopstore.Service.AnalyticsEventService.extractSessionId(httpRequest);
        analyticsEventService.trackAiChat(httpSessionId != null ? httpSessionId : conversationKey, userId, clientIp);

        // Lưu câu hỏi user vào DB (không block nếu lỗi)
        aiChatHistoryService.saveUserMessage(conversationKey, clientIp, userId, request.getMessage());

        // Gọi RAG service
        ChatResponseDto response = ragIntegrationService.chat(request);

        // Lưu response assistant vào DB
        String responseJson = null;
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("[RAG] Không thể serialize response thành JSON: {}", e.getMessage());
        }
        Double confidence = response != null ? response.getConfidenceScore() : null;
        aiChatHistoryService.saveAssistantMessage(
                conversationKey, clientIp, userId,
                response != null ? response.getAnswer() : "", responseJson, confidence
        );

        // 🔹 Track AI_PRODUCT_RECOMMENDED nếu RAG trả về gợi ý sản phẩm
        if (response != null && response.getRecommendedProducts() != null && !response.getRecommendedProducts().isEmpty()) {
            try {
                List<Long> prodIds = response.getRecommendedProducts().stream()
                        .map(p -> p.getId())
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
                if (!prodIds.isEmpty()) {
                    String idsJson = objectMapper.writeValueAsString(prodIds);
                    analyticsEventService.trackAiProductRecommended(
                            httpSessionId != null ? httpSessionId : conversationKey,
                            userId, idsJson);
                }
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/ai/history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy lịch sử chat theo conversationKey của request hiện tại.
     * Trả về danh sách ChatHistoryItemDto (không lộ IP).
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryItemDto>> getHistory(HttpServletRequest httpRequest,
                                                                Principal principal) {
        String conversationKey = resolveConversationKey(httpRequest);
        List<AiChatHistory> history = aiChatHistoryService.getHistory(conversationKey);

        List<ChatHistoryItemDto> dtos = history.stream()
                .map(h -> new ChatHistoryItemDto(
                        h.getId(),
                        h.getRole(),
                        h.getMessage(),
                        h.getResponseJson(),
                        h.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/ai/history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xóa toàn bộ lịch sử chat theo conversationKey của request hiện tại.
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory(HttpServletRequest httpRequest,
                                                             Principal principal) {
        String conversationKey = resolveConversationKey(httpRequest);
        aiChatHistoryService.clearHistory(conversationKey);
        log.info("[RAG] Đã xóa lịch sử cho key={}", conversationKey);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa lịch sử chat."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private String resolveConversationKey(HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                return "USER:" + user.getId();
            }
        }
        String clientIp = ClientIpUtil.getClientIp(httpRequest);
        return "IP:" + clientIp;
    }
}
