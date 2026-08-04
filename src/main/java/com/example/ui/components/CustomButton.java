package com.example.ui.components;

import com.example.utils.ThemeUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 统一样式按钮 - 浅灰 + 墨绿主题
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
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor.darker(), 1),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)
        ));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);
        setPreferredSize(new Dimension(95, 32));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(backgroundColor.brighter());
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(backgroundColor);
                repaint();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(backgroundColor.darker());
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(backgroundColor.brighter());
                repaint();
            }
        });
    }

    public void setColor(Color color) {
        this.backgroundColor = color;
        setBackground(color);
    }
}