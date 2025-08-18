package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private final RedisDistributedLock lock;
    @Autowired
    private final OrderMapper orderMapper;

    public OrderTask(RedisDistributedLock lock, OrderMapper orderMapper) {
        this.lock = lock;
        this.orderMapper = orderMapper;
    }

    @Scheduled(cron = "0 * * * * ?")  // 每分钟执行一次
    public void processTimeoutOrder() {
        String lockKey = "lock:cancel_order";  // 业务id
        Duration expire = Duration.ofSeconds(5);  // 锁持有时间
        if (!lock.tryLock(lockKey, expire)) {
            return;  // 没有抢到锁，说明别的实例在跑
        }
        try {
            log.info("处理超时的订单，当前时间: {}", LocalDate.now());
            Thread.sleep(10000);  // 模拟业务执行时间
            LocalDateTime time = LocalDateTime.now().minusMinutes(15);
            List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT, time);
            // 查询出来ordersList，但是这时候发生线程调度，用户完成支付，订单状态为已支付，在我们的业务系统中是（数字2，TO_BE_CONFIRMED，待接单）
            if (ordersList != null && ordersList.size() > 0) {
                ordersList.forEach(order -> {
                    order.setStatus(Orders.CANCELLED);
                    order.setCancelReason("支付超时，自动取消");
                    order.setCancelTime(LocalDateTime.now());
                    orderMapper.updateWithVersion(order, Orders.PENDING_PAYMENT);  // 更新的时候，要带上version判定和订单状态判定（只取消处于PENDING_PAYMENT状态的订单）
                });
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock(lockKey);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")  // 每日凌晨1点触发
    public void processDeliveryOrder() {
        log.info("处理派送中订单，当前时间: {}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }
}
