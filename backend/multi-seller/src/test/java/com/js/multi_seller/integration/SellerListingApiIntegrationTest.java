package com.js.multi_seller.integration;

import com.jayway.jsonpath.JsonPath;
import com.js.multi_seller.AbstractIntegrationTest;
import com.js.multi_seller.repository.SellerListingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerListingApiIntegrationTest extends AbstractIntegrationTest {

    private static final long SELLER_ID = 1L;
    private static final long OTHER_SELLER_ID = 2L;
    private static final long PENDING_SELLER_ID = 4L;
    private static final long PRODUCT_ID = 10L;
    private static final String CREATE_BODY = "{\"productId\":10,\"price\":100.00,\"stock\":10,\"minOrderQty\":2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SellerListingRepository listingRepository;

    @BeforeEach
    void cleanBefore() {
        removeListing();
    }

    @AfterEach
    void cleanAfter() {
        removeListing();
    }

    private void removeListing() {
        listingRepository.findBySellerIdAndProductId(SELLER_ID, PRODUCT_ID).ifPresent(listingRepository::delete);
    }

    @Test
    void requiresSellerHeader() throws Exception {
        mockMvc.perform(get("/api/seller/listings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void createsListingWithVersionZero() throws Exception {
        createListing()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void duplicateActiveListingReturnsConflict() throws Exception {
        createListing().andExpect(status().isCreated());
        createListing()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void stoppingThenReAddingReactivatesSameListing() throws Exception {
        long id = createListingId();

        mockMvc.perform(delete("/api/seller/listings/{id}", id).header("X-Seller-Id", SELLER_ID))
                .andExpect(status().isNoContent());

        createListing()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void rejectsStaleVersionOnUpdate() throws Exception {
        long id = createListingId();

        mockMvc.perform(patch("/api/seller/listings/{id}", id)
                        .header("X-Seller-Id", SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":110.00,\"stock\":9,\"minOrderQty\":1,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/seller/listings/{id}", id)
                        .header("X-Seller-Id", SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":120.00,\"stock\":8,\"minOrderQty\":1,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void cannotModifyAnotherSellersListing() throws Exception {
        long id = createListingId();

        mockMvc.perform(patch("/api/seller/listings/{id}", id)
                        .header("X-Seller-Id", OTHER_SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":110.00,\"stock\":9,\"minOrderQty\":1,\"version\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendingSellerCannotCreateListing() throws Exception {
        mockMvc.perform(post("/api/seller/listings")
                        .header("X-Seller-Id", PENDING_SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void pendingSellerCanStillReadOwnListings() throws Exception {
        mockMvc.perform(get("/api/seller/listings").header("X-Seller-Id", PENDING_SELLER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMinOrderGreaterThanStock() throws Exception {
        mockMvc.perform(post("/api/seller/listings")
                        .header("X-Seller-Id", SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":10,\"price\":100.00,\"stock\":5,\"minOrderQty\":50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsFieldErrorsForInvalidPrice() throws Exception {
        mockMvc.perform(post("/api/seller/listings")
                        .header("X-Seller-Id", SELLER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":10,\"price\":-1,\"stock\":5,\"minOrderQty\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    private ResultActions createListing() throws Exception {
        return mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", SELLER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY));
    }

    private long createListingId() throws Exception {
        MvcResult result = createListing().andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
