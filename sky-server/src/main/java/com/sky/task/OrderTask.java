package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    OrderMapper orderMapper;

    /**
     * 每分钟检查超时任务订单
     */
    @Scheduled(cron = " * 0/1 * * * ? ")//cron表达式设计为每分钟执行一次
    public void processTimeoutOrders(){
        log.info("定时处理超时订单/min");
        //搜索所有订单支付时间超过15分钟且未支付（状态为1 PENDING_PAYMENT && orderTime+15分钟<now）
        List<Orders> ordersList=orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (ordersList!=null&&ordersList.size()!=0) {
            for (Orders orders : ordersList) {
                //status 设置为6 CANCELLED cancelReason= "用户未支付" cancelTime=now  amount=0
                Orders orders1 = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.CANCELLED)
                        .cancelReason("用户支付超时,自动取消")
                        .cancelTime(LocalDateTime.now())
                        .amount(BigDecimal.valueOf(0))
                        .build();
                orderMapper.update(orders1);
            }
        }

    }

    /**
     * 处理一直处于派送中的订单为已派送
     */
    @Scheduled(cron = " 0 0 1 * * ? ")//每天凌晨1点处理一次
    public void processDeliveryOrder(){
        log.info("处理一直处于派送中的订单为已派送/凌晨1点");
        //搜索所有订单支付时间超过15分钟且未支付（状态为4 DELIVERY_IN_PROGRESS && orderTime<now-1小时）
        List<Orders> ordersList=orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if (ordersList!=null&&ordersList.size()!=0) {
            for (Orders orders : ordersList) {
                //status 设置为6 CANCELLED cancelReason= "用户未支付" cancelTime=now  amount=0
                Orders orders1 = Orders.builder()
                        .id(orders.getId())
                        .status(Orders.COMPLETED)
                        .deliveryTime(LocalDateTime.now())
                        .build();
                orderMapper.update(orders1);
            }
        }
    }
}
