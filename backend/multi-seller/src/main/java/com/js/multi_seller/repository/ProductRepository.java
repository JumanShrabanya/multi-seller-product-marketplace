package com.js.multi_seller.repository;

import com.js.multi_seller.dto.product.ProductSearchCriteria;
import com.js.multi_seller.dto.product.ProductSort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class ProductRepository {

    private static final String OFFER_SUMMARY = """
            WITH offer_summary AS (
                SELECT listing.product_id, MIN(listing.price) AS lowest_price, COUNT(*) AS offer_count
                FROM seller_listing listing
                JOIN seller ON seller.id = listing.seller_id
                WHERE %s
                GROUP BY listing.product_id
            )
            """.formatted(BuyerListingVisibility.WHERE_CLAUSE);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductSummaryRow> findProducts(ProductSearchCriteria criteria) {
        SqlParts sqlParts = buildFilters(criteria);
        String sql = OFFER_SUMMARY + """
                SELECT product.id, product.name, product.brand, product.category, product.unit,
                       offer_summary.lowest_price, COALESCE(offer_summary.offer_count, 0) AS offer_count
                FROM product
                LEFT JOIN offer_summary ON offer_summary.product_id = product.id
                WHERE %s
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(sqlParts.whereClause(), orderBy(criteria));

        MapSqlParameterSource parameters = sqlParts.parameters()
                .addValue("limit", criteria.size())
                .addValue("offset", (long) criteria.page() * criteria.size());
        return jdbcTemplate.query(sql, parameters, this::mapProductSummary);
    }

    public long countProducts(ProductSearchCriteria criteria) {
        SqlParts sqlParts = buildFilters(criteria);
        String sql = OFFER_SUMMARY + """
                SELECT COUNT(*)
                FROM product
                LEFT JOIN offer_summary ON offer_summary.product_id = product.id
                WHERE %s
                """.formatted(sqlParts.whereClause());
        Long result = jdbcTemplate.queryForObject(sql, sqlParts.parameters(), Long.class);
        return result == null ? 0 : result;
    }

    public ProductDetailRow findProductById(long productId) {
        String sql = OFFER_SUMMARY + """
                SELECT product.id, product.name, product.brand, product.category, product.unit, product.description,
                       offer_summary.lowest_price, COALESCE(offer_summary.offer_count, 0) AS offer_count
                FROM product
                LEFT JOIN offer_summary ON offer_summary.product_id = product.id
                WHERE product.id = :productId
                """;
        List<ProductDetailRow> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("productId", productId),
                this::mapProductDetail);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<OfferRow> findBuyableOffers(long productId) {
        String sql = """
                SELECT seller.id AS seller_id, seller.name AS seller_name, seller.city AS seller_city,
                       listing.price, listing.stock, listing.min_order_qty
                FROM seller_listing listing
                JOIN seller ON seller.id = listing.seller_id
                WHERE listing.product_id = :productId
                  AND %s
                ORDER BY listing.price ASC, seller.name ASC
                """.formatted(BuyerListingVisibility.WHERE_CLAUSE);
        return jdbcTemplate.query(sql, new MapSqlParameterSource("productId", productId), this::mapOffer);
    }

    public boolean existsById(long productId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM product WHERE id = :productId)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("productId", productId),
                Boolean.class));
    }

    public List<CatalogueProductRow> searchCatalogue(String query, int limit) {
        String filter = query == null ? "" : "WHERE LOWER(name) LIKE :query ESCAPE '\\' OR LOWER(brand) LIKE :query ESCAPE '\\' ";
        String sql = """
                SELECT id, name, brand, category, unit
                FROM product
                %sORDER BY name ASC
                LIMIT :limit
                """.formatted(filter);
        MapSqlParameterSource parameters = new MapSqlParameterSource("limit", limit);
        if (query != null) {
            parameters.addValue("query", LikePatterns.contains(query.toLowerCase()));
        }
        return jdbcTemplate.query(sql, parameters, this::mapCatalogueProduct);
    }

    public List<ProductRef> findProductRefs(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT id, name, brand FROM product WHERE id IN (:productIds)";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("productIds", productIds), this::mapProductRef);
    }

    private SqlParts buildFilters(ProductSearchCriteria criteria) {
        List<String> filters = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (criteria.query() != null) {
            filters.add("(LOWER(product.name) LIKE :query ESCAPE '\\' OR LOWER(product.brand) LIKE :query ESCAPE '\\')");
            parameters.addValue("query", LikePatterns.contains(criteria.query().toLowerCase()));
            parameters.addValue("relevanceQuery", criteria.query().toLowerCase());
        }
        if (criteria.category() != null) {
            filters.add("LOWER(product.category) = :category");
            parameters.addValue("category", criteria.category().toLowerCase());
        }
        if (criteria.brand() != null) {
            filters.add("LOWER(product.brand) = :brand");
            parameters.addValue("brand", criteria.brand().toLowerCase());
        }
        if (criteria.minPrice() != null) {
            filters.add("offer_summary.lowest_price >= :minPrice");
            parameters.addValue("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            filters.add("offer_summary.lowest_price <= :maxPrice");
            parameters.addValue("maxPrice", criteria.maxPrice());
        }
        if (criteria.inStockOnly()) {
            filters.add("offer_summary.offer_count IS NOT NULL");
        }
        return new SqlParts(filters.isEmpty() ? "TRUE" : String.join(" AND ", filters), parameters);
    }

    private String orderBy(ProductSearchCriteria criteria) {
        ProductSort sort = criteria.sort();
        return switch (sort) {
            case PRICE_ASC -> "offer_summary.lowest_price ASC NULLS LAST, product.name ASC";
            case PRICE_DESC -> "offer_summary.lowest_price DESC NULLS LAST, product.name ASC";
            case NAME -> "product.name ASC, product.id ASC";
            case RELEVANCE -> criteria.query() == null ? "product.name ASC, product.id ASC"
                    : "similarity(lower(product.name), :relevanceQuery) DESC, product.name ASC";
        };
    }

    private ProductSummaryRow mapProductSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductSummaryRow(resultSet.getLong("id"), resultSet.getString("name"), resultSet.getString("brand"),
                resultSet.getString("category"), resultSet.getString("unit"), resultSet.getBigDecimal("lowest_price"),
                resultSet.getInt("offer_count"));
    }

    private ProductDetailRow mapProductDetail(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductDetailRow(resultSet.getLong("id"), resultSet.getString("name"), resultSet.getString("brand"),
                resultSet.getString("category"), resultSet.getString("unit"), resultSet.getString("description"),
                resultSet.getBigDecimal("lowest_price"), resultSet.getInt("offer_count"));
    }

    private OfferRow mapOffer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OfferRow(resultSet.getLong("seller_id"), resultSet.getString("seller_name"),
                resultSet.getString("seller_city"), resultSet.getBigDecimal("price"), resultSet.getInt("stock"),
                resultSet.getInt("min_order_qty"));
    }

    private CatalogueProductRow mapCatalogueProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogueProductRow(resultSet.getLong("id"), resultSet.getString("name"),
                resultSet.getString("brand"), resultSet.getString("category"), resultSet.getString("unit"));
    }

    private ProductRef mapProductRef(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductRef(resultSet.getLong("id"), resultSet.getString("name"), resultSet.getString("brand"));
    }

    private record SqlParts(String whereClause, MapSqlParameterSource parameters) {
    }

    public record ProductSummaryRow(long id, String name, String brand, String category, String unit,
                                    BigDecimal lowestPrice, int offerCount) {
    }

    public record ProductDetailRow(long id, String name, String brand, String category, String unit, String description,
                                   BigDecimal lowestPrice, int offerCount) {
    }

    public record OfferRow(long sellerId, String sellerName, String sellerCity, BigDecimal price, int stock,
                           int minOrderQty) {
    }

    public record CatalogueProductRow(long id, String name, String brand, String category, String unit) {
    }

    public record ProductRef(long id, String name, String brand) {
    }
}
