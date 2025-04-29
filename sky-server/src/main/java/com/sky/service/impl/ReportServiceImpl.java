package com.sky.service.impl;

import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.context.BaseContext;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
