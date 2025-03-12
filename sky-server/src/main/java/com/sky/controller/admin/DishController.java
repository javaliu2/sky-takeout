package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api("菜品管理相关接口")
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品 逻辑控制
     * @param dishDTO
     * @return
     */
    @ApiOperation("新增菜品")
    @PostMapping()
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品信息：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 分页查询 逻辑控制
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> pageShow(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询参数：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @return
     */
    @DeleteMapping()
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids) {
        // 前端请求参数 ...?ids=1,2,3
        // 通过@RequestParam, SpringBoot自动解析该值然后封装到List<Long>类型的参数中
        // dish表中菜品ID字段类型是bigint, 刚好对应Java中的long类型数值范围（有符号的情况下）
        // 如果bigint设置为无符号，那么需要在Java中使用BigInteger来映射
        log.info("批量删除菜品，菜品id：{}", ids);
        dishService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 根据id决定起售或停售该菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("起售或停售菜品")
    public Result saleOrHalt(@PathVariable Integer status, Long id) {
        log.info("将id为{}的菜品状态置为{}", id, status);
        dishService.saleOrHalt(id, status);
        return Result.success();
    }
}
