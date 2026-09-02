package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.RfmSegmentationService;
import com.laptopstore.laptopstore.dto.CustomerRfmDto;
import com.laptopstore.laptopstore.dto.RfmSegmentSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/customers/rfm")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerRfmController {

    private final RfmSegmentationService rfmSegmentationService;

    public AdminCustomerRfmController(RfmSegmentationService rfmSegmentationService) {
        this.rfmSegmentationService = rfmSegmentationService;
    }

    @GetMapping
    public String rfmSegmentationPage(Model model) {
        List<CustomerRfmDto> customerRfmList = rfmSegmentationService.getAllCustomerRfmAnalysis();
        List<RfmSegmentSummaryDto> segmentSummaries = rfmSegmentationService.getRfmSegmentSummaries();

        model.addAttribute("customerRfmList", customerRfmList);
        model.addAttribute("segmentSummaries", segmentSummaries);
        model.addAttribute("active", "customers_rfm");
        model.addAttribute("pageTitle", "Phân Khúc Khách Hàng RFM & Cá Nhân Hóa - LaptopStore Admin");

        return "admin/customers_rfm";
    }
}
