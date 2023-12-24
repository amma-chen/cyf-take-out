package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
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

    @Autowired
    UserMapper userMapper;

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

    /**
     * 统计指定时间的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //自己计算出日期列表
        List<LocalDate> dateList = new ArrayList<>();
        //dateList	string	必须		日期列表，以逗号分隔
        dateList.add(begin);
        while (!begin.equals(end)&& begin.isBefore(end))
        {
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime=LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);
            //newUserList	string	必须		新增用户数列表，以逗号分隔
            Integer newUser = userMapper.countNewUserByCreateTime(beginTime,endTime);
            newUserList.add(newUser);
            //totalUserList	string	必须		总用户量列表，以逗号分隔
            Integer totalUser = userMapper.countTotalUserByCreateTime(endTime);
            totalUserList.add(totalUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }
}
