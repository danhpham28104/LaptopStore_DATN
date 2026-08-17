package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.AiChatHistoryRepository;
import com.techstore.techstore.entity.AiChatHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service quản lý lịch sử chat AI.
 * Mọi thao tác lưu/xóa lịch sử đều qua service này để dễ mở rộng sau này.
 */
@Service
public class AiChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(AiChatHistoryService.class);

    /** Giới hạn số bản ghi lịch sử trả về để không quá tải */
    private static final int MAX_HISTORY_RECORDS = 100;

    private final AiChatHistoryRepository repository;

    public AiChatHistoryService(AiChatHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Lưu tin nhắn của user vào lịch sử.
     */
    public void saveUserMessage(String conversationKey, String clientIp, Long userId, String message) {
        try {
            AiChatHistory entry = new AiChatHistory();
            entry.setConversationKey(conversationKey);
            entry.setClientIp(clientIp);
            entry.setUserId(userId);
            entry.setRole("user");
            entry.setMessage(message);
            repository.save(entry);
        } catch (Exception e) {
            log.error("[AiChatHistory] Lỗi khi lưu message user (key={}): {}", conversationKey, e.getMessage());
        }
    }

    /**
     * Lưu phản hồi của assistant vào lịch sử.
     *
     * @param answer      Câu trả lời text (hiển thị với user)
     * @param responseJson JSON đầy đủ của response từ RAG (để restore UI sau)
     */
    public void saveAssistantMessage(String conversationKey, String clientIp, Long userId,
                                     String answer, String responseJson) {
        try {
            AiChatHistory entry = new AiChatHistory();
            entry.setConversationKey(conversationKey);
            entry.setClientIp(clientIp);
            entry.setUserId(userId);
            entry.setRole("assistant");
            entry.setMessage(answer != null ? answer : "");
            entry.setResponseJson(responseJson);
            repository.save(entry);
        } catch (Exception e) {
            log.error("[AiChatHistory] Lỗi khi lưu response assistant (key={}): {}", conversationKey, e.getMessage());
        }
    }

    /**
     * Lấy lịch sử chat theo conversationKey (giới hạn MAX_HISTORY_RECORDS bản ghi gần nhất).
     */
    public List<AiChatHistory> getHistory(String conversationKey) {
        try {
            return repository.findRecentByConversationKey(conversationKey, MAX_HISTORY_RECORDS);
        } catch (Exception e) {
            log.error("[AiChatHistory] Lỗi khi lấy lịch sử (key={}): {}", conversationKey, e.getMessage());
            return List.of();
        }
    }

    /**
     * Xóa toàn bộ lịch sử chat theo conversationKey.
     */
    public void clearHistory(String conversationKey) {
        try {
            repository.deleteByConversationKey(conversationKey);
            log.info("[AiChatHistory] Đã xóa lịch sử cho key={}", conversationKey);
        } catch (Exception e) {
            log.error("[AiChatHistory] Lỗi khi xóa lịch sử (key={}): {}", conversationKey, e.getMessage());
        }
    }
}
