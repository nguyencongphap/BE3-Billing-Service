package com.github.klefstad_teaching.cs122b.billing.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Item;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.ItemUtil;
import com.github.klefstad_teaching.cs122b.billing.repo.entity.Sale;
import com.github.klefstad_teaching.cs122b.core.error.ResultError;
import com.github.klefstad_teaching.cs122b.core.result.BillingResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Component
public class BillingRepo
{
    private static final Logger LOG = LoggerFactory.getLogger(BillingRepo.class);

    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public BillingRepo(ObjectMapper objectMapper, NamedParameterJdbcTemplate template)
    {
        this.objectMapper = objectMapper;
        this.template = template;
    }


    /**
     * Check if item is already in cart
     * @param userId
     * @param movieId
     */
    public void checkConflictItemAlreadyInCart(Long userId, Long movieId) {
        try {
            String sql =
                    "SELECT DISTINCT cart.movie_id\n" +
                            "FROM billing.cart AS cart\n" +
                            "WHERE cart.user_id = :userId AND \n" +
                            "cart.movie_id = :movieId"
                    ;

            MapSqlParameterSource source =
                    new MapSqlParameterSource()
                            .addValue("userId", userId, Types.INTEGER)
                            .addValue("movieId", movieId, Types.INTEGER)
                    ;

            Integer movieIdInCart =
                    this.template.queryForObject(
                            sql,
                            source,
                            Integer.class
                    );

            throw new ResultError(BillingResults.CART_ITEM_EXISTS);
        }
        catch (EmptyResultDataAccessException e) {
            ; // ALL GOOD! Item is not already in cart
        }
    }

    /**
     * Insert the movieId into a user's cart with the given quantity
     * @param userId
     * @param movieId
     * @param quantity
     */
    public void insertMovieToCart(Long userId, Long movieId, Integer quantity) {
        // Insert the movieId into a user's cart with the given quantity
        String sql =
                "INSERT INTO billing.cart (user_id, movie_id, quantity)\n" +
                        "VALUES (:userId, :movieId, :quantity);"
        ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("movieId", movieId, Types.INTEGER)
                        .addValue("quantity", quantity, Types.INTEGER)
        ;

        this.template.update(sql, source);
    }

    /**
     * Throw error if item is not in cart
     * @param userId
     * @param movieId
     */
    public void checkConflictItemNotInCart(Long userId, Long movieId) {
        try {
            String sql =
                    "SELECT DISTINCT cart.movie_id\n" +
                    "FROM billing.cart AS cart\n" +
                    "WHERE cart.user_id = :userId AND \n" +
                    "cart.movie_id = :movieId"
                    ;

            MapSqlParameterSource source =
                    new MapSqlParameterSource()
                            .addValue("userId", userId, Types.INTEGER)
                            .addValue("movieId", movieId, Types.INTEGER)
                    ;

            Integer movieIdInCart =
                    this.template.queryForObject(
                            sql,
                            source,
                            Integer.class
                    );
        }
        catch (EmptyResultDataAccessException e) {
            throw new ResultError(BillingResults.CART_ITEM_DOES_NOT_EXIST);
        }
    }

    public void updateMovieInCart(Long userId, Long movieId, Integer quantity) {
        // Update the movie with movieId in the given user's cart with the given quantity
        String sql =
                "UPDATE billing.cart \n" +
                "SET quantity = :quantity \n" +
                "WHERE cart.user_id = :userId AND \n" +
                "cart.movie_id = :movieId"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("movieId", movieId, Types.INTEGER)
                        .addValue("quantity", quantity, Types.INTEGER)
                ;

        this.template.update(sql, source);
    }

    public void deleteMovieInCart(Long userId, Long movieId) {
        String sql =
                "DELETE FROM billing.cart \n" +
                "WHERE cart.user_id = :userId AND \n" +
                "cart.movie_id = :movieId"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("movieId", movieId, Types.INTEGER)
                ;

        this.template.update(sql, source);
    }

