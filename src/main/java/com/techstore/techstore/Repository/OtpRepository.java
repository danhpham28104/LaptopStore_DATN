package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Tìm OTP gần nhất (chưa hết hạn) cho một số điện thoại
     */
    @Query("""
        SELECT o FROM OtpVerification o 
        WHERE o.phone = :phone 
        AND o.status = 'SENT'
        ORDER BY o.createdAt DESC 
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestByPhone(@Param("phone") String phone);

    /**
     * Tìm OTP đã xác thực gần nhất
     */
    @Query("""
        SELECT o FROM OtpVerification o 
        WHERE o.phone = :phone 
        AND o.status = 'VERIFIED'
        ORDER BY o.verifiedAt DESC 
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestVerifiedByPhone(@Param("phone") String phone);

    /**
     * Tìm tất cả OTP hết hạn để dọn dẹp
     */
    @Query("""
        SELECT o FROM OtpVerification o 
        WHERE o.expiryTime < :now
    """)
    List<OtpVerification> findExpiredOtps(@Param("now") LocalDateTime now);

    /**
     * Đếm OTP gần đây của số điện thoại (để giới hạn gửi lại)
     */
    @Query("""
        SELECT COUNT(o) FROM OtpVerification o 
        WHERE o.phone = :phone 
        AND o.createdAt >= :fromTime
    """)
    long countRecentOtpsByPhone(@Param("phone") String phone, @Param("fromTime") LocalDateTime fromTime);
}
