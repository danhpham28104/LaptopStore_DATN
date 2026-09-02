package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.ReturnRequestRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.OrderItem;
import com.laptopstore.laptopstore.entity.Product;
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
    private final ProductRepository productRepository;

    public ReturnRequestService(ReturnRequestRepository returnRequestRepository,
                                OrderRepository orderRepository,
                                UserRepository userRepository,
                                ProductRepository productRepository) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /** Tạo yêu cầu đổi trả hàng mới từ khách hàng. */
    public ReturnRequest createReturnRequest(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Đơn hàng này không thuộc về tài khoản của bạn.");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Chỉ đơn hàng đã giao (DELIVERED) mới có thể tạo yêu cầu trả hàng.");
        }

        if (returnRequestRepository.findByOrder_Id(orderId).isPresent()) {
            throw new IllegalStateException("Đơn hàng này đã gửi yêu cầu trả hàng trước đó.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng #" + userId));

        ReturnRequest req = new ReturnRequest(order, user, reason, order.getTotalAmount());
        returnRequestRepository.save(req);

        // Cập nhật trạng thái đơn thành RETURN_REQUESTED
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        return req;
    }

    /** Phê duyệt yêu cầu đổi trả & tự động cộng lại tồn kho. */
    public ReturnRequest approveReturnRequest(Long returnRequestId, String adminNote) {
        ReturnRequest req = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu #" + returnRequestId));

        if (req.getStatus() != ReturnStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        req.setStatus(ReturnStatus.COMPLETED);
        req.setAdminNote(adminNote);
        returnRequestRepository.save(req);

        Order order = req.getOrder();
        order.setOrderStatus(OrderStatus.RETURNED);
        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.REFUND_PENDING);
        }
        orderRepository.save(order);

        // Hoàn trả tồn kho sản phẩm (Restore stock)
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    int quantity = item.getQuantity();
                    int currentStock = product.getStock();
                    product.setStock(currentStock + quantity);
                    productRepository.save(product);
                }
            }
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
        order.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        return req;
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getAllReturnRequests() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc();
    }
}
