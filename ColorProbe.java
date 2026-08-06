import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorProbe {
    public static void main(String[] args) {
        // 打印 ThemeUtils 关键颜色，验证新主题生效
        System.out.println("COLOR_PRIMARY  = " + toHex(ThemeUtils.COLOR_PRIMARY) + "  (期待 1890ff 科技蓝)");
        System.out.println("COLOR_BG       = " + toHex(ThemeUtils.COLOR_BG) + "  (期待 f5f7fa 浅灰)");
        System.out.println("COLOR_BG_CARD  = " + toHex(ThemeUtils.COLOR_BG_CARD) + "  (期待 ffffff 纯白)");
        System.out.println("COLOR_HEADER_BG= " + toHex(ThemeUtils.COLOR_HEADER_BG) + "  (期待 1c3454 深蓝)");
        System.out.println("COLOR_DANGER   = " + toHex(ThemeUtils.COLOR_DANGER) + "  (期待 dc4040 红)");
        System.out.println("COLOR_SUCCESS  = " + toHex(ThemeUtils.COLOR_SUCCESS) + "  (期待 48b362 绿)");
        System.out.println("COLOR_WARNING  = " + toHex(ThemeUtils.COLOR_WARNING) + "  (期待 f5a623 橙)");
        System.out.println("FONT_NORMAL    = " + ThemeUtils.FONT_NORMAL.getFontName() + " " + ThemeUtils.FONT_NORMAL.getSize() + "px");
        System.out.println("FONT_TITLE     = " + ThemeUtils.FONT_TITLE.getFontName() + " " + ThemeUtils.FONT_TITLE.getSize() + "px");

        // 采样主窗口预览图的标题栏和背景像素
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new java.io.File("/tmp/db_mainframe_preview.png"));
            System.out.println("\n预览图像素采样 (1200x750):");
            // 顶部标题栏区域 (y=30) 应是深蓝
            Color header = new Color(img.getRGB(600, 30));
            System.out.println("  标题栏中心 (600,30) = " + toHex(header));
            // 内容区背景 (y=400) 应是浅灰/白
            Color content = new Color(img.getRGB(600, 400));
            System.out.println("  内容区中心 (600,400) = " + toHex(content));
            // 卡片区域 (y=300)
            Color card = new Color(img.getRGB(600, 250));
            System.out.println("  卡片区 (600,250)    = " + toHex(card));
        } catch (Exception e) {
            System.out.println("采样失败: " + e.getMessage());
        }
        System.exit(0);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
