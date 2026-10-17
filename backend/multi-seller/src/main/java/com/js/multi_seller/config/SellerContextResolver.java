package com.js.multi_seller.config;

import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.entity.Seller;
import com.js.multi_seller.exception.BadRequestException;
import com.js.multi_seller.exception.NotFoundException;
import com.js.multi_seller.repository.SellerRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class SellerContextResolver implements HandlerMethodArgumentResolver {

    public static final String SELLER_HEADER = "X-Seller-Id";

    private final SellerRepository sellerRepository;

    public SellerContextResolver(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(SellerContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(SELLER_HEADER);
        if (header == null || header.isBlank()) {
            throw new BadRequestException(SELLER_HEADER + " header is required");
        }
        long sellerId;
        try {
            sellerId = Long.parseLong(header.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException(SELLER_HEADER + " header must be a numeric seller id");
        }
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller " + sellerId + " was not found"));
        return new SellerContext(seller.getId(), seller.getName(), seller.getCity(), seller.getStatus());
    }
}
