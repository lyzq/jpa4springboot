package com.fleeting.jpa.annotation;

import java.lang.annotation.*;

/**
 * Created by cxx on 2017/8/31.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryField {
}
