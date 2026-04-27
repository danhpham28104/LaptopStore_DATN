package com.techstore.techstore.Service;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final String SEPAY_QR_BASE = "https://qr.sepay.vn/img";
    private final String VA_ACCOUNT = "5282587777"; // tài khoản VA của bạn
    private final String BANK_CODE = "MBBank";

    public String generatePaymentQR(long amount, String orderCode) {
        return SEPAY_QR_BASE
                + "?acc=" + VA_ACCOUNT
                + "&bank=" + BANK_CODE
                + "&amount=" + amount
                + "&des=" + orderCode;
    }
}
