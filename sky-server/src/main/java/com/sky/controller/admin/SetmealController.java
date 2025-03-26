package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐，逻辑控制
     * @param setmealDTO
     * @return
     */
    @PostMapping
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐，SetmealDTO：{}", setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询，逻辑控制
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询，前端参数：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据套餐id返回该套餐的所有信息，包括其包含的菜品【逻辑控制】
     * @param id 套餐id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> getSetmealById(@PathVariable Long id) {
        log.info("查询id为{}的套餐数据", id);
        SetmealVO setmealVO = setmealService.getSetmealByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 根据前端DTO数据更新套餐数据【功能控制】
     * @param setmealDTO
     * @return
     */
    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("更新套餐数据，新数据：{}", setmealDTO);
        setmealService.updateSetmealWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 将主键为id的套餐状态修改为status【功能控制】
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result editStatus(@PathVariable Integer status, Long id) {
        log.info("将主键为{}的套餐状态修改为{}", id, status);
        setmealService.editStatus(id, status);
        return Result.success();
    }

    /**
     * 删除ids对应的所有套餐【功能控制】
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result batchDelete(@RequestParam List<Long> ids) {
        log.info("删除主键为{}的套餐数据", ids);
        setmealService.batchDelete(ids);
        return Result.success();
    }
}
