package com.techstore.techstore.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class để lấy IP thật của client.
 * Đọc các proxy header theo thứ tự ưu tiên trước khi fallback về getRemoteAddr().
 */
public final class ClientIpUtil {

    private static final String[] PROXY_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    private ClientIpUtil() {}

    /**
     * Lấy IP thật của client từ request.
     * Nếu X-Forwarded-For chứa nhiều IP (chuỗi phân cách bằng dấu phẩy), lấy IP đầu tiên.
     *
     * @param request HttpServletRequest
     * @return IP string của client (không null)
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For có thể chứa "client, proxy1, proxy2" → lấy phần đầu
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex);
                }
                return ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
