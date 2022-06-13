package com.github.klefstad_teaching.cs122b.billing.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Item;
import com.github.klefstad_teaching.cs122b.core.base.ResponseModel;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailResponse extends ResponseModel<OrderDetailResponse> {
    private BigDecimal total;
    private Item[] items;

    public BigDecimal getTotal() {
        return total;
    }

    public OrderDetailResponse setTotal(BigDecimal total) {
        this.total = total;
        return this;
    }

    public Item[] getItems() {
        return items;
    }

    public OrderDetailResponse setItems(Item[] items) {
        this.items = items;
        return this;
    }
}
