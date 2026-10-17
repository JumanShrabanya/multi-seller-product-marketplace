package com.js.multi_seller.controller;

import com.js.multi_seller.dto.seller.CatalogueProductResponse;
import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public List<CatalogueProductResponse> searchProducts(SellerContext seller,
                                                         @RequestParam(required = false, name = "q") String query,
                                                         @RequestParam(defaultValue = "20") int size) {
        return productService.searchCatalogue(query, size);
    }
}
