package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/check")
    public Map<String, Object> checkVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {

        Map<String, Object> result = new HashMap<>();

        var v = voucherService.findByCode(code);

        if (v == null) {
            result.put("valid", false);
            result.put("message", "❌ Mã không tồn tại");
            return result;
        }

        if (!v.getActive()) {
            result.put("valid", false);
            result.put("message", "❌ Mã đã bị khóa");
            return result;
        }

        // 🚨 Check ngày null
        if (v.getStartDate() == null || v.getEndDate() == null) {
            result.put("valid", false);
            result.put("message", "❌ Voucher không hợp lệ");
            return result;
        }

        LocalDateTime now = LocalDateTime.now();

        if (v.getStartDate().isAfter(now) || v.getEndDate().isBefore(now)) {
            result.put("valid", false);
            result.put("message", "❌ Mã đã hết hạn");
            return result;
        }

        if (v.getQuantity() <= 0) {
            result.put("valid", false);
            result.put("message", "❌ Mã đã hết lượt sử dụng");
            return result;
        }

        if (v.getMinOrderValue() != null &&
                subtotal.compareTo(v.getMinOrderValue()) < 0) {

            result.put("valid", false);
            result.put("message", "❌ Đơn tối thiểu: " + v.getMinOrderValue() + " ₫");
            return result;
        }

        // 🔥 Tính giảm
        BigDecimal discount;

        if (v.getDiscountType().equals("PERCENT")) {
            discount = subtotal.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));

            // không vượt tổng đơn
            if (discount.compareTo(subtotal) > 0) {
                discount = subtotal;
            }

        } else { // AMOUNT
            discount = v.getDiscountValue();

            // không vượt tổng
            if (discount.compareTo(subtotal) > 0) {
                discount = subtotal;
            }
        }

        result.put("valid", true);
        result.put("discount", discount);
        result.put("message", "✔ Áp dụng thành công!");

        return result;
    }
}
