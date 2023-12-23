package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

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

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计数量
     * @param status
     * @return
     */
    @Select("select count(*) from orders where status=#{status}")
    Integer countByStatus(Integer status);

    /**
     * 查询订单状态和小于下单时间的订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select(" select * from orders where status =#{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 查询订单状态和下单时间范围内的订单总金额
     * @param status
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select(" select sum(amount) from orders where status =#{status} and order_time between #{beginTime} and #{endTime}")
    BigDecimal sumByStatusAndOrderTime(Integer status, LocalDateTime beginTime, LocalDateTime endTime);
}
