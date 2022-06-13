package com.github.klefstad_teaching.cs122b.billing.rest;

import com.github.klefstad_teaching.cs122b.billing.model.request.CartInsertRequest;
import com.github.klefstad_teaching.cs122b.billing.model.request.CartUpdateRequest;
import com.github.klefstad_teaching.cs122b.billing.model.response.CartRetrieveResponse;
import com.github.klefstad_teaching.cs122b.billing.model.response.GeneralResponse;
import com.github.klefstad_teaching.cs122b.billing.model.response.OrderDetailResponse;
import com.github.klefstad_teaching.cs122b.billing.repo.BillingRepo;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Item;
import com.github.klefstad_teaching.cs122b.billing.util.Validate;
import com.github.klefstad_teaching.cs122b.core.result.BillingResults;
import com.github.klefstad_teaching.cs122b.core.security.JWTManager;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Stream;


@RestController
public class CartController
{
    private static final Logger LOG = LoggerFactory.getLogger(CartController.class);

    private final BillingRepo repo;
    private final Validate    validate;

    @Autowired
    public CartController(BillingRepo repo, Validate validate)
    {
        this.repo = repo;
        this.validate = validate;
    }

    /**
     * Cart Insert
     */
    @PostMapping("/cart/insert")
    public ResponseEntity<GeneralResponse> cartInsert(
        @AuthenticationPrincipal SignedJWT user,
        @RequestBody CartInsertRequest request
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Long movieId = request.getMovieId();
        Integer quantity = request.getQuantity();

        // Validate request parameters
        validate.validateRequestMovieQuantity(quantity);
        repo.checkConflictItemAlreadyInCart(userId, movieId);

        // Insert the movieId into a user's cart with the given quantity
        // Subtract the given quantity from the sale_item with the same movieId
        repo.insertMovieToCart(userId, movieId, quantity);

        return new GeneralResponse()
                .setResult(BillingResults.CART_ITEM_INSERTED)
                .toResponse();
    }

    /**
     * Cart Update
     */
    @PostMapping("/cart/update")
    public ResponseEntity<GeneralResponse> cartUpdate(
            @AuthenticationPrincipal SignedJWT user,
            @RequestBody CartUpdateRequest request
            )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Long movieId = request.getMovieId();
        Integer quantity = request.getQuantity();

        // Validate request parameters
        validate.validateRequestMovieQuantity(quantity);
        repo.checkConflictItemNotInCart(userId, movieId);

        repo.updateMovieInCart(userId, movieId, quantity);

        return new GeneralResponse()
                .setResult(BillingResults.CART_ITEM_UPDATED)
                .toResponse();
    }

    /**
     * Cart Delete
     */
    @DeleteMapping("/cart/delete/{movieId}")
    public ResponseEntity<GeneralResponse> cartDelete(
            @AuthenticationPrincipal SignedJWT user,
            @PathVariable Long movieId
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        repo.checkConflictItemNotInCart(userId, movieId);

        repo.deleteMovieInCart(userId, movieId);

        return new GeneralResponse()
                .setResult(BillingResults.CART_ITEM_DELETED)
                .toResponse();
    }

    /**
     * Cart Retrieve
     */
    @GetMapping("/cart/retrieve")
    public ResponseEntity<CartRetrieveResponse> cartRetrieve(
            @AuthenticationPrincipal SignedJWT user
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Item[] items = repo.retrieveAllItemsFromCart(userId, userRoles);

        if (items.length == 0) {
            // TODO: Figure out why retrieve 0 movies when there are more
            LOG.info("items.length: " + items.length);
            return new CartRetrieveResponse()
                    .setResult(BillingResults.CART_EMPTY)
                    .toResponse()
                    ;
        }

        BigDecimal total = Stream.of(items)
                .map(item -> {return item
                        .getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        ;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = total.setScale(2, RoundingMode.DOWN);

        return new CartRetrieveResponse()
                .setItems(items)
                .setTotal(total)
                .setResult(BillingResults.CART_RETRIEVED)
                .toResponse()
                ;
    }

    /**
     * Cart Clear
     */
    @PostMapping("/cart/clear")
    ResponseEntity<GeneralResponse> cartClear(
            @AuthenticationPrincipal SignedJWT user
    )
    {
        JWTClaimsSet userJWTClaimsSet = getUserJWTClaimsSet(user);
        Long userId = getUserID(userJWTClaimsSet);
        List<String> userRoles = getUserRoles(userJWTClaimsSet);

        Item[] items = repo.retrieveAllItemsFromCart(userId, userRoles);
        if (items.length == 0) {
            return new GeneralResponse()
                .setResult(BillingResults.CART_EMPTY)
                .toResponse()
                ;
        }

        repo.clearAllItemsFromCart(userId);

        return new GeneralResponse()
                .setResult(BillingResults.CART_CLEARED)
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
}
