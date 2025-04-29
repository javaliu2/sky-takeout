package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    AddressBookMapper addressBookMapper;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    public OrderSubmitVO orderSubmit(OrdersSubmitDTO ordersSubmitDTO) {
        // 1、业务异常判断（前端已经对提交来的数据进行了判断，这里进行第二次判断的原因是提高系统鲁棒性，当使用postman方式提交数据时系统也能够正常响应，不至于宕机）
        // 1.1、地址簿是否为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 1.2、购物车是否为空
        Long userId = BaseContext.getCurrentId();  // 获取当前用户id
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 2、保存一份订单
        Orders order = new Orders();
        // 2.1、将DTO属性拷贝至order
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        // 2.2、补充order对象其他属性
        order.setNumber(String.valueOf(System.currentTimeMillis()));  // 将时间戳作为订单号
        order.setStatus(Orders.PENDING_PAYMENT);  // 设置订单状态为待付款
        order.setPayStatus(Orders.UN_PAID);  // 设置支付状态为未支付
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());  // 设置下单时间为当前时间
        //  我觉得下面这个地址比较合理
        String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();
        order.setAddress(address);
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());  // 收货人
        // 2.3、保存Order对象数据至数据库表orders
        orderMapper.insert(order);
        // 3、保存订单明细，商品信息查询数据库表shopping_cart
        // 采取批量插入的方式
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            // 拷贝属性的时候，cart的id属性是否会赋值给orderDetail对应的id属性上，如果是的话，数据库操作会覆写orderDetail的id属性嘛
            // 是，不会。但是涉及到OrderDetail id属性操作的时候，必须先查询数据库获取正确的id值
            // 数据插入数据库时，并没有使用orderDetail对象中的id作为数据库表主键值，无伤大雅。但是存在安全隐患
            orderDetail.setOrderId(order.getId());  // sql语句执行完成之后会将数据库生成的id赋予order对象
            orderDetail.setId(null); //  显式声明id为null，确保数据库使用自增主键
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        // 4、清空购物车
        shoppingCartMapper.clean(shoppingCart);
        // 5、构造VO对象然后返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.
                builder().
                id(order.getId()).
                orderNumber(order.getNumber()).
                orderAmount(order.getAmount()).
                orderTime(order.getOrderTime()).
                build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        // 向商家端推送消息提醒
        Map<String, Object> data = new HashMap<>();
        data.put("type", 1); // 1表示来单提醒；2表示客户催单
        data.put("orderId", ordersDB.getId());
        data.put("content", "订单号：" + outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(data));  // 通过websocket与client通信

        orderMapper.update(orders);
    }

    /**
     * 客户催单【功能实现】
     *
     * @param id
     */
    public void reminder(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("type", 2); // 客户催单
        data.put("orderId", id);
        data.put("content", "订单号：" + order.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(data));
    }

    /**
     * 获取订单详情【功能实现】
     *
     * @param id
     * @return
     */
    @Transactional
    public OrderVO getOrderDetail(Long id) {
        OrderVO orderVO = new OrderVO();
        // 查询orders表获取订单数据
        Orders order = orderMapper.getById(id);
        BeanUtils.copyProperties(order, orderVO);
        // 查询order_detail表获取该订单包含的所有菜品数据
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        StringBuilder sb = new StringBuilder();
        for (OrderDetail detail : orderDetailList) {
            sb.append(detail.getName()).append("*").append(detail.getNumber()).append(", ");
        }
        if (sb.length() >= 2) {
            sb.delete(sb.length() - 2, sb.length());
        }
        orderVO.setOrderDishes(sb.toString());
        return orderVO;
    }

    /**
     * 查询所有订单【功能实现】
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> page = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id取消订单【功能实现】
     *
     */
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        Orders order = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);
    }

    /**
     * 再来一单【功能实现】
     * @param id
     */
    public void oneMore(Long id) {
        // 查询订单详细信息，即所包含的所有菜品
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        // 修改userId的购物车为当前订单商品
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        for (OrderDetail orderDetail : orderDetailList) {
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 服务端确认接单【功能实现】
     * @param ordersConfirmDTO
     */
    public void confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = orderMapper.getById(ordersConfirmDTO.getId());
        order.setStatus(Orders.CONFIRMED);
        orderMapper.update(order);
    }

    @Override
    public void rejectOrder(OrdersRejectionDTO ordersRejectionDTO) {
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderMapper.update(order);
    }

    /**
     * 派送订单【功能实现】
     * @param id
     */
    public void deliveryOrder(Long id) {
        // 只修改订单状态为'派送中'，属性deliveryStatus和deliveryTime先不管
        Orders order = orderMapper.getById(id);
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    /**
     * 完成订单【功能实现】
     * @param id
     */
    public void completeOrder(Long id) {
        // 修改订单状态为'已完成'
        Orders order = orderMapper.getById(id);
        order.setStatus(Orders.COMPLETED);
        orderMapper.update(order);
    }

    /**
     * 统计订单数量【功能实现】
     * @return
     */
    public OrderStatisticsVO orderStatistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.getStatusCount(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.getStatusCount(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.getStatusCount(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }
}
