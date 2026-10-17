package com.js.multi_seller.service;

import com.js.multi_seller.dto.PageResponse;
import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.dto.seller.SellerListingRequest;
import com.js.multi_seller.dto.seller.SellerListingResponse;
import com.js.multi_seller.dto.seller.SellerListingUpdateRequest;
import com.js.multi_seller.entity.SellerListing;
import com.js.multi_seller.exception.BadRequestException;
import com.js.multi_seller.exception.ConflictException;
import com.js.multi_seller.exception.ForbiddenException;
import com.js.multi_seller.exception.NotFoundException;
import com.js.multi_seller.repository.ProductRepository;
import com.js.multi_seller.repository.SellerListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SellerListingService {

    private final SellerListingRepository listingRepository;
    private final ProductRepository productRepository;

    public SellerListingService(SellerListingRepository listingRepository, ProductRepository productRepository) {
        this.listingRepository = listingRepository;
        this.productRepository = productRepository;
    }

    public PageResponse<SellerListingResponse> list(SellerContext seller, int page, int size) {
        Pagination.validate(page, size);
        Page<SellerListing> listings = listingRepository.findBySellerId(seller.id(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, ProductRepository.ProductRef> products = productRefs(listings.getContent());
        List<SellerListingResponse> content = listings.getContent().stream()
                .map(listing -> toResponse(listing, products.get(listing.getProductId())))
                .toList();
        return new PageResponse<>(content, page, size, listings.getTotalElements(), listings.getTotalPages());
    }

    @Transactional
    public SellerListingResponse create(SellerContext seller, SellerListingRequest request) {
        requireApproved(seller);
        validateQuantities(request.stock(), request.minOrderQty());
        if (!productRepository.existsById(request.productId())) {
            throw new NotFoundException("Product " + request.productId() + " was not found");
        }

        SellerListing listing = listingRepository.findBySellerIdAndProductId(seller.id(), request.productId())
                .map(existing -> reactivate(existing, request))
                .orElseGet(() -> newListing(seller, request));
        return toResponse(listingRepository.saveAndFlush(listing));
    }

    @Transactional
    public SellerListingResponse update(SellerContext seller, long listingId, SellerListingUpdateRequest request) {
        requireApproved(seller);
        validateQuantities(request.stock(), request.minOrderQty());
        SellerListing listing = ownedListing(seller, listingId);
        if (!listing.getVersion().equals(request.version())) {
            throw new ConflictException("Listing was modified by another request; refresh and try again");
        }
        listing.setPrice(request.price());
        listing.setStock(request.stock());
        listing.setMinOrderQty(request.minOrderQty());
        return toResponse(listingRepository.saveAndFlush(listing));
    }

    @Transactional
    public void stopSelling(SellerContext seller, long listingId) {
        requireApproved(seller);
        SellerListing listing = ownedListing(seller, listingId);
        listing.setActive(false);
        listingRepository.saveAndFlush(listing);
    }

    private SellerListing newListing(SellerContext seller, SellerListingRequest request) {
        SellerListing listing = new SellerListing(request.productId(), seller.id());
        applyDetails(listing, request.price(), request.stock(), request.minOrderQty());
        return listing;
    }

    private SellerListing reactivate(SellerListing existing, SellerListingRequest request) {
        if (existing.isActive()) {
            throw new ConflictException("Product " + request.productId()
                    + " is already listed; update the existing listing instead");
        }
        existing.setActive(true);
        applyDetails(existing, request.price(), request.stock(), request.minOrderQty());
        return existing;
    }

    private void applyDetails(SellerListing listing, BigDecimal price, int stock, int minOrderQty) {
        listing.setPrice(price);
        listing.setStock(stock);
        listing.setMinOrderQty(minOrderQty);
    }

    private SellerListing ownedListing(SellerContext seller, long listingId) {
        return listingRepository.findByIdAndSellerId(listingId, seller.id())
                .orElseThrow(() -> new NotFoundException("Listing " + listingId + " was not found"));
    }

    private void requireApproved(SellerContext seller) {
        if (!seller.isApproved()) {
            throw new ForbiddenException("Seller " + seller.id()
                    + " is " + seller.status() + " and cannot modify listings");
        }
    }

    private void validateQuantities(int stock, int minOrderQty) {
        if (stock > 0 && minOrderQty > stock) {
            throw new BadRequestException("minOrderQty must not exceed stock when stock is positive");
        }
    }

    private Map<Long, ProductRepository.ProductRef> productRefs(List<SellerListing> listings) {
        List<Long> productIds = listings.stream().map(SellerListing::getProductId).toList();
        return productRepository.findProductRefs(productIds).stream()
                .collect(Collectors.toMap(ProductRepository.ProductRef::id, Function.identity()));
    }

    private SellerListingResponse toResponse(SellerListing listing) {
        ProductRepository.ProductRef product = productRepository.findProductRefs(List.of(listing.getProductId()))
                .stream().findFirst().orElse(null);
        return toResponse(listing, product);
    }

    private SellerListingResponse toResponse(SellerListing listing, ProductRepository.ProductRef product) {
        return new SellerListingResponse(
                listing.getId(),
                listing.getProductId(),
                product == null ? "Unknown product" : product.name(),
                product == null ? null : product.brand(),
                listing.getPrice(),
                listing.getStock(),
                listing.getMinOrderQty(),
                listing.isActive(),
                listing.getVersion(),
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }
}
