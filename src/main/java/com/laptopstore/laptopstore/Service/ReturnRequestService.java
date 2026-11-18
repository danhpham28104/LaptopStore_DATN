package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.OrderStatusHistoryRepository;
import com.laptopstore.laptopstore.Repository.ReturnRequestRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.OrderStatusHistory;
import com.laptopstore.laptopstore.entity.ReturnRequest;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.PaymentStatus;
import com.laptopstore.laptopstore.enums.ReturnStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final OrderStatusHistoryRepository historyRepository;

    public ReturnRequestService(ReturnRequestRepository returnRequestRepository,
                                OrderRepository orderRepository,
                                UserRepository userRepository,
                                OrderService orderService,
                                OrderStatusHistoryRepository historyRepository) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.historyRepository = historyRepository;
    }

    /** Tạo yêu cầu đổi trả hàng mới từ khách hàng. */
    public ReturnRequest createReturnRequest(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc về tài khoản của bạn.");
        }

        if (order.getOrderStatus() != OrderStatus.DA_GIAO_HANG
                && order.getOrderStatus() != OrderStatus.HOAN_THANH) {
            throw new IllegalStateException("Chỉ đơn hàng Đã giao hàng hoặc Hoàn thành mới có thể tạo yêu cầu trả hàng.");
        }

        if (returnRequestRepository.findByOrder_Id(orderId).isPresent()) {
            throw new IllegalStateException("Đơn hàng này đã gửi yêu cầu trả hàng trước đó.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng #" + userId));

        ReturnRequest req = new ReturnRequest(order, user, reason, order.getTotalAmount());
        returnRequestRepository.save(req);

        // Chuyển trạng thái đơn hàng qua OrderService để kiểm tra luồng & ghi lịch sử
        orderService.updateOrderStatus(orderId, OrderStatus.YEU_CAU_HOAN_TRA, user.getUsername(), reason);

        return req;
    }

    /** Phê duyệt yêu cầu đổi trả & tự động hoàn kho qua OrderService. */
    public ReturnRequest approveReturnRequest(Long returnRequestId, String adminNote) {
        ReturnRequest req = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu #" + returnRequestId));

        if (req.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        req.setStatus(ReturnStatus.COMPLETED);
        req.setAdminNote(adminNote);
        returnRequestRepository.save(req);

        Order order = orderService.updateOrderStatus(
                req.getOrder().getId(),
                OrderStatus.DA_HOAN_TRA,
                "Admin",
                adminNote
        );
        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.REFUND_PENDING);
            orderRepository.save(order);
        }

        return req;
    }

    /** Từ chối yêu cầu đổi trả. */
    public ReturnRequest rejectReturnRequest(Long returnRequestId, String adminNote) {
        ReturnRequest req = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu #" + returnRequestId));

        if (req.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        req.setStatus(ReturnStatus.REJECTED);
        req.setAdminNote(adminNote);
        returnRequestRepository.save(req);

        Order order = req.getOrder();
        OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.DA_GIAO_HANG);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(
                order,
                oldStatus != null ? oldStatus.name() : null,
                OrderStatus.DA_GIAO_HANG.name(),
                "Admin",
                "Từ chối yêu cầu hoàn trả" + (adminNote != null && !adminNote.isBlank() ? ": " + adminNote : "")
        ));

        return req;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getAllReturnRequests() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc();
    }
}
