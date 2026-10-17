package com.js.multi_seller.integration;

import com.js.multi_seller.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededProductsWithDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void paginatesWithRequestedSize() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(4));
    }

    @Test
    void rejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownSort() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchesByName() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "cement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void searchesByBrand() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "ultratech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("OPC Cement 53 Grade"));
    }

    @Test
    void treatsWildcardCharactersLiterally() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersByCategory() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "Cement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filtersByBrand() throws Exception {
        mockMvc.perform(get("/api/products").param("brand", "UltraTech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filtersByLowestVisibleOfferPrice() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "cement").param("minPrice", "400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("OPC Cement 53 Grade"));
    }

    @Test
    void inStockOnlyHidesProductsWithoutBuyableOffers() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "Sand").param("inStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/products").param("category", "Sand").param("inStockOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].currentlyUnavailable").value(true));
    }

    @Test
    void sortsByLowestPriceAscendingPuttingUnavailableLast() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "price_asc").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].lowestPrice").value(8.2));
    }

    @Test
    void productDetailExcludesNonApprovedSellers() throws Exception {
        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerCount").value(1))
                .andExpect(jsonPath("$.currentlyUnavailable").value(false))
                .andExpect(jsonPath("$.offers.length()").value(1))
                .andExpect(jsonPath("$.offers[0].sellerId").value(2))
                .andExpect(jsonPath("$.offers[0].price").value(3150.0));
    }

    @Test
    void productDetailReportsCurrentlyUnavailableWhenNoBuyableOffers() throws Exception {
        mockMvc.perform(get("/api/products/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerCount").value(0))
                .andExpect(jsonPath("$.currentlyUnavailable").value(true))
                .andExpect(jsonPath("$.offers.length()").value(0));
    }

    @Test
    void unknownProductReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound());
    }
}
