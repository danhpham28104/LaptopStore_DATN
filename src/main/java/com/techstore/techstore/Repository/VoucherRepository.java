package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Voucher;
import org.antlr.v4.runtime.atn.SemanticContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    @Query("""
    SELECT v FROM Voucher v
    WHERE v.active = true
      AND v.quantity > 0
""")
    List<Voucher> findAllAvailable();

}
