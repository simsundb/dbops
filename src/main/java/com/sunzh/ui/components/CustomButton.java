package com.sunzh.ui.components;

import com.sunzh.utils.ThemeUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 统一样式按钮 - 支持自定义颜色，统一尺寸与 hover/按下态
 * 通过 HSB 推导过渡色，替代手工 brighter/darker（避免发灰）
 */
public class CustomButton extends JButton {
    private Color backgroundColor;

    public CustomButton(String text, Color bgColor) {
        super(text);
        this.backgroundColor = bgColor;
        initStyle();
    }

    private void initStyle() {
        setFont(ThemeUtils.FONT_BOLD);
        setBackground(backgroundColor);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(
                ThemeUtils.BTN_PAD_Y, ThemeUtils.BTN_PAD_X, ThemeUtils.BTN_PAD_Y, ThemeUtils.BTN_PAD_X));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);
        setPreferredSize(new Dimension(0, ThemeUtils.BTN_HEIGHT));
        setFocusable(true);

        Color hover = deriveHover(backgroundColor);
        Color pressed = derivePressed(backgroundColor);
        putClientProperty("JButton.base", backgroundColor);
        putClientProperty("JButton.hover", hover);
        putClientProperty("JButton.pressed", pressed);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hover);
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(backgroundColor);
                repaint();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(pressed);
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(hover);
                repaint();
            }
        });
    }

    private static Color deriveHover(Color base) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        return Color.getHSBColor(hsb[0], Math.max(0f, hsb[1] - 0.08f), Math.min(1f, hsb[2] + 0.10f));
    }

    private static Color derivePressed(Color base) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        return Color.getHSBColor(hsb[0], Math.min(1f, hsb[1] + 0.06f), Math.max(0f, hsb[2] - 0.10f));
    }

    public void setColor(Color color) {
        this.backgroundColor = color;
        setBackground(color);
    }
}
