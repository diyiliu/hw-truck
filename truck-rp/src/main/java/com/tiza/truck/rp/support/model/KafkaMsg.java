package com.tiza.truck.rp.support.model;

import lombok.Data;

/**
 * Description: KafkaMsg
 * Author: DIYILIU
 * Update: 2019-06-14 11:46
 */

@Data
public class KafkaMsg {

    public KafkaMsg() {
    }

    public KafkaMsg(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public KafkaMsg(String key, Object value, String topic) {
        this.key = key;
        this.value = value;
        this.topic = topic;
    }

    private String key;

    private Object value;

    private boolean tstar = false;

    private String topic;
}
