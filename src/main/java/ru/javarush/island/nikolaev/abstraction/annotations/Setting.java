package ru.javarush.island.nikolaev.abstraction.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Setting {
    String name();

    String icon();

    double maxWeight();

    int maxCount();

    int maxSpeed();

    double maxFood();

    int idFromTheSpecTable();

}
