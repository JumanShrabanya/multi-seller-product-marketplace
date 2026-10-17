package com.js.multi_seller.repository;

final class BuyerListingVisibility {

    private BuyerListingVisibility() {
    }

    static final String WHERE_CLAUSE = """
            seller.status = 'APPROVED'
            AND listing.active = TRUE
            AND listing.stock >= listing.min_order_qty
            """;
}
