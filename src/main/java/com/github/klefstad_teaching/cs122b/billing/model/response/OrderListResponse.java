package com.github.klefstad_teaching.cs122b.billing.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Sale;
import com.github.klefstad_teaching.cs122b.core.base.ResponseModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderListResponse extends ResponseModel<OrderListResponse> {
    private Sale[] sales;

    public Sale[] getSales() {
        return sales;
    }

    public OrderListResponse setSales(Sale[] sales) {
        this.sales = sales;
        return this;
    }
}
