package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 将商品加入购物车【功能接口】
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车【功能接口】
     * @return
     */
    List<ShoppingCart> query();

    /**
     * 清空购物车【功能接口】
     */
    void clean();

    /**
     * 将商品数量减去1【功能接口】
     * @param shoppingCartDTO
     */
    void subtractOne(ShoppingCartDTO shoppingCartDTO);
}
