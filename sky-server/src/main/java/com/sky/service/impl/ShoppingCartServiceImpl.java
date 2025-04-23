package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    ShoppingCartMapper shoppingCartMapper;

    @Autowired
    DishMapper dishMapper;

    @Autowired
    SetmealMapper setmealMapper;
    /**
     * 将商品加入购物车【功能实现】
     * @param shoppingCartDTO
     */
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        // 检查商品是否已经存在于购物车
        // 存在的话，只将其数量加一
        // 否则，不存在的话，将其加入购物车，
        // 我觉得他这里设计的不好，添加购物车应该是post，数量加一应该是put或者patch
        // 他这里都用的采用post方式的一个接口，不太好，需要在里面判断了，繁琐。
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
//        shoppingCart.setId(userId);  // 这里写错了，应该是userId
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {  // 表示购物车中有这个商品
            ShoppingCart cart = list.get(0);
            // 将商品数量加一
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 将商品加入购物车，更新数据库表
            // 判断是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {  // 是菜品
                // 通过mapper查询数据库获取菜品名、价格、图片路径
                Dish dish = dishMapper.getDishById(dishId);
                // 设置ShoppingCart对象对应属性
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            } else {  // 是套餐
                Setmeal setmeal = setmealMapper.getSetmealById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            // 其他属性赋值
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            // 通过mapper将对象数据保存至数据库
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查询购物车【功能实现】
     */
    public List<ShoppingCart> query() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车【功能实现】
     */
    public void clean() {
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        shoppingCartMapper.clean(shoppingCart);
    }

    /**
     * 将商品数量减去1【功能实现】
     * @param shoppingCartDTO
     */
    public void subtractOne(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 根据用户id和商品id（菜品id或者套餐id）查询数据库得到该商品的所有信息
        // 还是可以用mapper.list，无非这里查询到的数据只有一条
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        // list一定不为空且size为1，因为这是将商品数量减1，该商品一定存在于购物车
        ShoppingCart cart = list.get(0);
        Integer number = cart.getNumber();
        if (number - 1 == 0) {
            // 从购物车中将该商品清除
            // 套餐通过用户id和套餐id唯一确定；菜品除了用户id和菜品id以外还需要口味，因为相同菜品不同口味的话是两个商品
            // 忽略了主键唯一确定一条商品，因此只根据主键就可以完成删除或者数量减1的操作
            shoppingCartMapper.removeOne(cart);
        } else {
            cart.setNumber(number - 1);
            shoppingCartMapper.updateNumberById(cart);
        }
    }
}
