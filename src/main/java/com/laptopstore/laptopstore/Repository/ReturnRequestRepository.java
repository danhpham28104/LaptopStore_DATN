package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.ReturnRequest;
import com.laptopstore.laptopstore.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrder_Id(Long orderId);
    List<ReturnRequest> findByUser_Id(Long userId);
    List<ReturnRequest> findByStatus(ReturnStatus status);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
}
