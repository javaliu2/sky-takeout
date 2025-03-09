package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;

public interface DishService {
    /**
     * 保存菜品及其口味数据
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);
}