    /**
     * Retrieve's all items from the user's cart with some movie details.
     * If the user has the Premium Role then we should report back with the "discounted rate".
     * (Refer to Formula for applying the discount on how to do this for Premium users)
     * @param userId
     * @param userRoles
     * @return
     */
    public Item[] retrieveAllItemsFromCart(Long userId, List<String> userRoles) {
        boolean isPremiumUser = false;

        for (String role : userRoles) {
            if (role.equalsIgnoreCase("Premium")) {
                isPremiumUser = true;
                break;
            }
        }

        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT( \n" +
                        "'unitPrice', joinResult.unit_price, \n" +
                        "'quantity', joinResult.quantity, \n" +
                        "'movieId', joinResult.movie_id, \n" +
                        "'movieTitle', joinResult.title, \n" +
                        "'backdropPath', joinResult.backdrop_path, \n" +
                        "'posterPath', joinResult.poster_path, \n" +
                        "'premiumDiscount', joinResult.premium_discount\n" +
                        ")) AS jsonArrayString \n" +
                        "FROM ( \n" +
                        "SELECT DISTINCT \n" +
                        "movie_price.unit_price,\n" +
                        "cart.quantity,\n" +
                        "cart.movie_id,\n" +
                        "movie.title,\n" +
                        "movie.backdrop_path,\n" +
                        "movie.poster_path,\n" +
                        "movie_price.premium_discount\n" +
                        "FROM \n" +
                        "billing.cart AS cart\n" +
                        "JOIN billing.movie_price AS movie_price ON cart.movie_id = movie_price.movie_id\n" +
                        "JOIN movies.movie AS movie ON cart.movie_id = movie.id\n" +
                        "WHERE cart.user_id = :userId) AS joinResult"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                ;

        LOG.info("userId: " + userId);
        LOG.info("\n" + sql);

        String resultJSONArrayString = null;
        try {

            resultJSONArrayString = this.template.queryForObject(
                    sql,
                    source,
                    String.class
            );
        }
        catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }

        ItemUtil[] itemUtils = new ItemUtil[0];
        if (resultJSONArrayString != null) {
            try {
                itemUtils =
                        objectMapper.readValue(resultJSONArrayString, ItemUtil[].class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Item> items = new ArrayList<Item>();
        if (isPremiumUser) {
            for (ItemUtil itemUtil : itemUtils) {
                items.add(new Item(
                        calculateDiscountedUnitPrice(itemUtil.getUnitPrice(),itemUtil.getPremiumDiscount()),
                        itemUtil.getQuantity(),
                        itemUtil.getMovieId(),
                        itemUtil.getMovieTitle(),
                        itemUtil.getBackdropPath(),
                        itemUtil.getPosterPath()
                ));
            }
        }
        else {
            for (ItemUtil itemUtil : itemUtils) {
                items.add(new Item(
                        itemUtil.getUnitPrice(),
                        itemUtil.getQuantity(),
                        itemUtil.getMovieId(),
                        itemUtil.getMovieTitle(),
                        itemUtil.getBackdropPath(),
                        itemUtil.getPosterPath()
                ));
            }
        }

        Item[] itemsArray = new Item[items.size()];
        items.toArray(itemsArray);

        return itemsArray;
    }

    // Helper function
    private BigDecimal calculateDiscountedUnitPrice(BigDecimal unitPrice, Integer discount) {
        BigDecimal discountedUnitPrice = unitPrice.multiply(BigDecimal.valueOf((1 - (discount / 100.0))));
        return  discountedUnitPrice.setScale(2, RoundingMode.DOWN);
    }

    public void clearAllItemsFromCart(Long userId) {
        String sql =
                "DELETE FROM billing.cart AS cart \n" +
                "WHERE cart.user_id = :userId;"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                ;

        this.template.update(sql, source);
    }

    public void insertSaleRecord(Long userId, BigDecimal total, Timestamp orderDate) {
        String sql =
                "INSERT INTO billing.sale(user_id, total, order_date) \n" +
                "VALUES (:userId, :total, :orderDate)"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("total", total, Types.DECIMAL)
                        .addValue("orderDate", orderDate, Types.TIMESTAMP)
                ;

        this.template.update(sql, source);
    }

    public Integer getLatestSaleIdOfUser(Long userId) {
        String sql =
                "SELECT s2.id\n" +
                "FROM (\n" +
                "SELECT MAX(sale.order_date) AS order_date\n" +
                "FROM billing.sale AS sale\n" +
                "WHERE sale.user_id = :userId \n" +
                ") s1\n" +
                "JOIN (\n" +
                "SELECT sale.id, sale.order_date\n" +
                "FROM billing.sale AS sale\n" +
                "WHERE sale.user_id = :userId \n" +
                ") s2\n" +
                "ON s1.order_date = s2.order_date\n"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER);

        Integer saleId = null;
        try {
            saleId = this.template.queryForObject(
                    sql,
                    source,
                    Integer.class
            );
        }
        catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }

        return saleId;
    }

    public void insertSaleItemRecord(Integer saleId, Long movieId, Integer quantity) {
        String sql =
                "INSERT INTO billing.sale_item(sale_id, movie_id, quantity)\n" +
                "VALUES (:saleId, :movieId, :quantity)"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("saleId", saleId, Types.INTEGER)
                        .addValue("movieId", movieId, Types.INTEGER)
                        .addValue("quantity", quantity, Types.INTEGER)
                ;

        this.template.update(sql, source);
    }

    public Sale[] getSalesFoundForUser(Long userId) {
        String sql =
                "SELECT DISTINCT \n" +
                "sale.user_id,\n" +
                "sale.id,\n" +
                "sale.total,\n" +
                "sale.order_date\n" +
                "FROM \n" +
                "billing.sale AS sale\n" +
                "WHERE sale.user_id = :userId \n" +
                "ORDER BY sale.order_date DESC\n" +
                "LIMIT 5"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                ;

        List<Sale> sales = new ArrayList<Sale>();
        try {
            sales = this.template.query(
                    sql,
                    source,
                    (rs, rowNum) ->
                            new Sale()
                                    .setSaleId(rs.getLong("id"))
                                    .setTotal(rs.getBigDecimal("total"))
                                    .setOrderDate(rs.getTimestamp("order_date").toInstant())
            );
        }
        catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }

        Sale[] salesArray = new Sale[sales.size()];
        sales.toArray(salesArray);

        return salesArray;
    }

