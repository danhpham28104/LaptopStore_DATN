package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.dto.CustomerAnalyticsDto;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerAnalyticsService {

    private static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public CustomerAnalyticsService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public CustomerAnalyticsDto getCustomerAnalytics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<User> allUsers = userRepository.findAll();

        long newCustomers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(start) && !u.getCreatedAt().isAfter(end))
                .count();

        long customersWithOrders = 0;
        long customersWithoutOrders = 0;
        long returningCustomers = 0;
        BigDecimal totalRevenueFromCustomers = BigDecimal.ZERO;
        long totalOrdersFromCustomers = 0;

        for (User u : allUsers) {
            var orders = orderRepository.findByUser_Id(u.getId()).stream()
                    .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end)
                            && SUCCESS_STATUSES.contains(o.getOrderStatus()))
                    .toList();

            if (orders.isEmpty()) {
                customersWithoutOrders++;
            } else {
                customersWithOrders++;
                if (orders.size() > 1) {
                    returningCustomers++;
                }
                for (var o : orders) {
                    totalRevenueFromCustomers = totalRevenueFromCustomers.add(o.getTotalAmount());
                    totalOrdersFromCustomers++;
                }
            }
        }

        double avgOrdersPerCustomer = customersWithOrders > 0
                ? (double) totalOrdersFromCustomers / customersWithOrders
                : 0.0;

        BigDecimal avgRevenuePerCustomer = customersWithOrders > 0
                ? totalRevenueFromCustomers.divide(BigDecimal.valueOf(customersWithOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CustomerAnalyticsDto(
                newCustomers,
                returningCustomers,
                customersWithOrders,
                customersWithoutOrders,
                Math.round(avgOrdersPerCustomer * 100.0) / 100.0,
                avgRevenuePerCustomer
        );
    }
}
