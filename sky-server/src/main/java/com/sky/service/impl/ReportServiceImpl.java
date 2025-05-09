package com.sky.service.impl;

import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;  // 也可以注入其他的service，而不仅仅只局限于mapper

    /**
     * 营业额统计【功能实现】
     *
     * @param beginDate
     * @param endDate
     * @return
     */
    public TurnoverReportVO turnoverStatistics(LocalDate beginDate, LocalDate endDate) {
        // 计算beginDate到endDate的所有天
        List<LocalDate> dates = new ArrayList<>();
        dates.add(beginDate);
        while (!beginDate.equals(endDate)) {
            beginDate = beginDate.plusDays(1);
            dates.add(beginDate);
        }
        // 统计每一天的营业额，营业额的定义（需求分析阶段应该明确的）：订单状态处于“已完成”且date这一天的订单金额合计
        List<Double> turnovers = new ArrayList<>();
        for (LocalDate date : dates) {
            // 通过日期API计算date这一天的开始时刻和结束时刻
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 调用mapper查询数据库，查询参数通过Map形式传递
            Map<String, Object> param = new HashMap<>();
            param.put("status", Orders.COMPLETED);
            param.put("beginTime", beginTime);
            param.put("endTime", endTime);
            Double turnover = orderMapper.getDailyTurnover(param);
            turnover = turnover == null ? 0.0 : turnover;
            turnovers.add(turnover);
        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .turnoverList(StringUtils.join(turnovers, ","))
                .build();
    }

    @Override
    public OrderReportVO ordersStatistics(LocalDate beginDate, LocalDate endDate) {
        // 计算beginDate到endDate的所有天
        List<LocalDate> dates = new ArrayList<>();
        dates.add(beginDate);
        while (!beginDate.equals(endDate)) {
            beginDate = beginDate.plusDays(1);
            dates.add(beginDate);
        }
        // 统计每一天的订单总数、有效订单数量（有效的定义为处于‘完成’状态的订单）
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> vaildOrderCountList = new ArrayList<>();
        for (LocalDate date : dates) {
            // 通过日期API计算date这一天的开始时刻和结束时刻
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 调用mapper查询数据库，查询参数通过Map形式传递
            Map<String, Object> param = new HashMap<>();
            param.put("beginTime", beginTime);
            param.put("endTime", endTime);
            Integer orderCount = orderMapper.getDailyOrderCount(param);
            orderCount = orderCount == null ? 0 : orderCount;
            orderCountList.add(orderCount);
            param.put("status", Orders.COMPLETED);
            Integer vaildOrderCount = orderMapper.getDailyOrderCount(param);
            vaildOrderCount = vaildOrderCount == null ? 0 : vaildOrderCount;
            vaildOrderCountList.add(vaildOrderCount);
        }
        int total = orderCountList.stream().mapToInt(Integer::intValue).sum();
        int vaild = vaildOrderCountList.stream().mapToInt(Integer::intValue).sum();
        double completionRate = 0;
        if (total > 0) {
            completionRate = (double) vaild / total;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(vaildOrderCountList, ","))
                .orderCompletionRate(completionRate)
                .totalOrderCount(total)
                .validOrderCount(vaild)
                .build();
    }

    @Override
    public UserReportVO userStatistics(LocalDate beginDate, LocalDate endDate) {
        // 计算beginDate到endDate的所有天
        List<LocalDate> dates = new ArrayList<>();
        dates.add(beginDate);
        while (!beginDate.equals(endDate)) {
            beginDate = beginDate.plusDays(1);
            dates.add(beginDate);
        }
        // 统计date日新增用户数，统计截止date的所有用户数
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dates) {
            // 通过日期API计算date这一天的开始时刻和结束时刻
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 调用mapper查询数据库，查询参数通过Map形式传递
            Map<String, Object> param = new HashMap<>();

            param.put("endTime", beginTime);  // 截止到今天之前的用户数据
            Integer totalUserCount = userMapper.getUserCount(param);
            totalUserCount = totalUserCount == null ? 0 : totalUserCount;
            totalUserList.add(totalUserCount);

            param.put("beginTime", beginTime);
            param.put("endTime", endTime);
            Integer newUserCount = userMapper.getUserCount(param);
            newUserCount = newUserCount == null ? 0 : newUserCount;
            newUserList.add(newUserCount);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dates, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        // 统计[begin, end]日期区间内销量最高的10个商品（包括菜品和套餐）
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 统计orders表中结账时间位于区间[beginTime, endTime]的所有订单所涉及到商品的数量统计
        // 1. 查询符合条件的订单ID（已结账的）
        List<Long> orderIdList = orderMapper.getVaildOrderCountWithCheckoutTime(beginTime, endTime);

        if (CollectionUtils.isEmpty(orderIdList)) {
            return SalesTop10ReportVO.builder().nameList("").numberList("").build();
        }

        // 2. 查询对应订单的明细，分组统计销量
        List<Map<String, Object>> top10List = orderDetailMapper.getTop10SalesCount(orderIdList);

        // 3. 拆分结果，组装 VO
        List<String> names = new ArrayList<>();
        List<BigDecimal> numbers = new ArrayList<>();

        for (Map<String, Object> map : top10List) {
            names.add((String) map.get("name"));
            numbers.add((BigDecimal) map.get("num"));
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     *
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件发送到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
