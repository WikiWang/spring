package cn.edu.buaa.wk.spring.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WkAutowired {
    String value() default "";
}
