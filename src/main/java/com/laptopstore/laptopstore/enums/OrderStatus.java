package com.laptopstore.laptopstore.enums;

import java.util.List;

/**
 * Trạng thái chuẩn hóa của đơn hàng — tách biệt hoàn toàn với PaymentStatus.
 *
 * <p>Luồng chính:
 * CHO_THANH_TOAN → CHO_XAC_NHAN → DA_XAC_NHAN → CHUAN_BI_HANG → DANG_GIAO_HANG → DA_GIAO_HANG → HOAN_THANH
 *
 * <p>Luồng ngoại lệ:
 * - CHO_THANH_TOAN → DA_HUY
 * - CHO_XAC_NHAN   → DA_HUY
 * - DA_XAC_NHAN    → DA_HUY (hoàn kho tự động)
 * - CHUAN_BI_HANG  → DA_HUY (hoàn kho tự động)
 * - DA_GIAO_HANG   → YEU_CAU_HOAN_TRA
 * - HOAN_THANH     → YEU_CAU_HOAN_TRA
 * - YEU_CAU_HOAN_TRA → DA_HOAN_TRA (hoàn kho tự động)
 * - DA_HOAN_TRA    → DA_HOAN_TIEN
 */
public enum OrderStatus {

    // ── Luồng chính ──────────────────────────────────────────────────────────
    CHO_THANH_TOAN("Chờ thanh toán", "bg-warning text-dark"),
    CHO_XAC_NHAN("Chờ xác nhận", "bg-info text-dark"),
    DA_XAC_NHAN("Đã xác nhận", "bg-primary text-white"),
    CHUAN_BI_HANG("Chuẩn bị hàng", "bg-secondary text-white"),
    DANG_GIAO_HANG("Đang giao hàng", "bg-info text-white"),
    DA_GIAO_HANG("Đã giao hàng", "bg-success text-white"),
    HOAN_THANH("Hoàn thành", "bg-success text-white"),

    // ── Trạng thái ngoại lệ ──────────────────────────────────────────────────
    DA_HUY("Đã hủy", "bg-danger text-white"),
    YEU_CAU_HOAN_TRA("Yêu cầu hoàn trả", "bg-warning text-dark"),
    DA_HOAN_TRA("Đã hoàn trả", "bg-secondary text-white"),
    DA_HOAN_TIEN("Đã hoàn tiền", "bg-dark text-white");

