package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetialMapper orderDetialMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //获取用户id
        Long userId = BaseContext.getCurrentId();

        //1.处理业务异常
        //地址簿是否为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //购物车是否为空
            //获取购物车对象
        ShoppingCart shoppingCart= ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList.size()==0||shoppingCartList==null){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //根据userId获取user对象
        User user=userMapper.getById(userId);


        //2.向订单表插入一条数据
        //获取对象，保存到order对象里
        Orders orders=new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setUserName(user.getName());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        //3.向订单明细表加入N条数据,由购物车决定
        List<OrderDetail> orderDetailList=new ArrayList<>();
        for (ShoppingCart shoppingCart1 : shoppingCartList) {
            OrderDetail orderDetail=new OrderDetail();//订单明细
            orderDetail.setOrderId(orders.getId());
            BeanUtils.copyProperties(shoppingCart1,orderDetail);
            orderDetailList.add(orderDetail);
        }
        orderDetialMapper.insertBatch(orderDetailList);
        //4.清空当前用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);
        //5.封装VO并返回
        OrderSubmitVO orderSubmitVO=OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
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

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        //1.根据分页参数构建分页对象
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //2.根据订单状态分页查询所有对应的订单列表
        Page<Orders> page1 =orderMapper.pageQuery(ordersPageQueryDTO);
        //3.循环遍历订单列表查询对应的订单详情
            //创建VO集合
        List<OrderVO> orderVOList=new ArrayList<>();
        if (page1 != null && page1.getTotal() > 0) {//重要
            for (Orders orders : page1) {
                List<OrderDetail> orderDetails = orderDetialMapper.getByOrderId(orders.getId());
                //组合添加到VO对象中
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                //添加到VO集合中
                orderVOList.add(orderVO);
            }
        }
        //4.创建pageResult对象并返回
        PageResult pageResult=new PageResult(page1.getTotal(),orderVOList);
        return pageResult;
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        //构建OrderVO对象
        OrderVO orderVO=new OrderVO();
        //根据订单id查询订单
        Orders orders=orderMapper.getById(id);
        //根据订单id查询订单详情
        List<OrderDetail> orderDetailList = orderDetialMapper.getByOrderId(id);
        //封装到对象里
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) {
        //查询订单表查看当前状态   1待付款 2待接单 3已接单 4派送中
        Orders orders = orderMapper.getById(id);
        Integer status = orders.getStatus();
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
            //待支付和待接单状态下，用户可直接取消订单  1待付款 2待接单
            //商家已接单状态下和派送中状态下，用户取消订单需电话沟通商家    3已接单    4派送中
        if (status>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        //如果在待接单状态下取消订单，需要给用户退款     2待接单
        if (status.equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            /*      weChatPayUtil.refund(
                    orders.getNumber(), //商户订单号
                    orders.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            */
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }
        //取消订单后需要将订单状态修改为“已取消”,取消原因、取消时间
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        //根据商品号到商品明细表搜索菜品id和套餐id
        List<OrderDetail> orderDetailList = orderDetialMapper.getByOrderId(id);
        //将其添加到购物车表中  拷贝属性  自己添加userId   createTime
        List<ShoppingCart> shoppingCartList=new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart=new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id");
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartList.add(shoppingCart);
        }
        //批量添加
        shoppingCartMapper.insertBatch(shoppingCartList);

    }
}
