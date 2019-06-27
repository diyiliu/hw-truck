import cn.com.tiza.tstar.gateway.codec.CustomDecoder;
import com.tiza.plugin.util.CommonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Description: TruckDecoder
 * Author: DIYILIU
 * Update: 2019-06-19 14:46
 */

@Slf4j
public class TruckDecoder extends CustomDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (buf.readableBytes() < 25) {
            return;
        }

        buf.markReaderIndex();
        byte header1 = buf.readByte();
        byte header2 = buf.readByte();

        // 头标识
        if (header1 != 0x23 || header2 != 0x23) {
            String host = ctx.channel().remoteAddress().toString().trim().replaceFirst("/", "");
            log.error("协议头校验失败, 断开连接[{}]!", host);
            ctx.close();
            return;
        }

        buf.readBytes(new byte[20]);
        // 数据单元长度
        int length = buf.readUnsignedShort();
        if (buf.readableBytes() < length + 1) {

            buf.resetReaderIndex();
            return;
        }
        buf.resetReaderIndex();

        byte[] bytes = new byte[2 + 20 + 2 + length + 1];
        buf.readBytes(bytes);

        // 验证校验位
        if (!check(bytes)) {
            // 关闭连接
            log.error("校验位错误, 断开连接!");
            ctx.close();
            return;
        }

        out.add(Unpooled.copiedBuffer(bytes));
    }

    /**
     * 验证校验位
     *
     * @param bytes
     * @return
     */
    private boolean check(byte[] bytes) {
        ByteBuf buf = Unpooled.buffer(bytes.length);
        buf.writeBytes(bytes);

        byte[] content = new byte[bytes.length - 1];
        buf.readBytes(content);

        byte last = buf.readByte();
        byte check = CommonUtil.getCheck(content);

        if (last == check) {

            return true;
        }
        log.error("校验位错误, [实际值{}, 计算值{}, 原始指令[{}]]!", last, check, CommonUtil.bytesToStr(bytes));

        return false;
    }
}
