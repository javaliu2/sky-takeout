package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@Api("店铺相关接口")
@RequestMapping("/admin/shop")
public class ShopController {
    private static final String KEY = "SHOP_STATUS";
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺状态为：{}", status == 1 ? "营业" : "打烊");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> setStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        // Context: 前端登录店铺，他会自动获取店铺状态，但是现在Redis中没有保存状态数据
        // 获取到的status是null对象，报错：
        // 服务器：java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "status" is null
        // 前端得到的HTTP状态码：500 Internal Server Error
        if (status == null) {
            status = 0;
        }
        log.info("获取店铺当前状态：{}", status == 1 ? "营业" : "打烊");
        return Result.success(status);
    }
}
