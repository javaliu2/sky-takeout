package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 构造redis缓存数据的key，规则：dish_{categoryId}
        String key = "dish_" + categoryId;
        // 查找缓存数据是否存在
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);  // 存储的时候是什么类型，获取的时候就是什么类型
        // 至于他是怎么存储，读取的规则是什么，不用管
        // 1、存在，直接返回
        if (list != null && list.size() > 0) {
            return Result.success(list);
        }
        // 2、不存在，进行数据库查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        list = dishService.listWithFlavor(dish);
        // 2.1、将数据缓存
        // Value是redis中的string类型，这里存储的时候，自动将数据对象进行JDK序列化
        redisTemplate.opsForValue().set(key, list);
        return Result.success(list);
    }
}
