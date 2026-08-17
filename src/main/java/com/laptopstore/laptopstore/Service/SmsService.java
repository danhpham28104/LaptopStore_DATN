package com.laptopstore.laptopstore.Service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMS Service - Mock để gửi OTP qua SMS
 * 
 * Ban đầu in ra console để test.
 * Sau này có thể thay bằng Twilio, AWS SNS, hay SMS API khác.
 */
@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    /**
     * Gửi OTP qua SMS (Mock - in ra console)
     * 
     * Trong production, thay bằng:
     * - Twilio: twilio.getTwilioRestClient().getMessages().create()
     * - AWS SNS: snsClient.publish()
     * - Tài nguyên khác
     */
    public void sendOtpSms(String phone, String otp) {
        // 🔹 Mock: In ra console để test
        String message = String.format(
            "\n" +
            "╔════════════════════════════════════════╗\n" +
            "║         🔐 OTP VERIFICATION            ║\n" +
            "╠════════════════════════════════════════╣\n" +
            "║ Phone: %-32s ║\n" +
            "║ OTP Code: %-29s ║\n" +
            "║ Valid for 5 minutes                    ║\n" +
            "║ Do not share this code with anyone     ║\n" +
            "╚════════════════════════════════════════╝\n",
            phone,
            otp
        );

        logger.info(message);
        System.out.println(message);

        // 🔹 TODO: Thay bằng code Twilio/AWS SNS thật:
        /*
        try {
            Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
            Message.creator(
                    new PhoneNumber("+84" + phone.substring(1)),  // To number
                    new PhoneNumber("+your-twilio-number"),      // From number
                    "Your LaptopStore OTP: " + otp + ". Valid for 5 minutes."
            ).create();
            
            logger.info("OTP sent successfully to {}", phone);
        } catch (Exception e) {
            logger.error("Failed to send OTP to {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send OTP");
        }
        */
    }

    /**
     * Gửi OTP qua Email (optional - nếu muốn alternative)
     */
    public void sendOtpEmail(String email, String otp) {
        String message = String.format(
            "Your LaptopStore OTP Code is: %s\n" +
            "Valid for 5 minutes.\n" +
            "Do not share this code with anyone.",
            otp
        );
        
        logger.info("Email OTP to {}: {}", email, message);
        // TODO: Integrate with EmailService nếu cần
    }
}
