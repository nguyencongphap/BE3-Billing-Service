package com.github.klefstad_teaching.cs122b.billing.repo.entity;

import java.math.BigDecimal;

public class Item {
    private BigDecimal unitPrice;
    private Integer quantity;
    private Long movieId;
    private String movieTitle;
    private String backdropPath;
    private String posterPath;

    public Item() {
        // Allow creating empty Item before setting attributes using setters
        // Deleting this would only allow using the constructor below to instantiate Item
    }

    public Item(BigDecimal unitPrice, Integer quantity, Long movieId, String movieTitle, String backdropPath, String posterPath) {
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.backdropPath = backdropPath;
        this.posterPath = posterPath;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Item setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Item setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Long getMovieId() {
        return movieId;
    }

    public Item setMovieId(Long movieId) {
        this.movieId = movieId;
        return this;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public Item setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
        return this;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public Item setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
        return this;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Item setPosterPath(String posterPath) {
        this.posterPath = posterPath;
        return this;
    }
}
