package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void addSetmeal(SetmealDTO setmealDTO);

    /**
     * 套餐分页多参数查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据套餐id返回该套餐信息，包括其包含的菜品【功能接口】
     * @param id
     * @return
     */
    SetmealVO getSetmealByIdWithDish(Long id);

    /**
     * 根据DTO对象更新套餐数据【功能接口】
     * @param setmealDTO
     */
    void updateSetmealWithDish(SetmealDTO setmealDTO);

    /**
     * 将主键为id的套餐状态修改为status【功能接口】
     * @param id
     * @param status
     */
    void editStatus(Long id, Integer status);

    /**
     * 删除主键为ids的套餐数据【功能接口】
     * @param ids
     */
    void batchDelete(List<Long> ids);
}
