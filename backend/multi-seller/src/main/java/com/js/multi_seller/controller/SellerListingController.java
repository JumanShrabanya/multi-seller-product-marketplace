package com.js.multi_seller.controller;

import com.js.multi_seller.dto.PageResponse;
import com.js.multi_seller.dto.seller.SellerContext;
import com.js.multi_seller.dto.seller.SellerListingRequest;
import com.js.multi_seller.dto.seller.SellerListingResponse;
import com.js.multi_seller.dto.seller.SellerListingUpdateRequest;
import com.js.multi_seller.service.SellerListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/listings")
public class SellerListingController {

    private final SellerListingService sellerListingService;

    public SellerListingController(SellerListingService sellerListingService) {
        this.sellerListingService = sellerListingService;
    }

    @GetMapping
    public PageResponse<SellerListingResponse> getListings(
            SellerContext seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return sellerListingService.list(seller, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellerListingResponse createListing(SellerContext seller,
                                               @Valid @RequestBody SellerListingRequest request) {
        return sellerListingService.create(seller, request);
    }

    @PatchMapping("/{id}")
    public SellerListingResponse updateListing(SellerContext seller,
                                               @PathVariable long id,
                                               @Valid @RequestBody SellerListingUpdateRequest request) {
        return sellerListingService.update(seller, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stopSelling(SellerContext seller, @PathVariable long id) {
        sellerListingService.stopSelling(seller, id);
    }
}
