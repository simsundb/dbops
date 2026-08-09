package com.sunzh.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本编码工具：自动识别文件编码（UTF-8 / GB18030 择优），避免中文乱码。
 *
 * <p>识别策略：同时用 UTF-8（严格）与 GB18030（宽松，GBK/GB2312 的超集）解码，
 * 选择"可疑乱码字符"更少的结果；合法 UTF-8 直接通过，GBK/GB2312 文件回退到 GB18030。
 * 相较简单的"UTF-8 优先"更稳健——某些 GBK 双字节（如首字节 0xC0~0xDF）能凑成合法
 * UTF-8 序列而侥幸通过严格解码，导致解出乱码，本方案会因该结果乱码字符较多而选择 GB18030。
 * 自动跳过 UTF-8 BOM。</p>
 */
public class EncodingUtils {

    /** GB18030 覆盖 GBK/GB2312 全部中文编码，解码不会丢字 */
    private static final Charset GB18030;

    /**
     * 可疑乱码字符类别：
     * 替换符、谚文/朝鲜文（GBK 误当 UTF-8 的典型产物）、泰文/缅文、
     * 组合附加符号、私用区。
     */
    private static final Pattern SUSPICIOUS = Pattern.compile(
            "[\\uFFFD\\uAC00-\\uD7A3\\u0E00-\\u0E7F\\u1000-\\u109F\\u0300-\\u036F\\uE000-\\uF8FF]");

    static {
        Charset gbk;
        try {
            gbk = Charset.forName("GB18030");
        } catch (Exception e) {
            gbk = StandardCharsets.UTF_8; // 极端环境兜底
        }
        GB18030 = gbk;
    }

    private EncodingUtils() {
    }

    /**
     * 读取文件内容并自动识别编码。
     *
     * @param file 文本文件（SQL / YAML / 配置文件等）
     * @return 解码后的字符串
     * @throws IOException 文件读取失败
     */
    public static String readText(File file) throws IOException {
        return decode(Files.readAllBytes(file.toPath()));
    }

    /**
     * 读取输入流内容并自动识别编码（不关闭流）。
     *
     * @param in 输入流
     * @return 解码后的字符串
     * @throws IOException 读取失败
     */
    public static String readText(InputStream in) throws IOException {
        return decode(in.readAllBytes());
    }

    /**
     * 自动识别编码并解码字节数组：UTF-8 与 GB18030 择优（乱码字符更少者胜出）。
     *
     * @param bytes 原始字节
     * @return 解码后的字符串
     */
    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        // 0. UTF-16 BOM 优先处理（Windows 部分编辑器常见）
        if (hasUtf16LeBom(bytes)) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (hasUtf16BeBom(bytes)) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }

        int offset = hasUtf8Bom(bytes) ? 3 : 0;
        byte[] body = Arrays.copyOfRange(bytes, offset, bytes.length);

        // 1. UTF-8 严格解码（字节序列不合法时返回 null）
        String utf8 = tryStrictUtf8(body);

        // 2. GB18030 宽松解码（覆盖 GBK/GB2312，几乎不会失败）
        String gb = new String(body, GB18030);

        // 3. 择优：选"可疑乱码字符"更少的那个；平局偏好合法 UTF-8
        if (utf8 == null) {
            return gb;
        }
        int suspUtf8 = countSuspicious(utf8);
        int suspGb = countSuspicious(gb);
        return suspGb < suspUtf8 ? gb : utf8;
    }

    /** 严格 UTF-8 解码；字节序列不合法时返回 null（交由上层回退） */
    private static String tryStrictUtf8(byte[] body) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(body)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /** 统计字符串中"乱码产物"类字符的数量，用于在两种解码结果间择优 */
    private static int countSuspicious(String s) {
        int count = 0;
        Matcher m = SUSPICIOUS.matcher(s);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }

    private static boolean hasUtf16LeBom(byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE;
    }

    private static boolean hasUtf16BeBom(byte[] bytes) {
        return bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFE
                && (bytes[1] & 0xFF) == 0xFF;
    }
}
