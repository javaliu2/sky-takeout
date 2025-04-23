package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Update("update shopping_cart set number=#{number} where id=#{id}")
    void updateNumberById(ShoppingCart cart);

    void insert(ShoppingCart shoppingCart);

    /**
     * 从购物车中移除当前用户的所有商品
     * @param shoppingCart
     */
    @Delete("delete from shopping_cart where user_id=#{userId}")
    void clean(ShoppingCart shoppingCart);

    /**
     * 从购物车中移除一个商品
     * @param shoppingCart
     */
    @Delete("delete from shopping_cart where id=#{id}")
    void removeOne(ShoppingCart shoppingCart);
}
