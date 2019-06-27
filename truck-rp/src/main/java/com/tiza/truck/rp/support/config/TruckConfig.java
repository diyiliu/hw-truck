package com.tiza.truck.rp.support.config;

import com.tiza.truck.rp.support.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description: TruckConfig
 * Author: DIYILIU
 * Update: 2019-06-26 14:53
 */

@Slf4j
@Configuration
@EnableScheduling
@PropertySource("classpath:config.properties")
public class TruckConfig {

    @Value("${kafka.broker-list}")
    private String brokerList;

    @Bean
    public KafkaUtil kafkaUtil(){
        KafkaUtil kafkaUtil = new KafkaUtil();
        kafkaUtil.setBrokers(brokerList);
        kafkaUtil.init();

        return kafkaUtil;
    }
}
