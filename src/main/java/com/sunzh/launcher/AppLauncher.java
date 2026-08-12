package com.sunzh.launcher;

import com.sunzh.ui.MainFrame;
import com.sunzh.utils.ExternalConfigUtils;
import com.sunzh.utils.ThemeUtils;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class AppLauncher {

    public static void main(String[] args) {
        // 0. 先把 JAR 内全部默认配置导出到 conf/（已存在的文件不覆盖，用户自定义优先）
        //    必须在任何读配置之前执行，保证首次运行即自包含。
        ExternalConfigUtils.exportBundledDefaults();

        try {
            // 1. 先应用 FlatLaf 外观
            UIManager.setLookAndFeel(new FlatLightLaf());

            // 2. 然后应用自定义主题配置
            ThemeUtils.applyFlatLafTheme();

            // 3. 额外 UI 调整
            UIManager.put("TabbedPane.tabInsets", new Insets(10, 18, 10, 18));
            UIManager.put("Table.showGrid", false);
            UIManager.put("defaultFont", ThemeUtils.FONT_NORMAL);
            UIManager.put("Component.focusWidth", 0);
            UIManager.put("Component.arrowType", "triangle");

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