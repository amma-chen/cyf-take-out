package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    WorkspaceService workspaceService;
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

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //├─ dateList	string	必须		日期列表，以逗号分隔
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.isEqual(end)&& begin.isBefore(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList=new ArrayList<>();
        List<Integer> validOrderCountList=new ArrayList<>();
        Integer totalOrderCount=0;
        Integer validOrderCount=0;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //├─ orderCountList	string	必须		订单数列表，以逗号分隔
            Integer orderCount=orderMapper.countByStatusAndOrderTime(null,beginTime,endTime);
            orderCountList.add(orderCount);
            //├─ validOrderCountList	string	必须		有效订单数列表，以逗号分隔
            Integer validOrderCount1=orderMapper.countByStatusAndOrderTime(Orders.COMPLETED,beginTime,endTime);
            validOrderCountList.add(validOrderCount1);
            //├─ totalOrderCount	integer	必须		订单总数 format: int32
            //├─ validOrderCount	integer	必须		有效订单数 format: int32
            totalOrderCount+=orderCount;
            validOrderCount+=validOrderCount1;
        }
        //├─ orderCompletionRate	number	必须		订单完成率 format: double
        double orderCompletionRate=0.0;
        if (totalOrderCount!=0){
        orderCompletionRate=validOrderCount.doubleValue()/totalOrderCount;}




        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalOrderCount(totalOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCount(validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .build();
    }

    /**
     * 查询指定时间区间销量排名top10商品和销量
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        //├─ nameList	string	必须		商品名称列表，以逗号分隔
        //├─ numberList	string	必须		销量列表，以逗号分隔
        //找order表查看名称
        //找oder-detail表 查看销量
//select od.name,sum(od.number) from orders o left outer join order_detail od on o.id = od.order_id
//where o.status=5 and o.order_time between '2023-10-20 01:32:26' and '2023-12-25 01:32:26' group by od.name;
        List<GoodsSalesDTO> GoodsSalesList = orderMapper.getSumSalesByStatusAndOrderTime(Orders.COMPLETED, beginTime, endTime);
        for (GoodsSalesDTO GoodsSales : GoodsSalesList) {
            nameList.add(GoodsSales.getName());
            numberList.add(GoodsSales.getNumber());
        }


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查数据库
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询最近30天概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //读取表
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //写入表
            //概览数据
                //时间
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);
                //营业额
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
                //订单完成率
            XSSFRow row=sheet.getRow(3);
            row.getCell(4).setCellValue(businessDataVO.getTurnover());
            row.getCell(6).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(2).setCellValue(businessDataVO.getNewUsers());
                //获得第五
            row=sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //明细数据--每一天
            for (int i=0;i<30;i++){
                LocalDate date=dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //i+7行
                row=sheet.getRow(7+i);
                row.getCell( 1).setCellValue(date.toString());
                row.getCell( 2).setCellValue(businessData.getTurnover());
                row.getCell( 3).setCellValue(businessData.getValidOrderCount());
                row.getCell(  4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell( 6).setCellValue(businessData.getNewUsers());
            }

            //通过输出流向excel文件导出表到用户端
            ServletOutputStream out=response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
