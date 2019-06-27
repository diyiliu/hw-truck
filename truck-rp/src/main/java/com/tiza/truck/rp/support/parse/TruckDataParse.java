package com.tiza.truck.rp.support.parse;

import cn.com.tiza.tstar.common.process.BaseHandle;
import com.tiza.plugin.bean.VehicleInfo;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.DeviceData;
import com.tiza.plugin.model.Position;
import com.tiza.plugin.model.adapter.DataParseAdapter;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.truck.rp.support.model.KafkaMsg;
import com.tiza.truck.rp.support.util.KafkaUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.storm.guava.collect.Lists;
import org.apache.storm.guava.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * Description: TruckDataParse
 * Author: DIYILIU
 * Update: 2019-06-19 17:31
 */

@Slf4j
@Service
public class TruckDataParse extends DataParseAdapter {
    private final List<String> canIds = Lists.newArrayList("91");

    @Value("${tstar.track.topic}")
    private String trackTopic;

    @Value("${tstar.work.topic}")
    private String workTopic;

    @Resource
    private ICache vehicleInfoProvider;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void detach(DeviceData deviceData) {
        String device = deviceData.getDeviceId();

        byte[] bytes = device.getBytes();
        if (bytes == null || bytes.length < 10) {
            return;
        }

        Map paramMap = new HashMap();
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        while (buf.readableBytes() > 9) {
            int id = buf.readUnsignedShort();
            byte[] content = new byte[8];
            buf.readBytes(content);

            Map pack = unpack(id, content);
            paramMap.putAll(pack);
        }
        // 更新工况当前信息
        updateWorkInfo(device, paramMap);

        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(device);
        // 写入kafka 轨迹数据
        sendToTStar(String.valueOf(vehicleInfo.getId()), deviceData.getCmdId(), JacksonUtil.toJson(paramMap), deviceData.getTime(), workTopic);
    }

    @Override
    public void dealWithTStar(DeviceData deviceData, BaseHandle handle) {
        // gb32960 0x02
        if (deviceData.getDataBody() != null) {
            List<Map> paramValues = (List<Map>) deviceData.getDataBody();

            for (int i = 0; i < paramValues.size(); i++) {
                Map map = paramValues.get(i);
                for (Iterator iterator = map.keySet().iterator(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    Object value = map.get(key);

                    // 位置信息
                    if (key.equalsIgnoreCase("position")) {
                        Position position = (Position) value;
                        position.setTime(deviceData.getTime());
                        position.setCmd(deviceData.getCmdId());

                        dealPosition(deviceData.getDeviceId(), position);
                    }

                    // 透传数据
                    if (canIds.contains(key)) {
                        byte[] bytes = (byte[]) value;
                        deviceData.setBytes(bytes);
                        deviceData.setDataType(key);

                        detach(deviceData);
                    }
                }
            }
        }
    }


    public void sendToTStar(String device, int cmd, String value, long time, String topic) {
        byte[] bytes = CommonUtil.tstarKafkaArray(device, cmd, value, time, 0);
        KafkaMsg msg = new KafkaMsg(device, bytes, topic);
        msg.setTstar(true);
        KafkaUtil.send(msg);
    }

    @Override
    public void sendToDb(String sql, Object... args) {

        jdbcTemplate.update(sql, args);
    }

    @Override
    public void dealPosition(String deviceId, Position position) {
        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(deviceId);

        Object[] args = new Object[]{position.getLng(), position.getLat(), position.getEnLng(), position.getEnLat(), position.getProvince(), position.getCity(), position.getArea(),
                new Date(position.getTime()), new Date(), vehicleInfo.getId()};

        String sql = "UPDATE serv_device_position " +
                "SET " +
                " longitude = ?," +
                " latitude = ?," +
                " encrypt_long = ?," +
                " encrypt_lat = ?," +
                " province = ?," +
                " city = ?, " +
                " area = ?, " +
                " gps_time= ?," +
                " modified_date = ?" +
                "WHERE " +
                "device_id = ?";

        sendToDb(sql, args);
        // 写入kafka 轨迹数据
        sendToTStar(String.valueOf(vehicleInfo.getId()), position.getCmd(), JacksonUtil.toJson(position), position.getTime(), trackTopic);
    }

    /**
     * 更新当前位置信息
     *
     * @param terminal
     * @param map
     */
    public void updateWorkInfo(String terminal, Map map) {
        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(terminal);

        String sql = "SELECT t.data_json  FROM serv_device_work t WHERE t.device_id=" + vehicleInfo.getId();
        String json = jdbcTemplate.queryForObject(sql, String.class);

        // 工况参数
        Map workMap = new HashMap();
        try {
            if (StringUtils.isNotEmpty(json)) {
                workMap = JacksonUtil.toObject(json, HashMap.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 覆盖历史工况信息
        workMap.putAll(map);

        json = JacksonUtil.toJson(workMap);
        Object[] args = new Object[]{json, new Date(), vehicleInfo.getId()};
        sql = "UPDATE serv_device_work " +
                "SET " +
                " data_json = ?, " +
                " modified_date = ? " +
                "WHERE " +
                "device_id = ?";

        sendToDb(sql, args);
    }

    public Map unpack(int id, byte[] bytes) {
        Map map = Maps.newHashMap();

        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        if (0x0136 == id) {
            buf.readByte();
            int instEcon = buf.readUnsignedShort();
            int fuelCapacity = buf.readUnsignedShort();
            map.put("instEcon", instEcon);
            map.put("fuelCapacity", fuelCapacity);
        } else if (0x236 == id) {
            int waterTemp = buf.readUnsignedShort();
            double oilPressure = CommonUtil.keepDecimal(buf.readUnsignedShort(), 0.1, 1);
            int engineSpeed = buf.readUnsignedShort();
            int oilLevel = buf.readUnsignedShort();
            map.put("waterTemp", waterTemp);
            map.put("oilPressure", oilPressure);
            map.put("engineSpeed", engineSpeed);
            map.put("oilLevel", oilLevel);
        } else if (0x237 == id) {
            byte b = buf.readByte();
            String str = CommonUtil.byte2BinaryStr(b);
            int clearWater0 = Integer.valueOf(str.substring(0, 1));
            int clearWater1 = Integer.valueOf(str.substring(1, 2));
            int wasteWater0 = Integer.valueOf(str.substring(2, 3));
            int wasteWater1 = Integer.valueOf(str.substring(3, 4));
            int levelWater0 = Integer.valueOf(str.substring(4, 5));
            int levelWater1 = Integer.valueOf(str.substring(5, 6));
            // 预留
            buf.readByte();
            int weight = buf.readUnsignedShort();
            double flow = CommonUtil.keepDecimal(buf.readUnsignedShort(), 0.1, 1);

            map.put("clearWater0", clearWater0);
            map.put("clearWater1", clearWater1);
            map.put("wasteWater0", wasteWater0);
            map.put("wasteWater1", wasteWater1);
            map.put("levelWater0", levelWater0);
            map.put("levelWater1", levelWater1);
            map.put("weight", weight);
            map.put("flow", flow);
        } else if (0x238 == id) {
            double hDeg = CommonUtil.keepDecimal(buf.readUnsignedShort() - 10000, 0.1, 1);
            double vDeg = CommonUtil.keepDecimal(buf.readUnsignedShort() - 10000, 0.1, 1);
            int full = buf.readByte();

            map.put("hDeg", hDeg);
            map.put("vDeg", vDeg);
            map.put("full", full);
        }

        return map;
    }
}
