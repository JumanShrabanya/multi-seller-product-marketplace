package com.js.multi_seller.dto.product;

import java.math.BigDecimal;

public record ProductSummaryResponse(long id, String name, String brand, String category, String unit,
                                     BigDecimal lowestPrice, int offerCount, boolean currentlyUnavailable) {
}
