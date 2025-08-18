package com.sky;

import com.sky.entity.Employee;
import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务传播(propagation)行为: 一个方法被另外一个有事务的方法调用时，应该如何处理事务？
 * 常见传播行为：
 * 1）required（默认）：如果存在事务，加入；否则创建新事务
 * 2）required_new：总是新建新事务，如果存在事务，则挂起当前事务
 * 3）nested：如果存在事务，则在当前事务内部开启嵌套事务（savepoint），否则新建事务
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionPropagation {
    @Autowired
    private EmployeeService employeeService;
    @Test
    public void testRequired() {
        employeeService.method1();
    }

    @Test
    public void testRequiredNew() {
        employeeService.methodA();
    }
}
