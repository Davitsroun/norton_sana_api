package com.leang.authservice.util;

import com.leang.authservice.model.entity.Product;

import java.math.BigDecimal;

public final class ProductCostHelper {

    private ProductCostHelper() {
    }

    /** Unit cost (COGS) at time of sale; zero when not set on product. */
    public static BigDecimal unitCost(Product product) {
        if (product == null || product.getCostPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getCostPrice();
    }
}
