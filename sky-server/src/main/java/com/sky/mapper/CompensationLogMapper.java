package com.sky.mapper;

import com.sky.entity.CompensationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompensationLogMapper {
    @Insert("insert into compensation_log(orderId, beforeStatus, afterStatus, reason, operator, operateTime) " +
            "values (#{orderId}, #{beforeStatus}, #{afterStatus}, #{reason}, #{operator}, #{operateTime})")
    int insert(CompensationLog log);
}
