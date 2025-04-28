package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.util.HashMap;
import java.util.Map;

@RestController("user_order")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WebSocketServer webSocketServer;
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("提交的订单信息：{}", ordersSubmitDTO);
        OrderSubmitVO submitVO = orderService.orderSubmit(ordersSubmitDTO);
        return Result.success(submitVO);  // 通过http响应与client通信
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
//    @PutMapping("/payment")
//    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        log.info("订单支付：{}", ordersPaymentDTO);
//        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
//        log.info("生成预支付交易单：{}", orderPaymentVO);
//        return Result.success(orderPaymentVO);
//    }

    /**
     * 不通过微信支付服务，在本地服务器完成支付模拟（前端小程序代码也需要修改，前端不再去请求微信支付，而是调用直接支付成功的回调函数）。
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    public Result<String> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO){
        log.info("订单支付，本地模拟：{}", ordersPaymentDTO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success("模拟支付");
    }

    /**
     * 客户催单【逻辑控制】
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable("id") Long id) {
        orderService.reminder(id);
        return  Result.success();
    }

    /**
     * 根据订单id获取订单信息【逻辑控制】
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable("id") Long id) {
        log.info("获取id为{}的订单详情", id);
        OrderVO data = orderService.getOrderDetail(id);
        return Result.success(data);
    }

    /**
     * 查询历史订单【逻辑控制】
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    public Result<PageResult> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("查询所有订单：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id取消订单【逻辑控制】
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    public Result<String> cancelOrder(@PathVariable("id") Long id) {
        log.info("取消id为{}的订单", id);
        orderService.cancelOrder(id);
        return Result.success("取消成功");
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    public Result oneMore(@PathVariable("id") Long id) {
        log.info("再来一单id为{}的商品", id);
        orderService.oneMore(id);
        return Result.success();
    }
}
