package com.laptopstore.laptopstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * OTP Verification Entity - Lưu thông tin xác thực OTP
 * Dùng để xác thực khách hàng trước khi đặt hàng hoặc thanh toán
 */
@Entity
@Table(name = "otp_verification")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Số điện thoại cần xác thực
    @Column(length = 20, nullable = false)
    private String phone;

    // 🔹 OTP đã hash (không lưu plain text)
    @Column(nullable = false, length = 255)
    private String otpHash;

    // 🔹 Thời gian hết hạn OTP
    @Column(nullable = false)
    private LocalDateTime expiryTime;

    // 🔹 Số lần nhập sai (để chống brute-force)
    @Column(nullable = false)
    private Integer attemptCount = 0;

    // 🔹 Trạng thái OTP: SENT, VERIFIED, EXPIRED, FAILED
    @Column(length = 20, nullable = false)
    private String status = "SENT";

    // 🔹 Thời điểm tạo OTP
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 🔹 Thời điểm lần cuối gửi lại OTP (để giới hạn resend)
    @Column
    private LocalDateTime lastResendAt;

    // 🔹 Thời điểm xác thực thành công
    @Column
    private LocalDateTime verifiedAt;

    // ===== Constructors =====
    public OtpVerification() {}

    public OtpVerification(String phone, String otpHash, LocalDateTime expiryTime) {
        this.phone = phone;
        this.otpHash = otpHash;
        this.expiryTime = expiryTime;
        this.status = "SENT";
        this.attemptCount = 0;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastResendAt() { return lastResendAt; }
    public void setLastResendAt(LocalDateTime lastResendAt) { this.lastResendAt = lastResendAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    // ===== Helper Methods =====
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }

    public void incrementAttempt() {
        this.attemptCount = (this.attemptCount != null ? this.attemptCount : 0) + 1;
    }

    public boolean isMaxAttemptsReached(int maxAttempts) {
        return this.attemptCount != null && this.attemptCount >= maxAttempts;
    }

    @Override
    public String toString() {
        return "OtpVerification{" +
                "id=" + id +
                ", phone='" + phone + '\'' +
                ", status='" + status + '\'' +
                ", attemptCount=" + attemptCount +
                ", expiryTime=" + expiryTime +
                ", createdAt=" + createdAt +
                '}';
    }
}
