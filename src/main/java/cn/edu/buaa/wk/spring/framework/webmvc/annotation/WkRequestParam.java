package cn.edu.buaa.wk.spring.framework.webmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WkRequestParam {
    String value() default "";
}
