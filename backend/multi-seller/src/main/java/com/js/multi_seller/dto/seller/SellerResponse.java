package com.js.multi_seller.dto.seller;

import com.js.multi_seller.entity.SellerStatus;

public record SellerResponse(long id, String name, String city, SellerStatus status) {
}
