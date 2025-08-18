package com.sky;

import io.lettuce.core.ScriptOutputType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * Question：主线程等待其他线程返回数据，再继续执行的方式包括？
 * 1、Future和Callable（同步执行）
 * 2、CompletableFuture（支持异步执行）
 * 3、CountDown
 * 4、join()
 * 5、阻塞队列
 */
public class MultiThreadCollaboration {
    @Test
    public void testFutureWithCallable() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<Integer> task = () -> {
            Thread.sleep(100);
            return 10;
        };
        Future<Integer> future = executorService.submit(task);
        Integer result = null;
        try {
            result = future.get();  // 主线程阻塞等待任务线程执行结果返回
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("result is %d\n", result);
    }

    /**
     * output:
     * main; 2025-08-18T12:44:20.830368008; 主线程继续执行
     * execution task thread: ForkJoinPool.commonPool-worker-1
     * ForkJoinPool.commonPool-worker-1; 2025-08-18T12:44:20.930396083; result is 10
     */
    @Test
    public void testCompletableFuture() throws InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("execution task thread: " + Thread.currentThread().getName());
            return 10;
        });  // 使用线程池中线程执行提交的任务
//        try {
//            Integer integer = future.get();  // 阻塞等待
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }

        future.thenAccept(r -> System.out.println(Thread.currentThread().getName() + "; " + LocalDateTime.now() + "; result is " + r));  // 注册回调函数，主线程可以继续执行（非阻塞）
        // 回调函数在执行任务的线程上执行
        System.out.println(Thread.currentThread().getName() + "; " + LocalDateTime.now() + "; 主线程继续执行");
        Thread.sleep(200);
    }
}
