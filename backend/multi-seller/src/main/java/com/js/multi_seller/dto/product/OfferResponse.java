package com.js.multi_seller.dto.product;

import java.math.BigDecimal;

public record OfferResponse(
        long sellerId,
        String sellerName,
        String sellerCity,
        BigDecimal price,
        int stock,
        int minOrderQty) {
}
