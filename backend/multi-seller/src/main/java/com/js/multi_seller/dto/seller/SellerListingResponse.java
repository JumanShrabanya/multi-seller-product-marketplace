package com.js.multi_seller.dto.seller;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerListingResponse(
        long id,
        long productId,
        String productName,
        String brand,
        BigDecimal price,
        int stock,
        int minOrderQty,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
