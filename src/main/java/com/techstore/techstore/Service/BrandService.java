package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.BrandRepository;
import com.techstore.techstore.entity.Brand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BrandService {
    @Autowired
    private BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Brand> getById(Long id) {
        return brandRepository.findById(id);
    }

    @Transactional
    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }

    @Transactional
    public void delete(Long id) {
        brandRepository.deleteById(id);
    }

}
