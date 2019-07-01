package com.tiza.truck.api.controller;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import cn.com.tiza.tstar.datainterface.client.entity.ClientCmdSendResult;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.DateUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.truck.api.support.facade.CommandSendJpa;
import com.tiza.truck.api.support.facade.dto.CommandSend;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Description: TruckController
 * Author: DIYILIU
 * Update: 2019-07-01 10:20
 */

@Slf4j
@RestController
public class TruckController {

    @Resource
    private TStarSimpleClient tStarClient;

    @Resource
    private CommandSendJpa commandSendJpa;

    @Value("${tstar.terminal-type}")
    private String terminalType;

    @GetMapping("/send/{id}")
    public String sendCmd(@PathVariable Long id) throws Exception {
        CommandSend commandSend = commandSendJpa.findById(id).get();

        int cmd = commandSend.getCmdId();
        String vin = commandSend.getTerminalNo();
        String paramStr = commandSend.getSendData();
        byte[] bytes = null;
        switch (cmd) {
            case 0x80:
                bytes = queryParam(paramStr);
                break;
            case 0x81:
                if (paramStr.startsWith("128|")) {
                    bytes = setExtra(paramStr);
                } else {
                    bytes = setParam(paramStr);
                }
                break;
            default:
                log.info("无效指令: {}", cmd);
        }

        if (bytes != null) {
            Date now = new Date();
            // 时间
            byte[] time = CommonUtil.dateToBytes(now);
            // 生成下发指令
            byte[] content = CommonUtil.gb32960Response(vin, Unpooled.copiedBuffer(time, bytes).array(), cmd, false);
            log.info("设备[{}]原始指令下发: [{}]", vin, CommonUtil.bytesToStr(content));

            // TStar 指令下发
            ClientCmdSendResult sendResult = tStarClient.cmdSend(terminalType, vin, cmd, CommonUtil.getMsgSerial(), content, 1);
            if (sendResult.getIsSuccess()) {
                log.info("TSTAR 执行结果: [成功]!");
                commandSend.setStatus(1);
            } else {
                log.info("TSTAR 执行结果: [失败]!");
                commandSend.setStatus(4);
                commandSend.setErrorCode(sendResult.getErrorCode());
            }

            String hms = DateUtil.dateToString(now, "%1$tH%1$tM%1$tS");
            commandSend.setSerialId(Integer.parseInt(hms));
            commandSend.setSendTime(now);
            commandSendJpa.save(commandSend);
        }

        return "success";
    }

    /**
     * 参数查询 0x80
     *
     * @param content
     * @return
     */
    private byte[] queryParam(String content) {
        String[] paramIds = content.split(",");
        int n = 0;
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < paramIds.length; i++) {
            int id = Integer.valueOf(paramIds[i]);
            if (0x05 == id || 0x0E == id) {
                buf.writeByte(id - 1);
                n++;
            }
            buf.writeByte(id);
            n++;
        }

        return combine(n, buf);
    }

    /**
     * 参数设置 0x81
     *
     * @param content
     * @return
     */
    private byte[] setParam(String content) throws Exception {
        Map map = JacksonUtil.toObject(content, HashMap.class);

        int i = 0;
        ByteBuf buf = Unpooled.buffer();
        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext(); ) {
            Object key = iterator.next();
            int id = Integer.parseInt(String.valueOf(key));
            String value = String.valueOf(map.get(key));
            byte[] bytes = CommonUtil.gb32960SetParam(id, value);
            buf.writeBytes(bytes);
            i++;
            if (0x05 == id || 0x0E == id) {
                i++;
            }
        }

        return combine(i, buf);
    }

    /**
     * 扩展协议查询
     *
     * @param content
     * @return
     */
    private byte[] queryExtra(String content) {


        return new byte[0];
    }

    /**
     * 扩展协议设置
     *
     * @param content
     * @return
     */
    private byte[] setExtra(String content) {
        // 0x80
        if (content.startsWith("128|")) {
            String[] strArr = content.split("\\|");
            int id = Integer.parseInt(strArr[0]);
            String params = strArr[1];

            ByteBuf buf = Unpooled.buffer(19);
            buf.writeByte(1);
            buf.writeByte(id);
            buf.writeBytes(params.getBytes());

            return buf.array();
        }

        return new byte[0];
    }

    /**
     * 第一个字节 添加数量
     *
     * @param count
     * @param buf
     * @return
     */
    private byte[] combine(int count, ByteBuf buf) {
        int length = buf.writerIndex();
        byte[] bytes = new byte[length + 1];
        bytes[0] = (byte) count;
        buf.getBytes(0, bytes, 1, length);

        return bytes;
    }
}
