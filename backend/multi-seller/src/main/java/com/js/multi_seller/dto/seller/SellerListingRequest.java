package com.js.multi_seller.dto.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SellerListingRequest(
        @NotNull @Positive Long productId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotNull @Min(1) Integer minOrderQty
) {
}
