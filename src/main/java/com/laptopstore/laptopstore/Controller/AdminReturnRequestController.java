package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.ReturnRequestService;
import com.laptopstore.laptopstore.entity.ReturnRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/returns")
@PreAuthorize("hasAnyRole('ADMIN', 'SALE')")
public class AdminReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public AdminReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping
    public String listReturnRequests(Model model) {
        List<ReturnRequest> list = returnRequestService.getAllReturnRequests();
        model.addAttribute("returnRequests", list);
        model.addAttribute("active", "returns");
        model.addAttribute("pageTitle", "Quản Lý Đổi Trả & Hoàn Tiền - LaptopStore Admin");
        return "admin/returns";
    }

    @PostMapping("/{id}/approve")
    public String approveReturnRequest(@PathVariable Long id,
                                       @RequestParam(required = false) String adminNote,
                                       RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.approveReturnRequest(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt trả hàng & hoàn tất hoàn lại tồn kho!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/returns";
    }

    @PostMapping("/{id}/reject")
    public String rejectReturnRequest(@PathVariable Long id,
                                      @RequestParam(required = false) String adminNote,
                                      RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.rejectReturnRequest(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu trả hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/returns";
    }
}
