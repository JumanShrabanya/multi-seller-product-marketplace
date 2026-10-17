package com.js.multi_seller.repository;

final class LikePatterns {

    private LikePatterns() {
    }

    static String contains(String value) {
        return "%" + escape(value) + "%";
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
