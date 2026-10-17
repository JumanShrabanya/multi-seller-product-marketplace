package com.js.multi_seller.config;

import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.entity.Seller;
import com.js.multi_seller.entity.UserRole;
import com.js.multi_seller.exception.ForbiddenException;
import com.js.multi_seller.exception.UnauthorizedException;
import com.js.multi_seller.repository.SellerRepository;
import com.js.multi_seller.service.SessionStore;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class SellerContextResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SellerRepository sellerRepository;
    private final SessionStore sessionStore;

    public SellerContextResolver(SellerRepository sellerRepository, SessionStore sessionStore) {
        this.sellerRepository = sellerRepository;
        this.sessionStore = sessionStore;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(SellerContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Authorization: Bearer token is required");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        SessionStore.Session session = sessionStore.find(token)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (session.role() != UserRole.SELLER) {
            throw new ForbiddenException("A buyer account cannot manage listings");
        }
        Seller seller = sellerRepository.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        return new SellerContext(seller.getId(), seller.getName(), seller.getCity(), seller.getStatus());
    }
}
