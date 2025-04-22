package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    ShoppingCartService shoppingCartService;

    /**
     * 添加购物车【逻辑控制】
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("将商品:{}添加至购物车", shoppingCartDTO);
        shoppingCartService.add(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查看购物车【逻辑控制】
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> query() {
        log.info("查询购物车");
        List<ShoppingCart> data = shoppingCartService.query();
        return Result.success(data);
    }

    /**
     * 清空购物车【逻辑控制】
     * @return
     */
    @DeleteMapping("/clean")
    public Result clean() {
        log.info("清空购物车");
        shoppingCartService.clean();
        return Result.success();
    }

    /**
     * 商品数量减1【逻辑控制】
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    public Result subtractOne(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("将商品:{}数量减1", shoppingCartDTO);
        shoppingCartService.subtractOne(shoppingCartDTO);
        return Result.success();
    }
}
