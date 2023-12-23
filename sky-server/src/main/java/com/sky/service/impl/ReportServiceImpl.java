package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrderMapper orderMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO GetturnoverStatistics(LocalDate begin, LocalDate end) {
        //dateList
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)&&begin.compareTo(end) < 0){
            //日期计算，指定日期后一天的日期
            begin=begin.plusDays(1);
            dateList.add(begin);
        }
        String dateList1 = StringUtils.join(dateList, ",");

        //turnoverList

        //按日期查询
        List<BigDecimal> turnoverList=new ArrayList<>();
        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            BigDecimal amount = orderMapper.sumByStatusAndOrderTime(Orders.COMPLETED,beginTime,endTime);
            //一天营业额是0时返回的amount为null，需手动设置

            amount= amount==null?BigDecimal.valueOf(0):amount;
            turnoverList.add(amount);
        }
        String turnoverList1 = StringUtils.join(turnoverList, ",");
        //循环遍历每个元素，查询sum金额 当支付状态为
        return TurnoverReportVO.builder()
                .turnoverList(turnoverList1)
                .dateList(dateList1)
                .build();
    }
}
