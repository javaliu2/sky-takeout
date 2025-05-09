package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 向订单表中插入数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    @Select("select * from orders where status=#{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrdertimeLT(Integer status, LocalDateTime time);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    Page<OrderVO> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select count(*) from orders where status=#{status}")
    Integer getStatusCount(Integer status);


    Double getDailyTurnover(Map<String, Object> param);

    Integer getDailyOrderCount(Map<String, Object> param);

    List<Long> getVaildOrderCountWithCheckoutTime(LocalDateTime beginTime, LocalDateTime endTime);

    Integer countByMap(Map map);

    Double sumByMap(Map map);
}
