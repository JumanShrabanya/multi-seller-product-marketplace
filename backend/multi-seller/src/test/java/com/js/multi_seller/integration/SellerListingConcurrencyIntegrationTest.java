package com.js.multi_seller.integration;

import com.js.multi_seller.AbstractIntegrationTest;
import com.js.multi_seller.entity.SellerListing;
import com.js.multi_seller.repository.SellerListingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SellerListingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SellerListingRepository listingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long listingId;

    @BeforeEach
    void createDisposableListing() {
        listingRepository.findBySellerIdAndProductId(1L, 10L).ifPresent(listingRepository::delete);
        SellerListing listing = new SellerListing(10L, 1L);
        listing.setPrice(new BigDecimal("100.00"));
        listing.setStock(10);
        listing.setMinOrderQty(1);
        listingId = listingRepository.saveAndFlush(listing).getId();
    }

    @AfterEach
    void removeDisposableListing() {
        listingRepository.findById(listingId).ifPresent(listingRepository::delete);
    }

    @Test
    void concurrentUpdatesLetExactlyOneSucceed() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        CyclicBarrier bothHaveRead = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> update = () -> {
            try {
                transaction.executeWithoutResult(status -> {
                    SellerListing listing = listingRepository.findById(listingId).orElseThrow();
                    await(bothHaveRead);
                    listing.setPrice(listing.getPrice().add(BigDecimal.ONE));
                    listingRepository.saveAndFlush(listing);
                });
                return true;
            } catch (OptimisticLockingFailureException exception) {
                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(update);
            Future<Boolean> second = executor.submit(update);
            boolean firstSucceeded = first.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(30, TimeUnit.SECONDS);
            assertThat(firstSucceeded ^ secondSucceeded).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
