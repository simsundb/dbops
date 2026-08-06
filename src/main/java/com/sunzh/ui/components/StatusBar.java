package com.sunzh.ui.components;

import com.sunzh.utils.SvgIconUtils;
import javax.swing.*;
import java.awt.*;

/**
 * 底部状态栏
 * 显示就绪状态、数据源数量、内存占用等信息
 */
public class StatusBar extends JPanel {
    private JLabel leftLabel;
    private JLabel rightLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 244, 250));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(195, 205, 218)),
                BorderFactory.createEmptyBorder(4, 18, 4, 18)
        ));

        leftLabel = new JLabel("就绪");
        leftLabel.setIcon(SvgIconUtils.get("check", 12, new Color(90, 150, 120)));
        leftLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        leftLabel.setForeground(new Color(100, 110, 125));
        add(leftLabel, BorderLayout.WEST);

        rightLabel = new JLabel(Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
        rightLabel.setIcon(SvgIconUtils.get("hard-drive", 12, new Color(140, 150, 165)));
        rightLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        rightLabel.setForeground(new Color(140, 150, 165));
        add(rightLabel, BorderLayout.EAST);
    }

    public void setLeftText(String text) {
        leftLabel.setText(text);
    }

    public void setRightText(String text) {
        rightLabel.setText(text);
    }

    public void updateMemoryInfo() {
        rightLabel.setText(Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
    }
}