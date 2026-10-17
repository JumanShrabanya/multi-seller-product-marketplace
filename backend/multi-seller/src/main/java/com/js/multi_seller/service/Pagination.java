package com.js.multi_seller.service;

import com.js.multi_seller.exception.BadRequestException;

final class Pagination {

    static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    static void validate(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
