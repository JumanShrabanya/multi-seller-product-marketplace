package com.js.multi_seller.dto.product;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String query,
        String category,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStockOnly,
        ProductSort sort,
        int page,
        int size
) {
}
