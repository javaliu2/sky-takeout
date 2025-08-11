package com.sky;

import com.sky.entity.Employee;
import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@MapperScan("com.sky.mapper")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CASVersionDemo {

    @Autowired
    private EmployeeMapper employeeMapper;
    /**
     * 多个线程同时更新数据库记录时，使用version字段CAS机制避免数据不一致问题演示
     */
    @Test
    public void testThreadPoolStudy() throws InterruptedException {
        // 1、连接数据库(框架代做)
        // 2、使用线程池，提交多个任务，不带有version，这里也不会出错，因为数据库会有锁，但是数据会被更新n（线程个数）次
        // 2.1、自定义线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());
        // 2.2、定义任务
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Runnable task = () -> {
                // 没有使用try-catch之前，就算是有NPE也没有异常提示
                // 所以说在Runnable任务重使用try-catch很有必要
                try {
                    Employee employee = employeeMapper.getByUsername("刘晓松");  // username为刘晓松的没有，故返回空对象
                    System.out.println("employee: " + employee);  // null
                    employee.setPhone("15042132118");  // throw NPE
                    employeeMapper.update(employee);
                } catch (Exception e) {
                    System.out.println(e.getMessage());  // Cannot invoke "com.sky.entity.Employee.setPhone(String)" because "employee" is null
                }
            };
            tasks.add(task);
        }
        // 2.3、提交任务
        for (Runnable task : tasks) {
            Future<?> submit = threadPoolExecutor.submit(task);
        }
        /**
         * Initiates an orderly shutdown in which previously submitted tasks are executed,
         * but no new tasks will be accepted.
         * 发起一个有序关闭：已提交的任务会执行完，但不会再接收新任务。
         * Invocation has no additional effect if already shut down.
         * 如果线程池已经关闭，那么重复调用不会有额外的影响。
         * This method does not wait for previously submitted tasks to complete execution.
         * Use awaitTermination to do that.
         * 该方法不会等待先前已经提交的任务完成执行。使用awaitTermination()来完成这个功能。
         * 这句话的意思是说，调用该方法的线程不会阻塞等待线程池中任务执行完，需要programmer显式使用awaitTermination()完成等待
         */
        threadPoolExecutor.shutdown();  // 提交关闭信号 + 立即返回
        boolean finished = threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);  // 阻塞current线程阻塞等待线程池中任务全部执行完毕或者达到超时时间
        // more: 达到超时时间，也不会停止线程池中没有执行完的任务，只是当前线程继续执行了
        System.out.println("线程池正确结束与否: " + finished);  // 线程池正确结束与否: true
        // 3、通过version字段CAS机制保证一条记录在并发场景下只被修改一次
    }

    @Test
    public void testWithoutCASVersion() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());
        // 2.2、定义任务
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Runnable task = () -> {
                try {
                    Employee employee = employeeMapper.getByUsername("lxs");
                    System.out.println("employee: " + employee);
                    employee.setPhone("15042132118");
                    employeeMapper.update(employee);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            };
            tasks.add(task);
        }
        // 2.3、提交任务
        for (Runnable task : tasks) {
            Future<?> submit = threadPoolExecutor.submit(task);
        }
        threadPoolExecutor.shutdown();
        boolean finished = threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("线程池任务是否跑完: " + finished);
        // output: 查询到5条数据，更新了5次
    }

    /**
     * bug记录，xml文件中#{property}不能参与算数运算，version的更新直接使用sql原生语句，<where>中是and条件且没有逗号</where>
     * @throws InterruptedException
     */
    @Test
    public void testWithCASVersion() throws InterruptedException {
        // 通过version字段CAS机制保证一条记录在并发场景下只被修改一次
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 5, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());
        // 1、定义任务
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Runnable task = () -> {
                try {
                    Employee employee = employeeMapper.getByUsername("lxs");
                    System.out.println("employee: " + employee);
                    employee.setPhone("19803300595");
                    employeeMapper.updateWithCAS(employee);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            };
            tasks.add(task);
        }
        // 2、提交任务
        for (Runnable task : tasks) {
            Future<?> submit = threadPoolExecutor.submit(task);
        }
        threadPoolExecutor.shutdown();
        boolean finished = threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("线程池任务是否跑完: " + finished);
    }
}
