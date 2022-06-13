package com.github.klefstad_teaching.cs122b.billing.rest;

import com.github.klefstad_teaching.cs122b.billing.model.request.OrderCompleteRequest;
import com.github.klefstad_teaching.cs122b.billing.model.response.GeneralResponse;
import com.github.klefstad_teaching.cs122b.billing.model.response.OrderDetailResponse;
import com.github.klefstad_teaching.cs122b.billing.model.response.OrderListResponse;
import com.github.klefstad_teaching.cs122b.billing.model.response.OrderPaymentResponse;
import com.github.klefstad_teaching.cs122b.billing.repo.BillingRepo;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Item;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Sale;
import com.github.klefstad_teaching.cs122b.billing.util.Validate;
import com.github.klefstad_teaching.cs122b.core.result.BillingResults;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
public class OrderController
{
    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);

    private final BillingRepo repo;
    private final Validate    validate;

    @Autowired
    public OrderController(BillingRepo repo,Validate validate)
    {
        this.repo = repo;
        this.validate = validate;
    }

    @GetMapping("/order/payment")
    public ResponseEntity<OrderPaymentResponse> orderPayment(
            @AuthenticationPrincipal SignedJWT user
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Item[] items = repo.retrieveAllItemsFromCart(userId, userRoles);

        if (items.length == 0) {
            return new OrderPaymentResponse()
                .setResult(BillingResults.CART_EMPTY)
                .toResponse()
                ;
        }

        BigDecimal total = calculateItemsTotal(items);
        Integer totalInCents = total.multiply(BigDecimal.valueOf(100)).intValue();

        LOG.info(String.valueOf(total));

        List<Object> paymentMethodTypes =
                new ArrayList<>();
        paymentMethodTypes.add("card");

        StringBuffer descriptionStringBuffer = new StringBuffer();
        for (int i=0; i< items.length; i++) {
            if (i != items.length-1) {
                descriptionStringBuffer.append(items[i].getMovieTitle());
                descriptionStringBuffer.append(", ");
            }
        }
        String description = descriptionStringBuffer.toString();

        Map<String, Long> metadata = new HashMap<>();
        metadata.put("userId", userId);

        Map<String, Object> params = new HashMap<>();
        params.put("amount", totalInCents);
        params.put("currency", "usd");
        params.put(
                "payment_method_types",
                paymentMethodTypes
        );
        params.put("description", description);
        params.put("metadata", metadata);

        PaymentIntent paymentIntent = null;
        try {
            paymentIntent =
                    PaymentIntent.create(params);
        } catch (StripeException e) {
            e.printStackTrace();
        }

        return new OrderPaymentResponse()
                .setPaymentIntentId(paymentIntent.getId())
                .setClientSecret(paymentIntent.getClientSecret())
                .setResult(BillingResults.ORDER_PAYMENT_INTENT_CREATED)
                .toResponse()
                ;
    }

    /**
     * Order Complete
     */
    @PostMapping("/order/complete")
    public ResponseEntity<GeneralResponse> orderComplete (
            @AuthenticationPrincipal SignedJWT user,
            @RequestBody OrderCompleteRequest request
            )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        String paymentIntentId = request.getPaymentIntentId();
        PaymentIntent paymentIntent = null;
        try {
            paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            e.printStackTrace();
        }

        validate.validatePaymentIntentPaymentStatus(paymentIntent);
        validate.validatePaymentIntentUserId(paymentIntent, userId);

        Item[] items = repo.retrieveAllItemsFromCart(userId, userRoles);

        // Insert a new billing.sale db record into billing.sale table
        BigDecimal total = BigDecimal.valueOf(paymentIntent.getAmount() / 100.0)
                .setScale(2, RoundingMode.DOWN);
//        BigDecimal total = calculateItemsTotal(items);

        repo.insertSaleRecord(userId, total, Timestamp.from(Instant.now()));

        // get the id of the latest order for that user id. From there you can create the billing.sale_item entries.
        Integer saleId = repo.getLatestSaleIdOfUser(userId);
        // Populate the billing.sale_item with the contents of the user's billing cart
        for (Item item : items) {
            repo.insertSaleItemRecord(saleId, item.getMovieId(), item.getQuantity());
        }

        // Clear the users cart
        repo.clearAllItemsFromCart(userId);

        return new GeneralResponse()
                .setResult(BillingResults.ORDER_COMPLETED)
                .toResponse()
                ;
    }

    /**
     * Order List
     */
    @GetMapping("/order/list")
    public ResponseEntity<OrderListResponse> orderList(
            @AuthenticationPrincipal SignedJWT user
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        // Return a list of sales found for the given user.
        // To keep this endpoint simple return only the last five sales ordered by the most recent sales first.

        Sale[] sales = repo.getSalesFoundForUser(userId);

        if (sales.length == 0) {
            return new OrderListResponse()
                    .setResult(BillingResults.ORDER_LIST_NO_SALES_FOUND)
                    .toResponse()
                    ;
        }

        return new OrderListResponse()
                .setSales(sales)
                .setResult(BillingResults.ORDER_LIST_FOUND_SALES)
                .toResponse()
                ;
    }

    /**
     * Order Detail
     */
    @GetMapping("/order/detail/{saleId}")
    public ResponseEntity<OrderDetailResponse> orderDetail(
            @AuthenticationPrincipal SignedJWT user,
            @PathVariable Long saleId
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Item[] items = repo.retrieveUserItemsBySaleId(userId, saleId, userRoles);
        if (items.length == 0) {
            return new OrderDetailResponse()
                    .setResult(BillingResults.ORDER_DETAIL_NOT_FOUND)
                    .toResponse()
                    ;
        }

        BigDecimal total = calculateItemsTotal(items);
//        BigDecimal total = repo.retrieveUserItemsTotalBySaleId(userId, saleId);

        return new OrderDetailResponse()
                .setItems(items)
                .setTotal(total)
                .setResult(BillingResults.ORDER_DETAIL_FOUND)
                .toResponse()
                ;
    }



    // Helper Functions
    private JWTClaimsSet getUserJWTClaimsSet(SignedJWT user) {
        JWTClaimsSet userJWTClaimsSet = null;
        try {
            userJWTClaimsSet = user.getJWTClaimsSet();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return userJWTClaimsSet;
    }

    private Long getUserID(JWTClaimsSet userJWTClaimsSet) {
        Long userID = null;
        try {
            userID = userJWTClaimsSet.getLongClaim(JWTManager.CLAIM_ID);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return userID;
    }

    private List<String> getUserRoles(JWTClaimsSet userJWTClaimsSet) {
        List<String> userRoles = null;
        try {
            userRoles = userJWTClaimsSet.getStringListClaim(JWTManager.CLAIM_ROLES);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return userRoles;
    }

    private BigDecimal calculateItemsTotal(Item[] items) {
        BigDecimal total = BigDecimal.ZERO;
        total = Stream.of(items)
                .map(item -> {return item
                        .getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        ;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = total.setScale(2, RoundingMode.DOWN);

        return total;
    }
}
