package com.sky.controller.admin;

import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("admin_order")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PutMapping("/confirm")
    public Result<String> confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("确认接单，前端参数: {}", ordersConfirmDTO);
        orderService.confirmOrder(ordersConfirmDTO);
        return Result.success("已确认接单");
    }

    @PutMapping("/rejection")
    public Result<String> rejectOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        log.info("拒接接单，前端参数: {}", ordersRejectionDTO);
        orderService.rejectOrder(ordersRejectionDTO);
        return Result.success("已拒绝接单");
    }
    /**
     * 多条件查询符合条件的订单【逻辑控制】
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("查询符合条件的订单，查询条件：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }
}
