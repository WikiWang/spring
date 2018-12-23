package cn.edu.buaa.wk.spring.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WkController {
    String value() default "";
}
