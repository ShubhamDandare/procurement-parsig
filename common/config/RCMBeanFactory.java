package com.kpmg.rcm.sourcing.common.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RCMBeanFactory implements ApplicationContextAware {

    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
        log.info("Application context set.");
    }

    public static ApplicationContext getApplicationContext(){
        return ctx;
    }

    public static Object getBean(String name){
        return ctx.getBean(name);
    }
}
