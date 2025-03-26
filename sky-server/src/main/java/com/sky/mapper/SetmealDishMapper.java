package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 查询和菜品id有关的套餐id
     * @param dishIds
     * @return
     */
    // select setmeal_id from setmeal_dish where dish_id in (1,2,3)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 查询主键为id的套餐关联的菜品数据【数据库查询接口】
     * @param setmealId 套餐id
     * @return
     */
    List<SetmealDish> getDishOfSetmealById(Long setmealId);

    /**
     * 删除关系表中套餐id为setmealId的数据
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 批量删除套餐主键为ids的菜品
     * @param ids
     */
    void deleteBySetmealIds(List<Long> setmealIds);
}
