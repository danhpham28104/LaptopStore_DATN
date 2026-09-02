package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.dto.CustomerRfmDto;
import com.laptopstore.laptopstore.dto.RfmSegmentSummaryDto;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RfmSegmentationService {

    private static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public RfmSegmentationService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public List<CustomerRfmDto> getAllCustomerRfmAnalysis() {
        LocalDateTime now = LocalDateTime.now();
        List<User> users = userRepository.findAll();
        List<CustomerRfmDto> list = new ArrayList<>();

        for (User u : users) {
            List<Order> orders = orderRepository.findByUser_Id(u.getId()).stream()
                    .filter(o -> SUCCESS_STATUSES.contains(o.getOrderStatus()))
                    .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .toList();

            if (orders.isEmpty()) {
                list.add(new CustomerRfmDto(
                        u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                        null, 999L, 0L, BigDecimal.ZERO,
                        "Khách chưa mua hàng", "Tặng Voucher chào mừng 50k để kích cầu mua lần đầu"
                ));
                continue;
            }

            LocalDateTime lastOrderDate = orders.get(0).getCreatedAt();
            long recencyDays = lastOrderDate != null ? Duration.between(lastOrderDate, now).toDays() : 999L;
            long frequency = orders.size();
            BigDecimal monetary = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String segment = "Tiềm năng";
            String action = "Gửi ưu đãi sản phẩm mới";

            if (recencyDays <= 30 && frequency >= 3 && monetary.compareTo(BigDecimal.valueOf(30_000_000)) >= 0) {
                segment = "Champions (Khách VIP)";
                action = "Tri ân quà tặng đặc biệt, tư vấn 1-1 sản phẩm cao cấp mới nhất";
            } else if (recencyDays <= 60 && frequency >= 2 && monetary.compareTo(BigDecimal.valueOf(15_000_000)) >= 0) {
                segment = "Loyal Customers (Khách Thân Thiết)";
                action = "Tặng mã giảm giá trung thành 5%, mời tham gia chương trình tích điểm";
            } else if (recencyDays <= 30 && frequency == 1) {
                segment = "Potential Loyalist (Mới Mua 1 Lần)";
                action = "Gợi ý phụ kiện đi kèm (Chuột, Ba lô, Tai nghe) để tăng F";
            } else if (recencyDays > 60 && recencyDays <= 120 && frequency >= 2) {
                segment = "At Risk (Nguy Cơ Rời Bỏ)";
                action = "Gửi Email/SMS Remind kèm Voucher giảm giá 10% đặc biệt";
            } else if (recencyDays > 120) {
                segment = "Lost (Đã Rời Bỏ)";
                action = "Chiến dịch Winback: Tặng voucher tri ân lớn để kéo khách quay lại";
            }

            list.add(new CustomerRfmDto(
                    u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                    lastOrderDate, recencyDays, frequency, monetary, segment, action
            ));
        }

        list.sort(Comparator.comparing(CustomerRfmDto::getMonetaryTotal).reversed());
        return list;
    }

    public List<RfmSegmentSummaryDto> getRfmSegmentSummaries() {
        List<CustomerRfmDto> rfmList = getAllCustomerRfmAnalysis();
        long totalCustomers = rfmList.size();

        Map<String, Long> countMap = new HashMap<>();
        Map<String, BigDecimal> revMap = new HashMap<>();

        for (CustomerRfmDto dto : rfmList) {
            String seg = dto.getRfmSegment();
            countMap.put(seg, countMap.getOrDefault(seg, 0L) + 1);
            revMap.put(seg, revMap.getOrDefault(seg, BigDecimal.ZERO).add(dto.getMonetaryTotal()));
        }

        List<RfmSegmentSummaryDto> summaryList = new ArrayList<>();
        countMap.forEach((seg, count) -> {
            double pct = totalCustomers > 0 ? (double) count / totalCustomers * 100.0 : 0.0;
            summaryList.add(new RfmSegmentSummaryDto(seg, count, revMap.get(seg), Math.round(pct * 10.0) / 10.0));
        });

        summaryList.sort(Comparator.comparing(RfmSegmentSummaryDto::getCustomerCount).reversed());
        return summaryList;
    }
}
