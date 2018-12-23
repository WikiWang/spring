package cn.edu.buaa.wk.spring.demo.service.impl;

import cn.edu.buaa.wk.spring.framework.webmvc.annotation.WkService;
import cn.edu.buaa.wk.spring.demo.service.IDemoService;

@WkService
public class DemoService implements IDemoService {

    public String get(String name) {
        return "My name is " + name;
    }
}
