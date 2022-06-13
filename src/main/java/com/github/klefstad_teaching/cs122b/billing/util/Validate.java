package com.github.klefstad_teaching.cs122b.billing.util;

import com.github.klefstad_teaching.cs122b.core.error.ResultError;
import com.github.klefstad_teaching.cs122b.core.result.BillingResults;
import com.stripe.model.PaymentIntent;
import org.springframework.stereotype.Component;

@Component
public final class Validate
{

    /**
     * Check if quantity is zero or less
     * Check if quantity exceeded max limit
     * @param quantity
     */
    public void validateRequestMovieQuantity(Integer quantity) {
        if (quantity <= 0) {
            throw new ResultError(BillingResults.INVALID_QUANTITY);
        }
        if ( quantity >= 11) {
            throw new ResultError(BillingResults.MAX_QUANTITY);
        }
    }

    public void validatePaymentIntentPaymentStatus(PaymentIntent paymentIntent) {
        if (!paymentIntent.getStatus().equalsIgnoreCase("succeeded")) {
            throw new ResultError(BillingResults.ORDER_CANNOT_COMPLETE_NOT_SUCCEEDED);
        }
    }

    public void validatePaymentIntentUserId(PaymentIntent paymentIntent, Long userId) {
        if (!userId.toString().equalsIgnoreCase(paymentIntent.getMetadata().get("userId"))) {
            throw new ResultError(BillingResults.ORDER_CANNOT_COMPLETE_WRONG_USER);
        }
    }
}
