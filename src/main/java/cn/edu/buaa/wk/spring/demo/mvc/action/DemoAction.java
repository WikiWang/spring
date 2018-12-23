package cn.edu.buaa.wk.spring.demo.mvc.action;

import cn.edu.buaa.wk.spring.framework.webmvc.annotation.WkAutowired;
import cn.edu.buaa.wk.spring.framework.webmvc.annotation.WkController;
import cn.edu.buaa.wk.spring.framework.webmvc.annotation.WkRequestMapping;
import cn.edu.buaa.wk.spring.framework.webmvc.annotation.WkRequestParam;
import cn.edu.buaa.wk.spring.demo.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WkController
@WkRequestMapping("/demo")
public class DemoAction {

    @WkAutowired
    private IDemoService demoService;

    @WkRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse res,
                      @WkRequestParam("name") String name){

        String result = demoService.get(name);

        try {
            res.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @WkRequestMapping("/add")
    public void add (HttpServletRequest req, HttpServletResponse res,
                     @WkRequestParam("a") Integer a, @WkRequestParam("b") Integer b){

        try {
            res.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @WkRequestMapping("/remove")
    public void remove (HttpServletRequest req, HttpServletResponse res,
                        @WkRequestParam("id") Integer id){

    }

}
