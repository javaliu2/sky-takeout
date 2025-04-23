package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
}
