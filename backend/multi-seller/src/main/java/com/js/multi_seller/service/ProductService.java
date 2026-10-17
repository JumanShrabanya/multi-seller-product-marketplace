package com.js.multi_seller.service;

import com.js.multi_seller.dto.PageResponse;
import com.js.multi_seller.dto.product.OfferResponse;
import com.js.multi_seller.dto.product.ProductDetailResponse;
import com.js.multi_seller.dto.product.ProductSearchCriteria;
import com.js.multi_seller.dto.product.ProductSort;
import com.js.multi_seller.dto.product.ProductSummaryResponse;
import com.js.multi_seller.dto.seller.CatalogueProductResponse;
import com.js.multi_seller.exception.BadRequestException;
import com.js.multi_seller.exception.NotFoundException;
import com.js.multi_seller.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private static final int MAX_CATALOGUE_RESULTS = 20;
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductSummaryResponse> findProducts(String query, String category, String brand,
                                                             BigDecimal minPrice, BigDecimal maxPrice,
                                                             boolean inStockOnly, String sort, int page, int size) {
        validatePaginationAndPrices(minPrice, maxPrice, page, size);
        ProductSearchCriteria criteria = new ProductSearchCriteria(normalize(query), normalize(category), normalize(brand),
                minPrice, maxPrice, inStockOnly, ProductSort.from(sort), page, size);
        long totalElements = productRepository.countProducts(criteria);
        List<ProductSummaryResponse> products = productRepository.findProducts(criteria).stream()
                .map(row -> new ProductSummaryResponse(row.id(), row.name(), row.brand(), row.category(), row.unit(),
                        row.lowestPrice(), row.offerCount(), row.offerCount() == 0))
                .toList();
        return new PageResponse<>(products, page, size, totalElements, totalPages(totalElements, size));
    }

    public List<CatalogueProductResponse> searchCatalogue(String query, int size) {
        if (size < 1 || size > MAX_CATALOGUE_RESULTS) {
            throw new BadRequestException("size must be between 1 and " + MAX_CATALOGUE_RESULTS);
        }
        return productRepository.searchCatalogue(normalize(query), size).stream()
                .map(row -> new CatalogueProductResponse(row.id(), row.name(), row.brand(), row.category(), row.unit()))
                .toList();
    }

    public ProductDetailResponse findProduct(long productId) {
        ProductRepository.ProductDetailRow product = productRepository.findProductById(productId);
        if (product == null) {
            throw new NotFoundException("Product " + productId + " was not found");
        }
        List<OfferResponse> offers = productRepository.findBuyableOffers(productId).stream()
                .map(row -> new OfferResponse(row.sellerId(), row.sellerName(), row.sellerCity(), row.price(),
                        row.stock(), row.minOrderQty()))
                .toList();
        return new ProductDetailResponse(product.id(), product.name(), product.brand(), product.category(), product.unit(),
                product.description(), product.lowestPrice(), product.offerCount(), offers.isEmpty(), offers);
    }

    private void validatePaginationAndPrices(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Pagination.validate(page, size);
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new BadRequestException("price filters must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must be less than or equal to maxPrice");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int totalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }
}
