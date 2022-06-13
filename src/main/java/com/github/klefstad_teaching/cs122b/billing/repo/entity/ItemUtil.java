package com.github.klefstad_teaching.cs122b.billing.repo.entity;

import java.math.BigDecimal;

public class ItemUtil {
    private BigDecimal unitPrice;
    private Integer quantity;
    private Long movieId;
    private String movieTitle;
    private String backdropPath;
    private String posterPath;
    private Integer premiumDiscount;

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public ItemUtil setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ItemUtil setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Long getMovieId() {
        return movieId;
    }

    public ItemUtil setMovieId(Long movieId) {
        this.movieId = movieId;
        return this;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public ItemUtil setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
        return this;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public ItemUtil setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
        return this;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public ItemUtil setPosterPath(String posterPath) {
        this.posterPath = posterPath;
        return this;
    }

    public Integer getPremiumDiscount() {
        return premiumDiscount;
    }

    public ItemUtil setPremiumDiscount(Integer premiumDiscount) {
        this.premiumDiscount = premiumDiscount;
        return this;
    }
}
