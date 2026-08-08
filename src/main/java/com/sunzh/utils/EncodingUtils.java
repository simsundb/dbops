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

/**
 * 文本编码工具：自动识别文件编码（UTF-8 → GB18030 回退），避免中文乱码。
 *
 * <p>识别策略：优先按 UTF-8 严格解码；若字节序列不是合法 UTF-8（多为 GBK/GB2312 编码的中文），
 * 回退到 GB18030（GBK 的超集）解码；仍失败则以 UTF-8 兜底。自动跳过 UTF-8 BOM。</p>
 */
public class EncodingUtils {

    /** GB18030 覆盖 GBK/GB2312 全部中文编码，解码不会丢字 */
    private static final Charset GB18030;

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
     * 自动识别编码并解码字节数组。
     *
     * @param bytes 原始字节
     * @return 解码后的字符串
     */
    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        int offset = hasUtf8Bom(bytes) ? 3 : 0;

        // 1. 优先 UTF-8 严格解码
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return utf8Decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
        } catch (CharacterCodingException e) {
            // 非合法 UTF-8，落入下一步
        }

        // 2. GB18030（覆盖 GBK/GB2312）
        try {
            return new String(bytes, offset, bytes.length - offset, GB18030);
        } catch (Exception e) {
            // 3. 兜底
            return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        }
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }
}
