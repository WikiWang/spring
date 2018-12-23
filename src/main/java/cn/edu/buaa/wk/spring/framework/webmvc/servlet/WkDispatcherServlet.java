package cn.edu.buaa.wk.spring.framework.webmvc.servlet;

import cn.edu.buaa.wk.spring.framework.webmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jdk.nashorn.api.scripting.ScriptUtils.convert;

public class WkDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();//配置文件
    private List<String> classNames = new ArrayList<String>();//所有扫描到的类列表
    private Map<String, Object> ioc = new HashMap<String, Object>();
//    private Map<String, Method> handleMapping = new HashMap<String, Method>();
    private List<Handler> handlers = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

        //6.等待用户请求
        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url.replace(contextPath, "").replaceAll("/+", "/");

        Handler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("Not Found 404!");
            return;
        }

        Method method =handler.method;

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object [] paramValues= new Object[parameterTypes.length];

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String value =Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

            if(!handler.paramIndexMapping.containsKey(entry.getKey())){continue;}
            int index = handler.paramIndexMapping.get(entry.getKey());
            paramValues[index] = convert(value, parameterTypes[index]);
        }

        paramValues[handler.paramIndexMapping.get(HttpServletRequest.class.getName())] = req;
        paramValues[handler.paramIndexMapping.get(HttpServletResponse.class.getName())] = resp;

        handler.method.invoke(handler.controller, paramValues);

        System.out.println("Method: " + method);
    }

    private Object converts(Class<?> type, String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return value;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.解析配置文件，读取scanPackage的值，扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3.实例化扫描到的相关的类
        doInstance();

        //4.自动化的依赖注入
        doAutowired();

        //===============Spring================

        //5.初始化HandlerMapping
        initHandlerMapping();
    }

    private void initHandlerMapping() {

        if (ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(WkController.class)){
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(WkRequestMapping.class)){
                WkRequestMapping wkRequestMapping = clazz.getAnnotation(WkRequestMapping.class);
                baseUrl = wkRequestMapping.value();
            }

//            Method[] methods = clazz.getMethods();
//            for (Method method : methods) {
//                if (!method.isAnnotationPresent(WkRequestMapping.class))continue;
//                WkRequestMapping wkRequestMapping = method.getAnnotation(WkRequestMapping.class);
//                String url = wkRequestMapping.value();
//                url = (baseUrl + "/" + url).replaceAll("/+", "/");
//
//                this.handleMapping.put(url, method);
//                System.out.println("Mapped: " + url + "," + method);
//            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(WkRequestMapping.class))continue;
                WkRequestMapping wkRequestMapping = method.getAnnotation(WkRequestMapping.class);
                String regex = (baseUrl + "/" + wkRequestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                this.handlers.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("Mapped: " + regex + "," + method);
            }

        }
    }

    private void doAutowired() {

        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            //赋值就是给需要赋值的字段，从ioc容器中取一个值设进去

            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            //强制赋值
            for (Field field : fields) {

                if(!field.isAnnotationPresent(WkAutowired.class)){
                    continue;
                }
                WkAutowired wkAutowired = field.getAnnotation(WkAutowired.class);
                String beanName = wkAutowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                //真正暴力访问的骚操作
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {

        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String name : classNames) {

                Class<?> clazz = Class.forName(name);

                if (clazz.isAnnotationPresent(WkController.class)) {

                    Object instanse = clazz.newInstance();

                    //spring中beanId默认是类名首字母小写
                    String beanName = lowerFirstCase(clazz.getSimpleName());

                    ioc.put(beanName, instanse);

                } else if (clazz.isAnnotationPresent(WkService.class)) {

                    //1、用自己的命名
                    //2、用默认的命名
                    //3、把接口实现类的引用赋值给接口

                    WkService wkService = clazz.getAnnotation(WkService.class);

                    String beanName = wkService.value();

                    if ("".equals(beanName)) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = null;

                    instance = clazz.newInstance();


                    ioc.put(beanName, instance);

                    Class<?>[] interfaces = clazz.getInterfaces();

                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), instance);
                    }

                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        //递归
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        File classDir = new File(url.getFile());

        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private Handler getHandler(HttpServletRequest req) throws Exception{
        if (handlers.isEmpty()){return null;}

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlers) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()){continue;}
            return handler;
        }
        return null;
    }

    private class Handler {
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping;//参数顺序

        protected Handler(Pattern pattern, Object controller, Method method){

            this.controller = controller;
            this.pattern = pattern;
            this.method = method;

            this.paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {

            Annotation [] [] annotations = method.getParameterAnnotations();

            for (int i = 0; i < annotations.length; i++) {
                for (Annotation annotation : annotations[i]) {
                    if (annotation instanceof WkRequestParam) {
                        String paramName = ((WkRequestParam) annotation).value();
                        if(!"".equals(paramName)){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            Class<?> [] paramTypes = method.getParameterTypes();

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> type = paramTypes[i];
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }

}
