package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        // 拷贝属性
        BeanUtils.copyProperties(dishDTO, dish);
        // 保存菜品数据(一个)
        // 设置返回主键并且将返回值赋值给dish对象中id属性
        dishMapper.insert(dish);
        // 保存口味数据(若干)
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            // 获取该口味对应的菜品ID
            Long id = dish.getId();
            flavors.forEach(flavor -> {
                flavor.setDishId(id);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 分页查询接口【实现】
     * @param dishPageQueryDTO
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品业务【实现】
     * 操作了两张表，因此加 @Transactional注解以保证数据一致性
     * @param ids
     */
    @Transactional
    public void batchDelete(List<Long> ids) {
        // 1、是否停售
        // 逐一处理
        for (long id : ids) {
            // my implementation
//            Integer status = dishMapper.queryStatus(id);  // 2）根据id查询Dish吧，这样具备可重用性
//            log.debug("id={}, status={}", id, status);
//            if (Objects.equals(status, StatusConstant.ENABLE)) {
//                return MessageConstant.DISH_ON_SALE;  // 1）通过抛异常的方式提醒前端, 而不是向controller返回数据
//            }
            Dish dish = dishMapper.getDishById(id);
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE); // 提醒前端，原理是什么?
                // 通过GlobalExceptionHandler进行处理，将异常信息返回给前端
            }
        }
        // 2、是否被某一个套餐包含
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 3、删除菜品以其关联的口味数据
        // 以下代码可能会产生多次数据库操作，降低效率
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteByDishId(id);
//        }
        // 优化：使用in语句进行批量删除
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);
    }

    public void saleOrHalt(Long id, Integer status) {
        // 仿照EmployeeServiceImpl代码
        Dish dish = Dish.builder().status(status).id(id).build();
        dishMapper.update(dish);
    }
}
