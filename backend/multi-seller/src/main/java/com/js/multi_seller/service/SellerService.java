package com.js.multi_seller.service;

import com.js.multi_seller.dto.seller.SellerResponse;
import com.js.multi_seller.repository.SellerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    public List<SellerResponse> listSellers() {
        return sellerRepository.findAll(Sort.by("id")).stream()
                .map(seller -> new SellerResponse(seller.getId(), seller.getName(), seller.getCity(), seller.getStatus()))
                .toList();
    }
}
