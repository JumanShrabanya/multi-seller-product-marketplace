package com.js.multi_seller.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message, List<FieldError> fieldErrors) {
}
