package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.User;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api-key:MOCK_KEY}")
    private String sendGridKey;

    @Value("${email.from:techstore247sp@gmail.com}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String contentText) {
        try {
            if (sendGridKey == null || sendGridKey.isBlank() || "MOCK_KEY".equals(sendGridKey)) {
                log.info("📧 [MOCK EMAIL] To: {} | Subject: {}\nContent:\n{}", to, subject, contentText);
                return;
            }

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", contentText);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(sendGridKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            sg.api(request);
            log.info("✅ SendGrid Email sent to {}", to);
        } catch (Exception e) {
            log.error("❌ Error sending email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmationEmail(User user, Order order) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        String subject = "🛒 XÁC NHẬN ĐƠN HÀNG #" + order.getOrderCode() + " - LaptopStore";
        String content = String.format(
            "Kính chào %s,\n\n" +
            "Cảm ơn bạn đã đặt hàng tại LaptopStore!\n" +
            "Thông tin đơn hàng:\n" +
            "- Mã đơn hàng: %s\n" +
            "- Tổng tiền: %,d VNĐ\n" +
            "- Trạng thái: %s\n" +
            "- Địa chỉ giao hàng: %s\n\n" +
            "Chúng tôi sẽ xử lý và giao hàng cho bạn trong thời gian sớm nhất.\n\n" +
            "Trân trọng,\nĐội ngũ LaptopStore",
            user.getFullName() != null ? user.getFullName() : user.getUsername(),
            order.getOrderCode(),
            order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0,
            order.getOrderStatus(),
            order.getShippingAddress()
        );
        sendEmail(user.getEmail(), subject, content);
    }

    @Async
    public void sendOrderStatusUpdateEmail(User user, Order order) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        String subject = "📦 CẬP NHẬT ĐƠN HÀNG #" + order.getOrderCode() + " - " + order.getOrderStatus();
        String content = String.format(
            "Kính chào %s,\n\n" +
            "Đơn hàng #%s của bạn đã được cập nhật trạng thái mới: %s.\n\n" +
            "Trân trọng,\nĐội ngũ LaptopStore",
            user.getFullName() != null ? user.getFullName() : user.getUsername(),
            order.getOrderCode(),
            order.getOrderStatus()
        );
        sendEmail(user.getEmail(), subject, content);
    }

    @Async
    public void sendLowStockAlertEmail(String adminEmail, String productName, int currentStock) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        String subject = "⚠️ CẢNH BÁO TỒN KHO THẤP - " + productName;
        String content = String.format(
            "Cảnh báo hệ thống:\n\n" +
            "Sản phẩm '%s' hiện tại chỉ còn %d sản phẩm trong kho (dưới ngưỡng tối thiểu 3 sản phẩm).\n" +
            "Vui lòng nhập thêm hàng để tránh đứt gãy kinh doanh.\n\n" +
            "LaptopStore Admin System",
            productName, currentStock
        );
        sendEmail(adminEmail, subject, content);
    }
}
