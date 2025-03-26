package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 新增套餐
     * @param setmealDTO
     */
    public void addSetmeal(SetmealDTO setmealDTO) {
        // 操作两张表，分别是setmeal表和setmeal_dish表
        // 将DTO对象属性值拷贝至entity对象对应属性上去
        // 调用setmeal mapper的新增方法
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();  // 获取数据库表主键
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);  // 对每一个套餐菜品对象设置其套餐id
        });
        // 调用setmeal_dish mapper的批量新增方法
        setmealDishMapper.insertBatch(setmealDishes);
    }

    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 返回SetmealVO对象，其中包含分类名称，这需要到category表中进行查找
        // 同时包含该套餐下的所有菜品，这涉及到setmeal_dish表的查找
        // 又看了一下接口文档，这里返回的数据不包括菜品，只有套餐相关信息
        // 但是SetmealVO对象中是有套餐菜品关系属性的setmealDishes
        // finally，按照接口来
        try (Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO)) {
            return new PageResult(page.getTotal(), page.getResult());
        }
    }

    /**
     * 根据套餐id返回该套餐信息，包括其包含的菜品【功能实现】
     * @param id
     * @return
     */
    public SetmealVO getSetmealByIdWithDish(Long id) {
        // 首先获取套餐信息
        Setmeal setmeal = setmealMapper.getSetmealById(id);
        // 获取套餐关联的菜品信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishOfSetmealById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 根据DTO对象更新套餐数据【功能实现】
     * @param setmealDTO
     */
    public void updateSetmealWithDish(SetmealDTO setmealDTO) {
        // 1、更新 套餐表 数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
        // 2、更新 套餐菜品关系表 数据
        // 2.1 实现策略：首先删除该套餐所有数据，然后插入新的数据
        Long setmealId = setmeal.getId();
        setmealDishMapper.deleteBySetmealId(setmealId);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 将主键为id的套餐状态修改为status【功能实现】
     * @param id
     * @param status
     */
    public void editStatus(Long id, Integer status) {
        // 起售的时候需要判断是否包括停售的菜品，如果有的话，抛异常提醒前端
        if (status == StatusConstant.ENABLE) {
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if (dishes != null && dishes.size() > 0) {
                dishes.forEach(dish -> {
                        if (StatusConstant.DISABLE == dish.getStatus()) {
                            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                        }
                    }
                );
            }
        }
        Setmeal setmeal = Setmeal.builder().status(status).id(id).build();
        setmealMapper.update(setmeal);
    }

    /**
     * 删除主键为ids的套餐数据【功能实现】
     * @param ids
     */
    public void batchDelete(List<Long> ids){
        // 1、判断其售卖状态是否为 停售
        // 非停售（即售卖状态）不可删除，通过抛异常的方式提醒前端
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getSetmealById(id);
            if (Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 2、批量删除套餐（操作套餐表）及其包含菜品（操作套餐菜品表）
        setmealMapper.deleteByIds(ids);
        setmealDishMapper.deleteBySetmealIds(ids);
    }
}
