package com.example.launcher;

import com.example.ui.MainFrame;
import com.example.utils.ThemeUtils;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class AppLauncher {

    public static void main(String[] args) {
        try {
            // 1. 先应用 FlatLaf 外观
            UIManager.setLookAndFeel(new FlatLightLaf());

            // 2. 然后应用自定义主题配置
            ThemeUtils.applyFlatLafTheme();

            // 3. 额外 UI 调整
            UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));
            UIManager.put("Table.showGrid", false);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}