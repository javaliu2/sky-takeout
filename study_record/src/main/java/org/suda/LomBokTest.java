package org.suda;

/**
 * User类被 @Data 修饰，生成的字节码文件反编译后的java代码中自动生成了
 * User中每一个属性的getter, setter, 无参构造, 重写Object的equals、hashCode、toString方法
 * @AllArgsConstructor 生成全参数的构造方法，但是这样的话，无参构造就不存在了
 * @NoArgsConstructor 一般无参也是需要的，所以这个无参构造的注解也需要加上
 * @Builder 生成与属性名一致的方法，这些方法返回Builder对象本身，因此可以用于连续属性赋值
 */
public class LomBokTest {
    public static void main(String[] args) {
        // 1、builder()返回一个UserBuilder对象
        // 2、然后通过.attribute1().attribute2().,,,.attribute_n()的方式对User属性赋值
        // 3、最后通过build()返回刚才设置的属性值的User对象
        // builder在这里充当一个属性承载中间器的角色
        User user = User.builder().name("春蕊").age(24).build();
        System.out.println(user);
    }
}
