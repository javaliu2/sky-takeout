package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
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

    /**
     * 取消订单【逻辑控制】
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    public Result<String> cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        log.info("商家取消订单，前端参数：{}", ordersCancelDTO);
        orderService.cancelOrder(ordersCancelDTO);
        return Result.success("订单取消成功");
    }

    /**
     * 获取订单详情【逻辑控制】
     *
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable("id") Long id) {
        log.info("获取订单详情，前端参数，订单id：{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/delivery/{id}")
    public Result<String> deliveryOrder(@PathVariable("id") Long id) {
        log.info("派送订单，前端参数：【订单id：{}】", id);
        orderService.deliveryOrder(id);
        return Result.success("开始派送");
    }

    @PutMapping("/complete/{id}")
    public Result<String> completeOrder(@PathVariable("id") Long id) {
        log.info("完成订单，前端参数：订单id：{}", id);
        orderService.completeOrder(id);
        return Result.success("完成订单");
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> orderStatistics() {
        log.info("统计不同状态的订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.orderStatistics();
        return Result.success(orderStatisticsVO);
    }
}
