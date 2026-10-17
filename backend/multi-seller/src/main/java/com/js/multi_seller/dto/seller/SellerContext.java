package com.js.multi_seller.dto.seller;

import com.js.multi_seller.entity.SellerStatus;

public record SellerContext(long id, String name, String city, SellerStatus status) {

    public boolean isApproved() {
        return status == SellerStatus.APPROVED;
    }
}
