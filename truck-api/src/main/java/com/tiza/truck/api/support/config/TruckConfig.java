package com.tiza.truck.api.support.config;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * Description: TruckConfig
 * Author: DIYILIU
 * Update: 2019-07-01 10:23
 */

@Configuration
public class TruckConfig {

    @Resource
    private Environment environment;

    @Bean
    public TStarSimpleClient tStarClient() throws Exception{
        String username = environment.getProperty("tstar.username");
        String password = environment.getProperty("tstar.password");

        return new TStarSimpleClient(username, password);
    }
}
