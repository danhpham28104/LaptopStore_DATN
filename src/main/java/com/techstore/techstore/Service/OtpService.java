package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.OtpRepository;
import com.techstore.techstore.entity.OtpVerification;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * OTP Service - Quản lý OTP xác thực
 * 
 * Chức năng:
 * - Tạo OTP ngẫu nhiên
 * - Gửi OTP qua SMS
 * - Xác thực OTP
 * - Kiểm tra hết hạn, số lần nhập sai
 */
@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 🔹 Lấy từ application.properties
    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${otp.resend-wait-seconds:60}")
    private int otpResendWaitSeconds;

    // ===== PUBLIC METHODS =====

    /**
     * Tạo và gửi OTP mới cho số điện thoại
     * 
     * @param phone Số điện thoại người dùng
     * @throws RuntimeException nếu gửi OTP lại quá nhanh
     */
    @Transactional
    public void sendOtp(String phone) {
        // 🔹 Kiểm tra có OTP gần đây chưa
        Optional<OtpVerification> existingOtp = otpRepository.findLatestByPhone(phone);
        
        if (existingOtp.isPresent()) {
            OtpVerification existing = existingOtp.get();
            
            // Nếu còn hạn và không hết hạn, không cho gửi lại ngay
            if (!existing.isExpired()) {
                LocalDateTime lastResend = existing.getLastResendAt();
                LocalDateTime now = LocalDateTime.now();
                
                if (lastResend != null && 
                    now.isBefore(lastResend.plusSeconds(otpResendWaitSeconds))) {
                    long secondsWait = otpResendWaitSeconds - 
                        java.time.temporal.ChronoUnit.SECONDS.between(lastResend, now);
                    throw new RuntimeException("Vui lòng chờ " + secondsWait + 
                        " giây trước khi gửi lại OTP");
                }
            }
        }

        // 🔹 Tạo OTP mới
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        OtpVerification otpRecord = new OtpVerification(phone, otpHash, expiryTime);
        otpRecord.setLastResendAt(LocalDateTime.now());
        otpRepository.save(otpRecord);

        // 🔹 Gửi SMS
        smsService.sendOtpSms(phone, otp);
        
        logger.info("OTP sent to phone: {}", phone);
    }

    /**
     * Xác thực OTP nhập vào
     * 
     * @param phone Số điện thoại
     * @param otpInput OTP người dùng nhập vào
     * @return true nếu OTP đúng và chưa hết hạn
     * @throws RuntimeException nếu OTP sai, hết hạn hoặc nhập sai quá nhiều lần
     */
    @Transactional
    public boolean verifyOtp(String phone, String otpInput) {
        Optional<OtpVerification> optOtp = otpRepository.findLatestByPhone(phone);
        
        if (optOtp.isEmpty()) {
            throw new RuntimeException("OTP không tồn tại. Vui lòng gửi OTP mới.");
        }

        OtpVerification otp = optOtp.get();

        // 🔹 Kiểm tra đã xác thực chưa (để tránh xác thực lại)
        if (otp.isVerified()) {
            throw new RuntimeException("OTP này đã được xác thực.");
        }

        // 🔹 Kiểm tra hết hạn
        if (otp.isExpired()) {
            otp.setStatus("EXPIRED");
            otpRepository.save(otp);
            throw new RuntimeException("OTP đã hết hạn. Vui lòng gửi OTP mới.");
        }

        // 🔹 Kiểm tra số lần nhập sai
        if (otp.isMaxAttemptsReached(otpMaxAttempts)) {
            otp.setStatus("FAILED");
            otpRepository.save(otp);
            throw new RuntimeException("Bạn đã nhập sai OTP " + otpMaxAttempts + 
                " lần. Vui lòng gửi OTP mới.");
        }

        // 🔹 So sánh OTP (dùng PasswordEncoder để so sánh hash)
        if (!passwordEncoder.matches(otpInput, otp.getOtpHash())) {
            otp.incrementAttempt();
            otpRepository.save(otp);
            
            int remainingAttempts = otpMaxAttempts - otp.getAttemptCount();
            throw new RuntimeException("OTP không chính xác. " + remainingAttempts + 
                " lần thử còn lại.");
        }

        // 🔹 ✅ OTP chính xác - Đánh dấu đã xác thực
        otp.setStatus("VERIFIED");
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);

        logger.info("OTP verified successfully for phone: {}", phone);
        return true;
    }

    /**
     * Kiểm tra OTP có xác thực thành công hay không
     */
    public boolean isOtpVerified(String phone) {
        Optional<OtpVerification> otp = otpRepository.findLatestVerifiedByPhone(phone);
        if (otp.isEmpty()) {
            return false;
        }

        OtpVerification otpRecord = otp.get();
        
        // Chỉ hợp lệ nếu xác thực trong 5 phút gần đây (để tránh reuse sau lâu)
        LocalDateTime verifiedTime = otpRecord.getVerifiedAt();
        if (verifiedTime == null) {
            return false;
        }

        LocalDateTime validUntil = verifiedTime.plusMinutes(otpExpiryMinutes);
        return LocalDateTime.now().isBefore(validUntil);
    }

    /**
     * Xóa OTP cũ/hết hạn (cleanup)
     */
    @Transactional
    public void cleanupExpiredOtps() {
        var expiredOtps = otpRepository.findExpiredOtps(LocalDateTime.now());
        if (!expiredOtps.isEmpty()) {
            otpRepository.deleteAll(expiredOtps);
            logger.info("Cleaned up {} expired OTPs", expiredOtps.size());
        }
    }

    // ===== PRIVATE METHODS =====

    /**
     * Tạo OTP ngẫu nhiên (6 chữ số)
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = random.nextInt((int) Math.pow(10, otpLength));
        return String.format("%0" + otpLength + "d", otp);
    }

}
