package com.xynnity.watermanagement.websocket;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping(ApplicationContext applicationContext) {
        Map<String, WebSocketHandler> urlMap = new LinkedHashMap<>();
        var beans = applicationContext.getBeansWithAnnotation(ServerWebSocket.class);
        beans.forEach((name, bean) -> {
            Assert.isInstanceOf(WebSocketHandler.class, bean,
                    () -> "Bean annotated with @ServerWebSocket must implement WebSocketHandler: " + name);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            var annotation = AnnotationUtils.findAnnotation(targetClass, ServerWebSocket.class);
            Assert.notNull(annotation, () -> "Unable to resolve @ServerWebSocket annotation for bean " + name);
            urlMap.put(annotation.value(), (WebSocketHandler) bean);
        });
        var mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(urlMap);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}


