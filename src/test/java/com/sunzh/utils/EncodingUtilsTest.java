package com.sunzh.utils;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * EncodingUtils 自动编码识别测试：
 * 覆盖合法 UTF-8、UTF-8 BOM、GBK/GB18030，以及"GBK 字节凑成合法 UTF-8"的滑穿场景。
 */
public class EncodingUtilsTest {

    private static final String SAMPLE = "数据库巡检SQL预览中文乱码检查";

    @Test
    public void decodesUtf8() {
        String s = EncodingUtils.decode(SAMPLE.getBytes(StandardCharsets.UTF_8));
        assertEquals(SAMPLE, s);
    }

    @Test
    public void decodesUtf8WithBom() {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = SAMPLE.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(body, 0, withBom, bom.length, body.length);
        assertEquals(SAMPLE, EncodingUtils.decode(withBom));
    }

    @Test
    public void decodesGbk() {
        String s = EncodingUtils.decode(SAMPLE.getBytes(Charset.forName("GBK")));
        assertEquals(SAMPLE, s);
    }

    @Test
    public void decodesGbkThatSlipsThroughStrictUtf8() {
        // 这些字符的 GBK 字节首字节在 0xC0~0xDF，能凑成"合法 UTF-8"，UTF-8 优先策略会解出乱码
        String gbkChars = "拜您保候仙依位件佑体何余作你使侑侔侗侃儿兔充兆先光克免兑兕兖";
        String decoded = EncodingUtils.decode(gbkChars.getBytes(Charset.forName("GBK")));
        assertEquals(gbkChars, decoded);
    }

    @Test
    public void decodesAscii() {
        String ascii = "SELECT * FROM dual;";
        assertEquals(ascii, EncodingUtils.decode(ascii.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void decodesEmpty() {
        assertEquals("", EncodingUtils.decode(new byte[0]));
        assertEquals("", EncodingUtils.decode(null));
    }
}
