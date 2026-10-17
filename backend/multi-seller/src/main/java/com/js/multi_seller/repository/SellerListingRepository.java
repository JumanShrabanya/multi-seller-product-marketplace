package com.js.multi_seller.repository;

import com.js.multi_seller.entity.SellerListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerListingRepository extends JpaRepository<SellerListing, Long> {

    Page<SellerListing> findBySellerId(Long sellerId, Pageable pageable);

    Optional<SellerListing> findByIdAndSellerId(Long id, Long sellerId);

    Optional<SellerListing> findBySellerIdAndProductId(Long sellerId, Long productId);
}
