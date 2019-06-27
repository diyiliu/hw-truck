package com.tiza.truck.rp.support.util;

import com.tiza.truck.rp.support.model.KafkaMsg;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import kafka.serializer.DefaultEncoder;
import kafka.serializer.StringEncoder;

import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description: KafkaUtil
 * Author: DIYILIU
 * Update: 2019-04-24 14:26
 */

public class KafkaUtil {
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private final static Queue<KafkaMsg> pool = new ConcurrentLinkedQueue();
    private final static Queue<KafkaMsg> pool2 = new ConcurrentLinkedQueue();

    private String brokers;

    private Producer producer;

    private Producer producer2;

    public KafkaUtil() {

    }

    public void init() {
        Properties props = new Properties();
        props.put("metadata.broker.list", brokers);
        // 消息传递到broker时的序列化方式
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("serializer.class", StringEncoder.class.getName());
        // 是否获取反馈
        props.put("request.required.acks", "1");
        // 内部发送数据是异步还是同步 sync：同步(来一条数据提交一条不缓存), 默认 async：异步
        props.put("producer.type", "async");
        // 重试次数
        props.put("message.send.max.retries", "3");
        producer = new Producer(new ProducerConfig(props));

        // 生产 tstar 数据
        props.put("serializer.class", DefaultEncoder.class.getName());
        producer2 = new Producer(new ProducerConfig(props));

        buildSchedule(producer, 1, pool);
        buildSchedule(producer2, 2, pool2);
    }

    public void setBrokers(String brokers) {
        this.brokers = brokers;
    }

    public static void send(KafkaMsg msg) {
        if (msg.isTstar()) {
            pool2.add(msg);
        } else {
            pool.add(msg);
        }
    }

    public void buildSchedule(final Producer producer, int interval, final Queue<KafkaMsg> pool) {
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                while (!pool.isEmpty()) {
                    KafkaMsg data = pool.poll();
                    producer.send(new KeyedMessage(data.getTopic(), data.getKey(), data.getValue()));
                }
            }
        }, 10, interval, TimeUnit.SECONDS);
    }
}
