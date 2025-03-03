package org.suda;

final public class FinalTest {
    void func(final User user) {
        // 1、final修饰引用类型
        // 不能对final修饰的对象变量重新赋值，即更改其引用的对象
//        obj = new Object();  // Cannot assign a value to final variable 'obj'
        user.age = 18;  // 但是可以修改其属性
        // 2、final修饰基本数据类型
        final int const_var = 10;
//        const_var = 23;
    }
}
/**
 * 3、final修饰class, 表明该类不能被继承
class DeriveClass extends FinalTest {  // Cannot inherit from final 'org.suda.FinalTest'

} */
class Student extends User {
    // 4、final修饰方法，表明该方法不能被重写
//    @Override
//    void sayHello() {  // 'sayHello()' cannot override 'sayHello()' in 'org.suda.User'; overridden method is final
//
//    }
}


