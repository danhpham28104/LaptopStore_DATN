package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.AiChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, Long> {

    /**
     * Lấy lịch sử chat theo conversationKey, sắp xếp theo thời gian tạo tăng dần.
     * Giới hạn 100 bản ghi gần nhất để tránh quá tải.
     */
    @Query("SELECT h FROM AiChatHistory h WHERE h.conversationKey = :key ORDER BY h.createdAt ASC")
    List<AiChatHistory> findByConversationKeyOrderByCreatedAtAsc(@Param("key") String conversationKey);

    /**
     * Lấy N bản ghi gần nhất theo conversationKey.
     */
    @Query(value = "SELECT * FROM ai_chat_history WHERE conversation_key = :key ORDER BY created_at ASC LIMIT :limit",
           nativeQuery = true)
    List<AiChatHistory> findRecentByConversationKey(@Param("key") String conversationKey, @Param("limit") int limit);

    /**
     * Xóa toàn bộ lịch sử theo conversationKey.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AiChatHistory h WHERE h.conversationKey = :key")
    void deleteByConversationKey(@Param("key") String conversationKey);

    /** Đếm số bản ghi theo conversationKey */
    long countByConversationKey(String conversationKey);

    /** Đếm số bản ghi theo role */
    long countByRole(String role);

    /** Lấy danh sách theo role mới nhất trước */
    List<AiChatHistory> findByRoleOrderByCreatedAtDesc(String role);

    /** Tính confidence score trung bình của các câu trả lời AI */
    @Query("SELECT AVG(h.confidenceScore) FROM AiChatHistory h WHERE h.role = 'assistant' AND h.confidenceScore IS NOT NULL")
    Double findAvgConfidenceScore();

    /** Đếm số câu hỏi có độ tin cậy thấp (< threshold, ví dụ < 0.7) */
    @Query("SELECT COUNT(h) FROM AiChatHistory h WHERE h.role = 'assistant' AND h.confidenceScore < :threshold")
    long countLowConfidenceQueries(@Param("threshold") double threshold);
}