    public Item[] retrieveUserItemsBySaleId(Long userId, Long saleId, List<String> userRoles) {
        boolean isPremiumUser = false;

        for (String role : userRoles) {
            if (role.equalsIgnoreCase("Premium")) {
                isPremiumUser = true;
                break;
            }
        }

        String sql =
                "SELECT JSON_ARRAYAGG(JSON_OBJECT( \n" +
                "'unitPrice', joinResult.unit_price, \n" +
                "'quantity', joinResult.quantity, \n" +
                "'movieId', joinResult.movie_id, \n" +
                "'movieTitle', joinResult.title, \n" +
                "'backdropPath', joinResult.backdrop_path, \n" +
                "'posterPath', joinResult.poster_path,\n" +
                "'premiumDiscount', joinResult.premium_discount \n" +
                ")) AS jsonArrayString \n" +
                "FROM ( \n" +
                "SELECT DISTINCT \n" +
                "movie_price.unit_price,\n" +
                "sale_item.quantity,\n" +
                "sale_item.movie_id,\n" +
                "movie.title,\n" +
                "movie.backdrop_path,\n" +
                "movie.poster_path,\n" +
                "movie_price.premium_discount\n" +
                "FROM billing.sale AS sale\n" +
                "JOIN billing.sale_item AS sale_item ON sale_item.sale_id = sale.id\n" +
                "JOIN movies.movie AS movie ON sale_item.movie_id = movie.id\n" +
                "JOIN billing.movie_price AS movie_price ON sale_item.movie_id = movie_price.movie_id\n" +
                "WHERE \n" +
                "sale.user_id = :userId AND\n" +
                "sale_item.sale_id = :saleId \n" +
                ") AS joinResult"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("saleId", saleId, Types.INTEGER);

        String resultJSONArrayString = null;
        try {


            resultJSONArrayString = this.template.queryForObject(
                    sql,
                    source,
                    String.class
            );
        }
        catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }

        ItemUtil[] itemUtils = new ItemUtil[0];
        if (resultJSONArrayString != null) {
            try {
                itemUtils =
                        objectMapper.readValue(resultJSONArrayString, ItemUtil[].class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Item> items = new ArrayList<Item>();
        if (isPremiumUser) {
            for (ItemUtil itemUtil : itemUtils) {
                items.add(new Item(
                        calculateDiscountedUnitPrice(itemUtil.getUnitPrice(),itemUtil.getPremiumDiscount()),
                        itemUtil.getQuantity(),
                        itemUtil.getMovieId(),
                        itemUtil.getMovieTitle(),
                        itemUtil.getBackdropPath(),
                        itemUtil.getPosterPath()
                ));
            }
        }
        else {
            for (ItemUtil itemUtil : itemUtils) {
                items.add(new Item(
                        itemUtil.getUnitPrice(),
                        itemUtil.getQuantity(),
                        itemUtil.getMovieId(),
                        itemUtil.getMovieTitle(),
                        itemUtil.getBackdropPath(),
                        itemUtil.getPosterPath()
                ));
            }
        }

        Item[] itemsArray = new Item[items.size()];
        items.toArray(itemsArray);

        return itemsArray;
    }


    /**
     * Error: The amount got from sale for premium users are not discounted correctly
     */
    public BigDecimal retrieveUserItemsTotalBySaleId(Long userId, Long saleId) {
        String sql =
                "SELECT sale.total\n" +
                "FROM billing.sale AS sale\n" +
                "WHERE \n" +
                "sale.user_id = :userId AND\n" +
                "sale.id = :saleId"
                ;

        MapSqlParameterSource source =
                new MapSqlParameterSource()
                        .addValue("userId", userId, Types.INTEGER)
                        .addValue("saleId", saleId, Types.INTEGER)
                ;

        BigDecimal total = null;
        try {
            total = this.template.queryForObject(
                    sql,
                    source,
                    BigDecimal.class
            );
        }
        catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
        }

        return total.setScale(2, RoundingMode.DOWN);
    }
}
