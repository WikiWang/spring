package cn.edu.buaa.wk.spring.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WkRequestMapping {
    String value() default "";
}
