package cn.edu.buaa.wk.spring.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WkService {
    String value() default "";
}
