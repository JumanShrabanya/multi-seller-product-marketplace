package com.js.multi_seller.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        long id,
        String name,
        String brand,
        String category,
        String unit,
        String description,
        BigDecimal lowestPrice,
        int offerCount,
        boolean currentlyUnavailable,
        List<OfferResponse> offers) {
}
