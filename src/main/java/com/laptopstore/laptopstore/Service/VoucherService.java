package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.VoucherRepository;
import com.laptopstore.laptopstore.entity.Voucher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Transactional(readOnly = true)
    public List<Voucher> getAll() {
        return voucherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Voucher> getById(Long id) {
        return voucherRepository.findById(id);
    }

    @Transactional
    public Voucher save(Voucher v) {
        return voucherRepository.save(v);
    }

    @Transactional
    public void delete(Long id) {
        voucherRepository.deleteById(id);
    }

    public boolean existsByCode(String code) {
        return voucherRepository.existsByCode(code);
    }
    public Voucher findByCode(String code) {
        return voucherRepository.findByCode(code).orElse(null);
    }
    public List<Voucher> getAvailableVouchers(Long userId) {
        return voucherRepository.findAllAvailableNow(LocalDateTime.now());
    }

}
