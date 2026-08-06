package com.sunzh.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 中国传统美术风格按钮
 * 融合水墨、工笔、青绿山水元素
 * 风格：雅致、简约、含蓄
 */
public class CustomButton extends JButton {

    // 中国传统色彩
    private static final Color COLOR_VERMILION = new Color(200, 60, 50);      // 朱砂红
    private static final Color COLOR_CINNABAR = new Color(180, 40, 40);        // 朱红
    private static final Color COLOR_JADE = new Color(60, 130, 100);           // 翡翠绿
    private static final Color COLOR_MOUNTAIN = new Color(70, 110, 130);       // 山青色
    private static final Color COLOR_INK = new Color(50, 50, 55);              // 墨色
    private static final Color COLOR_PAPER = new Color(245, 240, 230);         // 宣纸色
    private static final Color COLOR_SCROLL = new Color(220, 210, 190);        // 绢本
    private static final Color COLOR_GOLD = new Color(180, 150, 80);           // 金色
    private static final Color COLOR_CLOUD = new Color(235, 230, 220);         // 云白

    private Color primaryColor;
    private Color hoverColor;
    private Color pressColor;
    private boolean isTraditional = true;
    private int cornerRadius = 6;

    // ==================== 构造方法 ====================

    /**
     * 构造传统风格按钮
     */
    public CustomButton(String text, Color bgColor) {
        super(text);
        this.primaryColor = bgColor;
        initColors();
        initStyle();
    }

    /**
     * 构造传统风格按钮（带图标）
     */
    public CustomButton(String text, Icon icon, Color bgColor) {
        super(text, icon);
        this.primaryColor = bgColor;
        initColors();
        initStyle();
    }

    /**
     * 构造传统风格按钮（无背景色，使用墨色）
     */
    public CustomButton(String text) {
        super(text);
        this.primaryColor = COLOR_INK;
        initColors();
        initStyle();
    }

    // ==================== 颜色初始化 ====================

    private void initColors() {
        // 根据主色自动生成配套颜色
        float[] hsb = Color.RGBtoHSB(
                primaryColor.getRed(),
                primaryColor.getGreen(),
                primaryColor.getBlue(),
                null
        );

        // 悬停：变亮10%
        hoverColor = new Color(
                Math.min(255, (int)(primaryColor.getRed() * 1.15)),
                Math.min(255, (int)(primaryColor.getGreen() * 1.15)),
                Math.min(255, (int)(primaryColor.getBlue() * 1.15))
        );

        // 按下：变暗15%
        pressColor = new Color(
                (int)(primaryColor.getRed() * 0.85),
                (int)(primaryColor.getGreen() * 0.85),
                (int)(primaryColor.getBlue() * 0.85)
        );
    }

    // ==================== 样式初始化 ====================

