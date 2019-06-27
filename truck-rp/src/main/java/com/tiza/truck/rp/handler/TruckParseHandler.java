package com.tiza.truck.rp.handler;

import cn.com.tiza.earth4j.LocationParser;
import cn.com.tiza.tstar.common.process.BaseHandle;
import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.Gb32960Header;
import com.tiza.plugin.model.Header;
import com.tiza.plugin.model.facade.IDataProcess;
import com.tiza.plugin.protocol.gb32960.Gb32960DataProcess;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.plugin.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: TruckParseHandler
 * Author: DIYILIU
 * Update: 2019-06-19 15:31
 */

@Slf4j
public class TruckParseHandler extends BaseHandle {

    @Override
    public RPTuple handle(RPTuple rpTuple) {
        log.info("终端[{}], 指令[{}]...", rpTuple.getTerminalID(), CommonUtil.toHex(rpTuple.getCmdID(), 2));
        ICache cmdCacheProvider = SpringUtil.getBean("cmdCacheProvider");
        Gb32960DataProcess process = (Gb32960DataProcess) cmdCacheProvider.get(rpTuple.getCmdID());
        if (process == null) {
            log.warn("CMD {}, 找不到指令[{}]解析器!", JacksonUtil.toJson(cmdCacheProvider.getKeys()), CommonUtil.toHex(rpTuple.getCmdID(), 2));
            return null;
        }

        // 解析消息头
        Gb32960Header header = (Gb32960Header) process.parseHeader(rpTuple.getMsgBody());
        if (header != null) {
            String terminal = header.getVin();

            header.setGwTime(rpTuple.getTime());
            if (parse(terminal, header.getContent(), header, process)) {
                return rpTuple;
            }
        }

        return null;
    }

    public boolean parse(String terminal, byte[] bytes, Header header, IDataProcess process){
        ICache vehicleInfoProvider = SpringUtil.getBean("vehicleInfoProvider");
        //log.info("设备缓存: {}", JacksonUtil.toJson(vehicleInfoProvider.getKeys()));
        // 验证设备是否绑定车辆
        if (!vehicleInfoProvider.containsKey(terminal)) {
            log.warn("设备[{}]未绑定车辆信息!", terminal);

            return false;
        }
        process.parse(bytes, header);
        return true;
    }

    @Override
    public void init() throws Exception {
        // 加载地图数据，解析省市区
        LocationParser.getInstance().init();

        // 装载 Spring 容器
        SpringUtil.init();
    }
}
