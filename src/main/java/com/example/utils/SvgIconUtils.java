package com.example.utils;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * SVG 图标工具类 — 从 resources/icons/ 加载 SVG 文件生成 Swing ImageIcon。
 *
 * 图标来源：Tabler Icons (MIT License, https://tabler.io/icons)
 * 下载位置：src/main/resources/icons/
 *
 * 使用方式：
 *   // 通用：16px，品牌色
 *   SvgIconUtils.get("database", 16)
 *   // 自定义尺寸和颜色
 *   SvgIconUtils.get("search", 24, Color.RED)
 *   // 纯色按钮上的白色图标
 *   SvgIconUtils.getWhite("play", 20)
 *   // 生成带图标和文字按钮
 *   SvgIconUtils.button("sync", "同步", ThemeUtils.COLOR_PRIMARY)
 *
 * 添加新图标：
 *   1. 下载 SVG 到 src/main/resources/icons/ 目录
 *   2. 调用时传文件名（不含 .svg）
 *   3. 无需注册，自动发现
 */
public class SvgIconUtils {

    private static final String ICON_DIR = "/icons/";
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    private SvgIconUtils() {}

    // ================================================================
    //  核心方法
    // ================================================================

    /**
     * 获取图标的 ImageIcon。
     *
     * @param name 文件名（不含 .svg），如 "database"
     * @param size 图标尺寸（宽高相同），如 16 / 20 / 24 / 32
     * @param tint 着色颜色（null 保留 SVG 原始颜色）
     */
    public static ImageIcon get(String name, int size, Color tint) {
        String cacheKey = name + "@" + size + "@" + (tint == null ? "none" : Integer.toHexString(tint.getRGB()));
        BufferedImage cached = cache.get(cacheKey);
        if (cached != null) {
            return new ImageIcon(cached);
        }

        try {
            String path = ICON_DIR + name + ".svg";
            InputStream is = SvgIconUtils.class.getResourceAsStream(path);
            if (is == null) {
                System.err.println("[SvgIconUtils] 未找到图标: " + path);
                return new ImageIcon();
            }

            BufferedImage[] holder = new BufferedImage[1];
            ImageTranscoder transcoder = new ImageTranscoder() {
                @Override
                public BufferedImage createImage(int w, int h) {
                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    holder[0] = bi;
                    return bi;
                }
                @Override
                public void writeImage(BufferedImage img, TranscoderOutput out) {
                    // 不需要额外输出
                }
            };
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) size);
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float) size);
            transcoder.transcode(new TranscoderInput(is), null);

            BufferedImage raw = holder[0];
            if (raw == null) return new ImageIcon();

            BufferedImage result;
            if (tint != null) {
                result = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result.createGraphics();
                g.drawImage(raw, 0, 0, null);
                g.setComposite(AlphaComposite.SrcAtop);
                g.setColor(tint);
                g.fillRect(0, 0, result.getWidth(), result.getHeight());
                g.dispose();
            } else {
                result = raw;
            }
            cache.put(cacheKey, result);
            return new ImageIcon(result);
        } catch (Exception e) {
            e.printStackTrace();
            return new ImageIcon();
        }
    }

    // ================================================================
    //  快捷方法（默认使用 ThemeUtils 品牌色）
    // ================================================================

    /** 16px，主题色 PRIMARY：用于按钮、标签 */
    public static ImageIcon get(String name) {
        return get(name, 16, ThemeUtils.COLOR_PRIMARY);
    }

    /** 指定尺寸，主题色 PRIMARY */
    public static ImageIcon get(String name, int size) {
        return get(name, size, ThemeUtils.COLOR_PRIMARY);
    }

    /** 白色图标：用于有色背景按钮上的图标 */
    public static ImageIcon getWhite(String name, int size) {
        return get(name, size, Color.WHITE);
    }

    /** 灰色图标：用于次要/禁用状态 */
    public static ImageIcon getGray(String name, int size) {
        return get(name, size, new Color(150, 160, 175));
    }

    /** 危险色（红色）图标：用于删除、错误 */
    public static ImageIcon getDanger(String name, int size) {
        return get(name, size, ThemeUtils.COLOR_DANGER);
    }

    /** 成功色（绿色）图标：用于成功状态 */
    public static ImageIcon getSuccess(String name, int size) {
        return get(name, size, ThemeUtils.COLOR_SUCCESS);
    }

    // ================================================================
    //  按钮工厂 — 生成带图标和文字的 JButton
    // ================================================================

    /**
     * 创建一个包含 SVG 图标和文字的按钮。
     * 图标为白色 16px，按钮使用指定背景色。
     *
     * @param iconName SVG 文件名（不含 .svg）
     * @param text     按钮文字
     * @param bgColor  按钮背景色
     */
    public static JButton button(String iconName, String text, Color bgColor) {
        ImageIcon icon = get(iconName, 16, Color.WHITE);
        JButton btn = new JButton(text, icon);
        btn.setFont(ThemeUtils.FONT_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.setIconTextGap(6);
        return btn;
    }

    /**
     * 创建一个带图标的 Outline 风格按钮（白色底 + 彩色边框+文字）。
     *
     * @param iconName SVG 文件名
     * @param text     按钮文字
     * @param color    边框和文字颜色
     */
    public static JButton outlineButton(String iconName, String text, Color color) {
        ImageIcon icon = get(iconName, 16, color);
        JButton btn = new JButton(text, icon);
        btn.setFont(ThemeUtils.FONT_BOLD);
        btn.setForeground(color);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btn.setIconTextGap(6);
        return btn;
    }

    // ================================================================
    //  Label 工厂 — 生成带图标的 JLabel
    // ================================================================

    /** 创建一个带图标的标签（图标 16px，主题色） */
    public static JLabel label(String iconName, String text) {
        JLabel label = new JLabel(text, get(iconName, 16), SwingConstants.LEADING);
        label.setIconTextGap(6);
        return label;
    }

    /** 创建一个带图标的标签，指定图标的尺寸和颜色，文字使用默认字体 */
    public static JLabel label(String iconName, String text, int iconSize, Color color) {
        JLabel label = new JLabel(text, get(iconName, iconSize, color), SwingConstants.LEADING);
        label.setIconTextGap(6);
        return label;
    }

    /** 创建一个带图标的标签，指定图标尺寸、颜色和文字字体（仅设置图标大小） */
    public static JLabel labelWithFont(String iconName, String text, Font font, Color color) {
        int size = font.getSize();
        JLabel label = new JLabel(text, get(iconName, size, color), SwingConstants.LEADING);
        label.setIconTextGap(6);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** 创建一个只有图标的 JLabel */
    public static JLabel iconLabel(String iconName, int size, Color color) {
        return new JLabel(get(iconName, size, color));
    }

    // ================================================================
    //  缓存管理
    // ================================================================

    /** 清空图标缓存（一般在主题切换后调用） */
    public static void clearCache() {
        cache.clear();
    }
}
