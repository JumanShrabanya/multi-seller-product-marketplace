package com.js.multi_seller.service;

import com.js.multi_seller.dto.auth.AuthResponse;
import com.js.multi_seller.dto.auth.LoginRequest;
import com.js.multi_seller.entity.Buyer;
import com.js.multi_seller.entity.Seller;
import com.js.multi_seller.entity.SellerStatus;
import com.js.multi_seller.entity.UserRole;
import com.js.multi_seller.exception.UnauthorizedException;
import com.js.multi_seller.repository.BuyerRepository;
import com.js.multi_seller.repository.SellerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final SellerRepository sellerRepository;
    private final BuyerRepository buyerRepository;
    private final SessionStore sessionStore;

    public AuthService(SellerRepository sellerRepository, BuyerRepository buyerRepository, SessionStore sessionStore) {
        this.sellerRepository = sellerRepository;
        this.buyerRepository = buyerRepository;
        this.sessionStore = sessionStore;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String password = request.password();

        Optional<Seller> seller = sellerRepository.findByEmail(email);
        if (seller.isPresent() && seller.get().getPassword().equals(password)) {
            return issue(seller.get().getId(), UserRole.SELLER, seller.get().getName(), seller.get().getEmail(),
                    seller.get().getStatus());
        }

        Optional<Buyer> buyer = buyerRepository.findByEmail(email);
        if (buyer.isPresent() && buyer.get().getPassword().equals(password)) {
            return issue(buyer.get().getId(), UserRole.BUYER, buyer.get().getName(), buyer.get().getEmail(), null);
        }

        throw new UnauthorizedException(INVALID_CREDENTIALS);
    }

    private AuthResponse issue(long userId, UserRole role, String name, String email, SellerStatus status) {
        SessionStore.Session session = sessionStore.create(userId, role);
        return new AuthResponse(session.token(), role.name(), userId, name, email, status);
    }
}
