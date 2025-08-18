package com.sky;

import com.sky.entity.Employee;
import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Transactional
    public void method1() {
        Employee employee = employeeMapper.getByUsername("lxs");
        method2();  // method2()默认是required，会加入method1的事务
    }

    @Transactional
    public void method2() {
        Employee employee = employeeMapper.queryByID(1L);  // 同一个事务
    }

    @Transactional
    public void methodA() {
        Employee employee = employeeMapper.getByUsername("lxs");
        methodB();
        throw new RuntimeException();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        Employee employee = employeeMapper.queryByID(1L);  // 新事务，与methodA的事务无关
        employee.setPhone("15042132118");
        employeeMapper.update(employee);
    }
}
