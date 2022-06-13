package com.github.klefstad_teaching.cs122b.billing.model.request;

public class CartUpdateRequest {
    private Long movieId;
    private Integer quantity;

    public Long getMovieId() {
        return movieId;
    }

    public CartUpdateRequest setMovieId(Long movieId) {
        this.movieId = movieId;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public CartUpdateRequest setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
