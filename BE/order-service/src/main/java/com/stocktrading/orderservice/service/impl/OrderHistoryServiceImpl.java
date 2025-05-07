package com.stocktrading.orderservice.service.impl;

import com.stocktrading.orderservice.common.BaseResponse;
import com.stocktrading.orderservice.common.Const;
import com.stocktrading.orderservice.common.PagingResponse;
import com.stocktrading.orderservice.model.Order;
import com.stocktrading.orderservice.payload.request.GetOrderRequest;
import com.stocktrading.orderservice.payload.response.GetOrdersResponse;
import com.stocktrading.orderservice.repository.OrderRepository;
import com.stocktrading.orderservice.service.OrderHistoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class OrderHistoryServiceImpl implements OrderHistoryService {
    private final MongoTemplate mongoTemplate;

    private final OrderRepository orderRepository;

    @Override
    public BaseResponse<?> getOrders(GetOrderRequest request) {
        String userId = request.getUserId();
        List<String> accountIds = request.getAccountIds() == null ? List.of() : request.getAccountIds();
        List<String> timeInForces = request.getTimeInForces() == null ? List.of() : request.getTimeInForces();
        List<String> orderTypes = request.getOrderTypes() == null ? List.of() : request.getOrderTypes();
        List<String> statuses = request.getStatuses() == null ? List.of() : request.getStatuses();
        List<String> sides = request.getSides() == null ? List.of() : request.getSides();
        List<String> stockSymbols = request.getStockSymbols() == null ? List.of() : request.getStockSymbols();
        String startDate = request.getStartDate() == null ? "" : request.getStartDate();
        String endDate = request.getEndDate() == null ? "" : request.getEndDate();
        int page = request.getPage();
        int size = request.getSize();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Query query = new Query();

        query.addCriteria(Criteria.where("userId").is(userId));

        if (!accountIds.isEmpty()) {
            query.addCriteria(Criteria.where("accountId").in(accountIds));
        }
        if (!timeInForces.isEmpty()) {
            query.addCriteria(Criteria.where("timeInForce").in(timeInForces));
        }
        if (!orderTypes.isEmpty()) {
            query.addCriteria(Criteria.where("orderType").in(orderTypes));
        }
        if (!statuses.isEmpty()) {
            query.addCriteria(Criteria.where("status").in(statuses));
        }
        if (!sides.isEmpty()) {
            query.addCriteria(Criteria.where("side").in(sides));
        }
        if (!stockSymbols.isEmpty()) {
            query.addCriteria(Criteria.where("stockSymbol").in(stockSymbols));
        }

        LocalDate startLocalDate = LocalDate.parse(startDate.isEmpty() ? "1970-01-01" : startDate, dateFormatter);
        LocalDate endLocalDate = LocalDate.parse(endDate.isEmpty() ? "9999-12-31" : endDate, dateFormatter);
        LocalTime defaultStartTime = LocalTime.parse("00:00:00", timeFormatter);
        LocalTime defaultEndTime = LocalTime.parse("23:59:59", timeFormatter);
        LocalDateTime startDateTime = LocalDateTime.of(startLocalDate, defaultStartTime);
        LocalDateTime endDateTime = LocalDateTime.of(endLocalDate, defaultEndTime);
        query.addCriteria(Criteria.where("createdAt").gte(startDateTime).lte(endDateTime));

        long totalOrders = mongoTemplate.find(query, Order.class).size();
        Pageable pageable = PageRequest.of(page, size);
        query.with(pageable);
        List<Order> orders = mongoTemplate.find(query, Order.class);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Retrieve orders successfully",
            new GetOrdersResponse(
                orders,
                new PagingResponse(
                        page, size, totalOrders, (int) Math.ceil((double) totalOrders / orders.size())
                )
            )
        );
    }

    @Override
    public BaseResponse<?> getOrderDetails(String orderId) {
        Order order = orderRepository.getById(orderId).orElse(null);
        if (order == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Order not found with orderId: " + orderId,
                ""
            );
        }
        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Order retrieve successfully",
            order
        );
    }
}
