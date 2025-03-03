package org.suda;

import lombok.Data;

@Data
public class User {
    String name;
    int age;
    final void sayHello() {}
}
