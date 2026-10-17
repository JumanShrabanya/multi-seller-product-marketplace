package com.js.multi_seller.service;

import com.js.multi_seller.exception.BadRequestException;
import com.js.multi_seller.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductService productService = new ProductService(productRepository);

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, null, null, false, null, -1, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("page");
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, null, null, false, null, 0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size");
    }

    @Test
    void rejectsZeroSize() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, null, null, false, null, 0, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNegativePriceFilter() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, new BigDecimal("-1"), null, false, null, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("price");
    }

    @Test
    void rejectsMinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, new BigDecimal("100"), new BigDecimal("50"),
                false, null, 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minPrice");
    }

    @Test
    void rejectsUnknownSort() {
        assertThatThrownBy(() -> productService.findProducts(null, null, null, null, null, false, "bogus", 0, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sort");
    }

    @Test
    void catalogueSearchRejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> productService.searchCatalogue("cement", 21))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("size");
    }
}
