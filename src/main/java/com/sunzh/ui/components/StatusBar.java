package com.sunzh.ui.components;

import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;
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
        setBackground(ThemeUtils.COLOR_BG_CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(5, 18, 5, 18)
        ));

        leftLabel = new JLabel("就绪");
        leftLabel.setIcon(SvgIconUtils.get("check", 12, ThemeUtils.COLOR_SUCCESS));
        leftLabel.setFont(ThemeUtils.FONT_SMALL);
        leftLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        add(leftLabel, BorderLayout.WEST);

        rightLabel = new JLabel(Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
        rightLabel.setIcon(SvgIconUtils.get("hard-drive", 12, ThemeUtils.COLOR_TEXT_HINT));
        rightLabel.setFont(ThemeUtils.FONT_SMALL);
        rightLabel.setForeground(ThemeUtils.COLOR_TEXT_HINT);
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
