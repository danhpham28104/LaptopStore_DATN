package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.BrandService;
import com.laptopstore.laptopstore.Service.VoucherService;
import com.laptopstore.laptopstore.entity.Voucher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private BrandService brandService;

    /** 🔹 Danh sách voucher */
    @GetMapping
    public String listVouchers(Model model) {
        List<Voucher> vouchers = voucherService.getAll();
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("pageTitle", "Quản lý Voucher - LaptopStore Admin");
        model.addAttribute("active", "vouchers");
        return "admin/vouchers";
    }

    /** 🔹 Thêm voucher mới */
    @PostMapping("/add")
    public String addVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal discountValue,
            @RequestParam String discountType,
            @RequestParam(required = false) BigDecimal minOrderValue,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String description,
            // 🟢 Advanced fields
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Long applicableBrandId,
            @RequestParam(required = false) Integer usageLimitPerUser
    ) {
        if (voucherService.existsByCode(code)) {
            return "redirect:/admin/vouchers?error=exists";
        }

        Voucher v = new Voucher();
        v.setCode(code);
        v.setDiscountValue(discountValue);
        v.setDiscountType(discountType);
        v.setMinOrderValue(minOrderValue);
        v.setStartDate(startDate);
        v.setEndDate(endDate);
        v.setQuantity(quantity);
        v.setDescription(description);
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        v.setActive(true);
        // 🟢 Advanced fields
        v.setMaxDiscountAmount(maxDiscountAmount);
        v.setApplicableBrandId(applicableBrandId);
        v.setUsageLimitPerUser(usageLimitPerUser);

        voucherService.save(v);
        return "redirect:/admin/vouchers?success=added";
    }

    /** 🔹 Cập nhật voucher */
    @PostMapping("/edit/{id}")
    public String editVoucher(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestParam BigDecimal discountValue,
            @RequestParam String discountType,
            @RequestParam(required = false) BigDecimal minOrderValue,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam Integer quantity,
            @RequestParam Boolean active,
            @RequestParam(required = false) String description,
            // 🟢 Advanced fields
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Long applicableBrandId,
            @RequestParam(required = false) Integer usageLimitPerUser
    ) {
        Voucher v = voucherService.getById(id).orElse(null);
        if (v == null) return "redirect:/admin/vouchers?error=notfound";

        v.setCode(code);
        v.setDiscountValue(discountValue);
        v.setDiscountType(discountType);
        v.setMinOrderValue(minOrderValue);
        v.setStartDate(startDate);
        v.setEndDate(endDate);
        v.setQuantity(quantity);
        v.setActive(active);
        v.setDescription(description);
        v.setUpdatedAt(LocalDateTime.now());
        // 🟢 Advanced fields
        v.setMaxDiscountAmount(maxDiscountAmount);
        v.setApplicableBrandId(applicableBrandId);
        v.setUsageLimitPerUser(usageLimitPerUser);

        voucherService.save(v);
        return "redirect:/admin/vouchers?success=updated";
    }

    /** 🔹 Xoá voucher */
    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Long id) {
        voucherService.delete(id);
        return "redirect:/admin/vouchers?success=deleted";
    }
}
