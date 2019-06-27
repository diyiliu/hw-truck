import cn.com.tiza.tstar.common.entity.TStarData;
import cn.com.tiza.tstar.gateway.handler.BaseUserDefinedHandler;
import com.tiza.plugin.util.CommonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.util.Date;

/**
 * Description: TruckHandler
 * Author: DIYILIU
 * Update: 2019-06-19 14:48
 */
public class TruckHandler  extends BaseUserDefinedHandler {

    /**
     * 0: 平台转发数据; 1: 车载终端数据
     **/
    private final static int ORIGIN_FROM = 1;

    public TStarData handleRecvMessage(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
        byte[] msgBody = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(0, msgBody);

        // 协议头
        byteBuf.readShort();
        // 命令标识
        int cmd = byteBuf.readUnsignedByte();
        // 应答标识
        int resp = byteBuf.readUnsignedByte();

        // VIN码
        byte[] vinArray = new byte[17];
        byteBuf.readBytes(vinArray);
        String vin = new String(vinArray);

        // 加密方式
        byteBuf.readByte();
        // 数据单元长度
        int length = byteBuf.readUnsignedShort();
        // 指令内容
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

        TStarData tStarData = new TStarData();
        tStarData.setMsgBody(msgBody);
        tStarData.setCmdID(cmd);
        tStarData.setTerminalID(vin);
        tStarData.setTime(System.currentTimeMillis());

        // 需要应答
        if (resp == 0xFE) {
            doResponse(channelHandlerContext, vin, cmd, bytes);
        }

        return tStarData;
    }

    /**
     * 指令应答
     *
     * @param ctx
     * @param terminal
     * @param cmd
     * @param bytes
     */
    private void doResponse(ChannelHandlerContext ctx, String terminal, int cmd, byte[] bytes) {
        byte[] respArr = new byte[0];

        // 车载终端数据
        if (1 == ORIGIN_FROM){
            respArr = bytes;
            // 校时
            if(0x08 == cmd){
                respArr = CommonUtil.dateToBytes(new Date());
            }
        }else {
            if (bytes.length > 5){
                byte[] dateArray = new byte[6];
                System.arraycopy(bytes, 0, dateArray, 0, 6);
                respArr = dateArray;
            }
        }

        // 应答内容
        byte[] content = packResp(terminal, cmd, respArr);

        TStarData respData = new TStarData();
        respData.setTerminalID(terminal);
        respData.setCmdID(cmd);
        respData.setMsgBody(content);
        respData.setTime(System.currentTimeMillis());
        ctx.channel().writeAndFlush(respData);
    }

    /**
     * 生成应答数据
     *
     * @param terminal
     * @param cmd
     * @param bytes
     * @return
     */
    private byte[] packResp(String terminal, int cmd, byte[] bytes) {
        int length = bytes.length;
        ByteBuf buf = Unpooled.buffer(25 + length);
        buf.writeByte(0x23);
        buf.writeByte(0x23);
        buf.writeByte(cmd);
        buf.writeByte(0x01);
        // VIN
        buf.writeBytes(terminal.getBytes());
        // 不加密
        buf.writeByte(0x01);
        buf.writeShort(length);
        // 返回数据
        buf.writeBytes(bytes);

        // 获取校验位
        byte[] content = new byte[22 + length];
        buf.getBytes(2, content);
        int check = CommonUtil.getCheck(content);
        buf.writeByte(check);

        return buf.array();
    }
}