    /** Nhóm trạng thái đơn hàng thành công (đã xác nhận / đang xử lý / hoàn tất) phục vụ thống kê */
    public static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            DA_XAC_NHAN, CHUAN_BI_HANG, DANG_GIAO_HANG, DA_GIAO_HANG, HOAN_THANH
    );

    /** Nhóm trạng thái hủy / hoàn trả / hoàn tiền phục vụ thống kê */
    public static final List<OrderStatus> CANCELLED_STATUSES = List.of(
            DA_HUY, DA_HOAN_TRA, DA_HOAN_TIEN
    );

    /** Nhóm trạng thái đang chờ thanh toán hoặc chờ xác nhận */
    public static final List<OrderStatus> PENDING_STATUSES = List.of(
            CHO_THANH_TOAN, CHO_XAC_NHAN
    );

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /**
     * Danh sách các trạng thái tiếp theo hợp lệ từ trạng thái hiện tại.
     * Không cho phép chuyển lùi, nhảy bước hoặc chọn trạng thái bất kỳ.
     */
    public List<OrderStatus> getAllowedNextStatuses() {
        switch (this) {
            case CHO_THANH_TOAN:
                return List.of(CHO_XAC_NHAN, DA_HUY);
            case CHO_XAC_NHAN:
                return List.of(DA_XAC_NHAN, DA_HUY);
            case DA_XAC_NHAN:
                return List.of(CHUAN_BI_HANG, DA_HUY);
            case CHUAN_BI_HANG:
                return List.of(DANG_GIAO_HANG, DA_HUY);
            case DANG_GIAO_HANG:
                return List.of(DA_GIAO_HANG);
            case DA_GIAO_HANG:
                return List.of(HOAN_THANH, YEU_CAU_HOAN_TRA);
            case HOAN_THANH:
                return List.of(YEU_CAU_HOAN_TRA);
            case YEU_CAU_HOAN_TRA:
                return List.of(DA_HOAN_TRA);
            case DA_HOAN_TRA:
                return List.of(DA_HOAN_TIEN);
            case DA_HUY:
            case DA_HOAN_TIEN:
            default:
                return List.of();
        }
    }

    /**
     * Helper kiểm tra xem có được phép chuyển từ trạng thái hiện tại sang trạng thái đích không.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return target != null && getAllowedNextStatuses().contains(target);
    }

    /**
     * Kiểm tra đơn có thể hủy được hay không.
     */
    public boolean isCancellable() {
        return canTransitionTo(DA_HUY);
    }

    /**
     * Kiểm tra đây có phải trạng thái đang chờ thanh toán hoặc chờ xác nhận.
     */
    public boolean isPendingPayment() {
        return this == CHO_THANH_TOAN || this == CHO_XAC_NHAN;
    }

    /**
     * Kiểm tra trạng thái thuộc nhóm đơn hàng thành công (thống kê/doanh thu).
     */
    public boolean isSuccessful() {
        return SUCCESS_STATUSES.contains(this);
    }

    /**
     * Kiểm tra đây có phải trạng thái kết thúc (không còn bước chuyển tiếp nào).
     */
    public boolean isTerminal() {
        return getAllowedNextStatuses().isEmpty();
    }

    /**
     * Kiểm tra đơn đã bị khóa dữ liệu chỉnh sửa (không cho sửa thông tin đơn/sản phẩm/số lượng).
     */
    public boolean isLockedForEdit() {
        return this == HOAN_THANH
                || this == DA_HUY
                || this == YEU_CAU_HOAN_TRA
                || this == DA_HOAN_TRA
                || this == DA_HOAN_TIEN;
    }

    /**
     * Trả về chỉ số bước (1..7) trên thanh tiến trình luồng chính; 0 nếu là trạng thái ngoại lệ.
     */
    public int getStepIndex() {
        switch (this) {
            case CHO_THANH_TOAN: return 1;
            case CHO_XAC_NHAN:   return 2;
            case DA_XAC_NHAN:    return 3;
            case CHUAN_BI_HANG:  return 4;
            case DANG_GIAO_HANG: return 5;
            case DA_GIAO_HANG:   return 6;
            case HOAN_THANH:     return 7;
            default:             return 0;
        }
    }

    /**
     * Chuyển đổi linh hoạt từ chuỗi trong DB cũ hoặc tên hiển thị sang Enum chuẩn.
     * Nếu không nhận diện được, trả về CHO_XAC_NHAN để đảm bảo an toàn đọc dữ liệu cũ.
     */
    public static OrderStatus fromString(String text) {
        OrderStatus parsed = parseNullable(text);
        return parsed != null ? parsed : CHO_XAC_NHAN;
    }

    /**
     * Chuyển đổi nghiêm ngặt từ tham số người dùng/API; ném IllegalArgumentException nếu không hợp lệ.
     */
    public static OrderStatus fromStringStrict(String text) {
        OrderStatus parsed = parseNullable(text);
        if (parsed == null) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + text);
        }
        return parsed;
    }

    private static OrderStatus parseNullable(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String t = text.trim();
        for (OrderStatus s : OrderStatus.values()) {
            if (s.name().equalsIgnoreCase(t) || s.displayName.equalsIgnoreCase(t)) {
                return s;
            }
        }
        switch (t.toUpperCase()) {
            case "PENDING_PAYMENT":
            case "CHO THANH TOAN":
                return CHO_THANH_TOAN;
            case "PENDING":
            case "CHO XAC NHAN":
                return CHO_XAC_NHAN;
            case "CONFIRMED":
            case "PAID":
            case "DA XAC NHAN":
                return DA_XAC_NHAN;
            case "PROCESSING":
            case "PACKING":
            case "DANG XU LY":
            case "DANG DONG GOI":
            case "CHUAN BI HANG":
                return CHUAN_BI_HANG;
            case "SHIPPING":
            case "SHIPPED":
            case "DANG GIAO HANG":
                return DANG_GIAO_HANG;
            case "DELIVERED":
            case "DA GIAO HANG":
                return DA_GIAO_HANG;
            case "COMPLETED":
            case "HOAN THANH":
                return HOAN_THANH;
            case "CANCELLED":
            case "CANCELED":
            case "PAYMENT TIMEOUT":
            case "PAYMENT_TIMEOUT":
            case "PAYMENT FAILED":
            case "PAYMENT_FAILED":
            case "DA HUY":
                return DA_HUY;
            case "RETURN_REQUESTED":
            case "YEU CAU HOAN TRA":
                return YEU_CAU_HOAN_TRA;
            case "RETURNED":
            case "DA HOAN TRA":
                return DA_HOAN_TRA;
            case "REFUNDED":
            case "DA HOAN TIEN":
                return DA_HOAN_TIEN;
            default:
                return null;
        }
    }
}
