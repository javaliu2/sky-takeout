package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 员工注册
     * @param employeeDTO
     */

    @Override
    public void register(EmployeeDTO employeeDTO) {
        // 保存的是entity，因此需要设置entity对象的属性
        // 由于 EmployeeDTO 是 Employee 的子类，属性一致，因此使用spring提供的库函数完成属性的赋值
        Employee newEmployee = new Employee();
        BeanUtils.copyProperties(employeeDTO, newEmployee);
        // 对 Employee 的其他属性进行赋值
        // 由于对公共字段进行统一赋值操作，因此这里不需要进行处理
//  1      newEmployee.setCreateTime(LocalDateTime.now());
//  2     newEmployee.setUpdateTime(LocalDateTime.now());
//        newEmployee.setId(1);  // 不用设置，因为id是由数据库在管理维护的，这也减少了业务代码的复杂度，体现了功能分层的思想
//        newEmployee.setCreateUser(10L);  // 需要追加'L'以表明这是long数据类型的数
//        newEmployee.setUpdateUser(10L);
        // 通过ThreadLocal设置线程级别变量，获取当前进行员工注册操作人的id
//  3     newEmployee.setCreateUser(BaseContext.getCurrentId());
//  4     newEmployee.setUpdateUser(BaseContext.getCurrentId());

        newEmployee.setStatus(StatusConstant.ENABLE);  // 将常量提取出来管理，便于后期维护
        newEmployee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));  // 同理

        // 持久层操作
        employeeMapper.insert(newEmployee);
    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 借助PageHelper完成分页查询功能的实现
        // 通过mysql提供的库函数limit完成指定页数数据的查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> data = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = data.getTotal();
        List<Employee> records = data.getResult();
        return new PageResult(total, records);
    }

    /**
     * 启用或者禁用员工 逻辑实现
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 对应的sql语句为 update employee set status = ? where id = ?
        // 考虑到别的功能也涉及到employee表的更新，因此这里对Employee进行更新
        // 使用<set>标签设置非空，即需要更新的字段为新的值
        Employee employee = Employee.builder().status(status).id(id).build();
        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工 逻辑实现
     * @param id
     * @return
     */
    @Override
    public Employee queryById(Long id) {
        // select * from employee where id = ?
        Employee employee = employeeMapper.queryByID(id);
        return employee;
    }

    /**
     * 根据id修改员工信息 逻辑实现
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.update(employee);
    }
}
