package com.js.multi_seller.controller;

import com.js.multi_seller.dto.PageResponse;
import com.js.multi_seller.dto.product.ProductDetailResponse;
import com.js.multi_seller.dto.product.ProductSummaryResponse;
import com.js.multi_seller.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductSummaryResponse> getProducts(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return productService.findProducts(query, category, brand, minPrice, maxPrice, inStockOnly, sort, page, size);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable long id) {
        return productService.findProduct(id);
    }
}