    private void initStyle() {
        setFont(new Font("华文楷体", Font.PLAIN, 15));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(8, 20, 8, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        // 设置默认尺寸
        if (getPreferredSize() == null || getPreferredSize().width < 80) {
            setPreferredSize(new Dimension(95, 36));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                repaint();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                repaint();
            }
        });
    }

    // ==================== 绘制方法（传统风格） ====================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        boolean isHover = getModel().isRollover();
        boolean isPress = getModel().isPressed();

        // ----- 1. 绘制背景（宣纸质感） -----
        Color bgColor = isPress ? pressColor : (isHover ? hoverColor : primaryColor);

        // 模拟宣纸纹理 - 轻微渐变
        GradientPaint gp = new GradientPaint(
                0, 0, bgColor,
                w, h, new Color(
                Math.min(255, bgColor.getRed() + 20),
                Math.min(255, bgColor.getGreen() + 20),
                Math.min(255, bgColor.getBlue() + 20)
        )
        );
        g2d.setPaint(gp);

        // 圆角矩形，模拟毛笔笔触
        Shape rect = new RoundRectangle2D.Double(0, 0, w, h, cornerRadius, cornerRadius);
        g2d.fill(rect);

        // ----- 2. 绘制边框（水墨勾边） -----
        g2d.setStroke(new BasicStroke(1.2f));
        Color borderColor = isHover ?
                primaryColor.darker() :
                new Color(
                        Math.max(0, bgColor.getRed() - 30),
                        Math.max(0, bgColor.getGreen() - 30),
                        Math.max(0, bgColor.getBlue() - 30)
                );
        g2d.setColor(borderColor);
        g2d.draw(rect);

        // ----- 3. 绘制角落装饰（传统纹样） -----
        if (isHover || isPress) {
            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.setStroke(new BasicStroke(1.0f));
            int size = 12;
            // 左上角
            g2d.drawLine(6, 6, 6 + size, 6);
            g2d.drawLine(6, 6, 6, 6 + size);
            // 右下角
            g2d.drawLine(w - 6, h - 6, w - 6 - size, h - 6);
            g2d.drawLine(w - 6, h - 6, w - 6, h - 6 - size);
        }

        // ----- 4. 绘制文字（毛笔字效果） -----
        g2d.setColor(Color.WHITE);
        g2d.setFont(getFont());

        FontMetrics fm = g2d.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int iconWidth = 0;

        // 如果有图标，计算图标位置
        Icon icon = getIcon();
        if (icon != null) {
            iconWidth = icon.getIconWidth() + 6;
        }

        // 文字居中
        int x = (w - textWidth - iconWidth) / 2 + iconWidth;
        int y = (h - textHeight) / 2 + fm.getAscent();

        // 绘制文字阴影（增加立体感）
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawString(text, x + 1, y + 1);

        // 绘制主体文字
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);

        // 绘制图标（在文字左侧）
        if (icon != null) {
            int iconX = (w - textWidth - iconWidth) / 2;
            int iconY = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2d, iconX, iconY);
        }

        g2d.dispose();
    }

    // ==================== Setter 方法 ====================

    /**
     * 设置主色
     */
    public void setPrimaryColor(Color color) {
        this.primaryColor = color;
        initColors();
        repaint();
    }

    /**
     * 设置圆角大小
     */
    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    /**
     * 设置是否使用传统风格
     */
    public void setTraditional(boolean traditional) {
        this.isTraditional = traditional;
        repaint();
    }

    // ==================== 工厂方法（快速创建传统风格按钮） ====================

    /**
     * 创建朱砂红按钮（主要操作）
     */
    public static CustomButton createPrimaryButton(String text) {
        return new CustomButton(text, COLOR_VERMILION);
    }

    /**
     * 创建朱砂红按钮（带图标）
     */
    public static CustomButton createPrimaryButton(String text, Icon icon) {
        return new CustomButton(text, icon, COLOR_VERMILION);
    }

    /**
     * 创建翡翠绿按钮（成功/确认）
     */
    public static CustomButton createSuccessButton(String text) {
        return new CustomButton(text, COLOR_JADE);
    }

    /**
     * 创建墨色按钮（次要操作）
     */
    public static CustomButton createSecondaryButton(String text) {
        return new CustomButton(text, COLOR_INK);
    }

    /**
     * 创建金色按钮（特殊/重要）
     */
    public static CustomButton createGoldButton(String text) {
        return new CustomButton(text, COLOR_GOLD);
    }

    /**
     * 创建宣纸风格按钮（轻量操作）
     */
    public static CustomButton createPaperButton(String text) {
        CustomButton btn = new CustomButton(text, COLOR_PAPER);
        btn.setForeground(COLOR_INK);
        btn.primaryColor = COLOR_PAPER;
        btn.initColors();
        return btn;
    }

    /**
     * 创建仅文字按钮（无背景）
     */
    public static CustomButton createTextButton(String text) {
        CustomButton btn = new CustomButton(text);
        btn.setForeground(COLOR_INK);
        btn.primaryColor = new Color(0, 0, 0, 0);
        btn.setContentAreaFilled(false);
        return btn;
    }
}