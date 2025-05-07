package com.stocktrading.orderservice.repository;

import com.stocktrading.orderservice.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Find orders by user ID
     */
    List<Order> findByUserId(String userId);

    /**
     * Find orders by account ID
     */
    List<Order> findByAccountId(String accountId);

    /**
     * Find orders by stock symbol
     */
    List<Order> findByStockSymbol(String stockSymbol);

    /**
     * Find by user ID with pagination
     */
    Page<Order> findByUserId(String userId, Pageable pageable);

    /**
     * Find by account ID with pagination
     */
    Page<Order> findByAccountId(String accountId, Pageable pageable);

    /**
     * Find orders by saga ID
     */
    Optional<Order> findBySagaId(String sagaId);

    /**
     * Find orders by broker order ID
     */
    Optional<Order> findByBrokerOrderId(String brokerOrderId);

    /**
     * Find by account ID and status
     */
    List<Order> findByAccountIdAndStatus(String accountId, Order.OrderStatus status);

    /**
     * Find by user ID and status
     */
    List<Order> findByUserIdAndStatus(String userId, Order.OrderStatus status);

    /**
     * Find active orders (not in terminal state)
     */
    @Query("{ 'status' : { $nin : ['COMPLETED', 'CANCELLED', 'REJECTED', 'FAILED', 'EXPIRED'] } }")
    List<Order> findActiveOrders();

    /**
     * Find orders created before a certain time that are not in a terminal state
     */
    @Query("{ 'createdAt' : { $lt : ?0 }, 'status' : { $nin : ['COMPLETED', 'CANCELLED', 'REJECTED', 'FAILED', 'EXPIRED'] } }")
    List<Order> findPotentiallyStaleOrders(Instant cutoffTime);

    Optional<Order> getById(String id);
}
