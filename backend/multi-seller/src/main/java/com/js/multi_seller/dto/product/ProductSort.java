package com.js.multi_seller.dto.product;

import com.js.multi_seller.exception.BadRequestException;

import java.util.Locale;

public enum ProductSort {
    PRICE_ASC,
    PRICE_DESC,
    NAME,
    RELEVANCE;

    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return RELEVANCE;
        }

        try {
            return ProductSort.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("sort must be one of: price_asc, price_desc, name, relevance");
        }
    }
}
