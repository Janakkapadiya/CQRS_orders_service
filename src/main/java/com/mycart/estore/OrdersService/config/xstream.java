package com.mycart.estore.OrdersService.config;

import com.thoughtworks.xstream.XStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class xstream {
    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();

        xStream.allowTypesByWildcard(new String[] {
                "com.mycart.estore**",
                "com.mycart.estore.core**"
        });

        return xStream;
    }
}