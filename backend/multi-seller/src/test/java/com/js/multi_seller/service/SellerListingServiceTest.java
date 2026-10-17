package com.js.multi_seller.service;

import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.dto.seller.SellerListingRequest;
import com.js.multi_seller.dto.seller.SellerListingUpdateRequest;
import com.js.multi_seller.entity.SellerListing;
import com.js.multi_seller.entity.SellerStatus;
import com.js.multi_seller.exception.BadRequestException;
import com.js.multi_seller.exception.ConflictException;
import com.js.multi_seller.exception.ForbiddenException;
import com.js.multi_seller.exception.NotFoundException;
import com.js.multi_seller.repository.ProductRepository;
import com.js.multi_seller.repository.SellerListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerListingServiceTest {

    private static final long PRODUCT_ID = 10L;
    private static final long LISTING_ID = 99L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private static final SellerContext APPROVED_SELLER =
            new SellerContext(1L, "BuildRight Supplies", "Mumbai", SellerStatus.APPROVED);
    private static final SellerContext PENDING_SELLER =
            new SellerContext(4L, "New Horizon Traders", "Nashik", SellerStatus.PENDING);

    @Mock
    private SellerListingRepository listingRepository;

    @Mock
    private ProductRepository productRepository;

    private SellerListingService sellerListingService;

    @BeforeEach
    void setUp() {
        sellerListingService = new SellerListingService(listingRepository, productRepository);
    }

    @Test
    void createRejectsNonApprovedSeller() {
        SellerListingRequest request = new SellerListingRequest(PRODUCT_ID, PRICE, 10, 1);

        assertThatThrownBy(() -> sellerListingService.create(PENDING_SELLER, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createRejectsMinOrderGreaterThanStock() {
        SellerListingRequest request = new SellerListingRequest(PRODUCT_ID, PRICE, 5, 50);

        assertThatThrownBy(() -> sellerListingService.create(APPROVED_SELLER, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minOrderQty");
    }

    @Test
    void createRejectsUnknownProduct() {
        SellerListingRequest request = new SellerListingRequest(PRODUCT_ID, PRICE, 10, 1);
        when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> sellerListingService.create(APPROVED_SELLER, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void createRejectsDuplicateActiveListing() {
        SellerListingRequest request = new SellerListingRequest(PRODUCT_ID, PRICE, 10, 1);
        SellerListing existing = new SellerListing(PRODUCT_ID, APPROVED_SELLER.id());
        existing.setActive(true);

        when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);
        when(listingRepository.findBySellerIdAndProductId(APPROVED_SELLER.id(), PRODUCT_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> sellerListingService.create(APPROVED_SELLER, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already listed");
    }

    @Test
    void updateRejectsNonApprovedSeller() {
        SellerListingUpdateRequest request = new SellerListingUpdateRequest(PRICE, 10, 1, 0L);

        assertThatThrownBy(() -> sellerListingService.update(PENDING_SELLER, LISTING_ID, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRejectsListingOwnedByAnotherSeller() {
        SellerListingUpdateRequest request = new SellerListingUpdateRequest(PRICE, 10, 1, 0L);
        when(listingRepository.findByIdAndSellerId(LISTING_ID, APPROVED_SELLER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerListingService.update(APPROVED_SELLER, LISTING_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsStaleVersion() {
        SellerListingUpdateRequest request = new SellerListingUpdateRequest(PRICE, 10, 1, 5L);
        SellerListing listing = new SellerListing(PRODUCT_ID, APPROVED_SELLER.id());
        ReflectionTestUtils.setField(listing, "version", 0L);

        when(listingRepository.findByIdAndSellerId(LISTING_ID, APPROVED_SELLER.id()))
                .thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> sellerListingService.update(APPROVED_SELLER, LISTING_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modified");
    }

    @Test
    void stopSellingRejectsNonApprovedSeller() {
        assertThatThrownBy(() -> sellerListingService.stopSelling(PENDING_SELLER, LISTING_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void stopSellingRejectsListingOwnedByAnotherSeller() {
        when(listingRepository.findByIdAndSellerId(LISTING_ID, APPROVED_SELLER.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerListingService.stopSelling(APPROVED_SELLER, LISTING_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
