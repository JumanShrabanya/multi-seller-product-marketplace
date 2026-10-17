package com.js.multi_seller.controller;

import com.js.multi_seller.dto.seller.SellerResponse;
import com.js.multi_seller.service.SellerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/mock")
    public List<SellerResponse> getMockSellers() {
        return sellerService.listSellers();
    }
}
