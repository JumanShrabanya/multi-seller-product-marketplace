package com.js.multi_seller.dto.auth;

import com.js.multi_seller.entity.SellerStatus;

public record AuthResponse(String token, String role, long userId, String name, String email, SellerStatus sellerStatus) {
}
