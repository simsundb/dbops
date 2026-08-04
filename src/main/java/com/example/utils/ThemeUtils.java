package com.example.utils;

import javax.swing.*;
import java.awt.*;

/**
 * 主题工具类 — 岩系冷调配色方案
 *
 * 设计理念：
 * - 以矿石/岩石的冷灰蓝色调为基调，营造"高智感"专业氛围
 * - 低饱和度 + 适度对比度，减少视觉疲劳
 * - 整体色调偏冷，传达沉稳、可靠的产品气质
 */
public class ThemeUtils {
    private ThemeUtils() {}

    // ═══════════════════════════════════════════════════════════════
    //  核心品牌色 — 岩蓝（Slate Blue）
    // ═══════════════════════════════════════════════════════════════
    /** 岩蓝主色 — 按钮、标题栏、选中态 */
    public static final Color COLOR_PRIMARY        = new Color(76,  110, 138);
    /** 岩蓝浅色 — hover / 进度条 */
    public static final Color COLOR_PRIMARY_LIGHT  = new Color(100, 136, 164);
    /** 岩蓝深色 — pressed / active */
    public static final Color COLOR_PRIMARY_DARK   = new Color(56,  82,  105);

    // ═══════════════════════════════════════════════════════════════
    //  辅助色
    // ═══════════════════════════════════════════════════════════════
    /** 暖灰辅助色 — 次要按钮、关闭按钮 */
    public static final Color COLOR_SECONDARY       = new Color(138, 145, 153);
    /** 暖灰辅助色浅 */
    public static final Color COLOR_SECONDARY_LIGHT = new Color(160, 168, 176);

    // ═══════════════════════════════════════════════════════════════
    //  中性背景色 — 岩灰调
    // ═══════════════════════════════════════════════════════════════
    /** 全局背景 — 冷调浅灰 */
    public static final Color COLOR_BG          = new Color(235, 238, 242);
    /** 卡片/面板背景 — 冷白 */
    public static final Color COLOR_BG_CARD     = new Color(248, 250, 253);
    /** 表格交替行 — 微冷调 */
    public static final Color COLOR_BG_ALTERNATE = new Color(242, 245, 249);
    /** 输入框背景 — 冷白 */
    public static final Color COLOR_BG_INPUT    = new Color(251, 252, 254);

    // ═══════════════════════════════════════════════════════════════
    //  边框和分隔线 — 冷灰
    // ═══════════════════════════════════════════════════════════════
    /** 默认边框 */
    public static final Color COLOR_BORDER       = new Color(200, 206, 213);
    /** 浅边框 — 分隔线、卡片边界 */
    public static final Color COLOR_BORDER_LIGHT = new Color(220, 225, 232);
    /** 分隔线 */
    public static final Color COLOR_DIVIDER      = new Color(210, 216, 224);

    // ═══════════════════════════════════════════════════════════════
    //  文字颜色 — 岩墨调
    // ═══════════════════════════════════════════════════════════════
    /** 主文字 — 深岩灰 */
    public static final Color COLOR_TEXT          = new Color(55,  65,  78);
    /** 次要文字 — 中灰 */
    public static final Color COLOR_TEXT_SECONDARY = new Color(130, 140, 150);
    /** 提示文字 — 浅灰 */
    public static final Color COLOR_TEXT_HINT     = new Color(175, 183, 192);
    /** 浅色底上的文字 — 白色 */
    public static final Color COLOR_TEXT_LIGHT    = new Color(250, 251, 253);

    // ═══════════════════════════════════════════════════════════════
    //  状态颜色
    // ═══════════════════════════════════════════════════════════════
    /** 成功 — 岩绿（青苔色调） */
    public static final Color COLOR_SUCCESS       = new Color(90,  150, 120);
    /** 成功浅 */
    public static final Color COLOR_SUCCESS_LIGHT = new Color(110, 170, 140);
    /** 警告 — 矿石琥珀 */
    public static final Color COLOR_WARNING       = new Color(195, 140, 85);
    /** 警告浅 */
    public static final Color COLOR_WARNING_LIGHT = new Color(215, 165, 110);
    /** 危险 — 矿石红 */
    public static final Color COLOR_DANGER        = new Color(190, 90,  85);
    /** 危险浅 */
    public static final Color COLOR_DANGER_LIGHT  = new Color(210, 115, 110);
    /** 信息 — 浅岩蓝 */
    public static final Color COLOR_INFO          = new Color(85,  135, 170);
    /** 信息浅 */
    public static final Color COLOR_INFO_LIGHT    = new Color(110, 160, 190);

    // ═══════════════════════════════════════════════════════════════
    //  标题栏和菜单
    // ═══════════════════════════════════════════════════════════════
    /** 菜单栏背景 — 冷白 */
    public static final Color COLOR_MENU_BG    = new Color(245, 247, 251);
    /** 菜单悬浮 — 极浅岩蓝 */
    public static final Color COLOR_MENU_HOVER = new Color(225, 232, 240);
    /** 顶部标题栏背景 — 岩蓝渐变起始色 */
    public static final Color COLOR_HEADER_BG  = new Color(60,  90,  115);
    /** 顶部标题栏文字 */
    public static final Color COLOR_HEADER_TEXT = new Color(238, 242, 248);

    // ═══════════════════════════════════════════════════════════════
    //  表格
    // ═══════════════════════════════════════════════════════════════
    /** 表头背景 — 深岩蓝灰 */
    public static final Color COLOR_TABLE_HEADER_BG   = new Color(72,  96,  118);
    /** 表头文字 */
    public static final Color COLOR_TABLE_HEADER_TEXT = new Color(240, 243, 248);
    /** 表格交替行 */
    public static final Color COLOR_TABLE_ROW_ALT     = COLOR_BG_ALTERNATE;

    // ═══════════════════════════════════════════════════════════════
    //  日志/控制台
    // ═══════════════════════════════════════════════════════════════
    /** 日志面板背景 — 深色终端风格 */
    public static final Color COLOR_LOG_BG      = new Color(35,  38,  48);
    /** 日志文字 — 冷白 */
    public static final Color COLOR_LOG_TEXT    = new Color(195, 205, 220);
    /** 日志信息 — 灰蓝 */
    public static final Color COLOR_LOG_INFO    = new Color(120, 170, 210);
    /** 日志成功 — 苔绿 */
    public static final Color COLOR_LOG_SUCCESS = new Color(120, 190, 145);
    /** 日志错误 — 矿红 */
    public static final Color COLOR_LOG_ERROR   = new Color(225, 110, 105);
    /** 日志警告 — 琥珀 */
    public static final Color COLOR_LOG_WARN    = new Color(215, 175, 105);

    // ═══════════════════════════════════════════════════════════════
    //  字体常量
    // ═══════════════════════════════════════════════════════════════
    public static final Font FONT_TITLE       = new Font("Microsoft YaHei", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE    = new Font("Microsoft YaHei", Font.BOLD, 14);
    public static final Font FONT_NORMAL      = new Font("Microsoft YaHei", Font.PLAIN, 13);
    public static final Font FONT_BOLD        = new Font("Microsoft YaHei", Font.BOLD, 13);
    public static final Font FONT_SMALL       = new Font("Microsoft YaHei", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD  = new Font("Microsoft YaHei", Font.BOLD, 12);
    public static final Font FONT_ICON        = new Font("Segoe UI", Font.PLAIN, 26);
    public static final Font FONT_LOG         = new Font("Consolas", Font.PLAIN, 12);

    // ═══════════════════════════════════════════════════════════════
    //  FlatLaf 全局主题配置
    // ═══════════════════════════════════════════════════════════════
    public static void applyFlatLafTheme() {
        // Button
        UIManager.put("Button.background", COLOR_PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.arc", 6);
        UIManager.put("Button.margin", new Insets(6, 16, 6, 16));
        UIManager.put("Button.hoverBackground", COLOR_PRIMARY_LIGHT);
        UIManager.put("Button.pressedBackground", COLOR_PRIMARY_DARK);

        // OptionPane（对话框按钮）
        UIManager.put("OptionPane.background", COLOR_BG_CARD);
        UIManager.put("OptionPane.messageForeground", COLOR_TEXT);
        UIManager.put("OptionPane.buttonBackground", COLOR_PRIMARY);
        UIManager.put("OptionPane.buttonForeground", Color.WHITE);
        UIManager.put("OptionPane.buttonHoverBackground", COLOR_PRIMARY_LIGHT);

        // TabbedPane
        UIManager.put("TabbedPane.background", COLOR_BG);
        UIManager.put("TabbedPane.selectedBackground", COLOR_BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", COLOR_PRIMARY);
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));

        // Table
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.alternateRowColor", COLOR_BG_ALTERNATE);
        UIManager.put("Table.rowHeight", 26);
        UIManager.put("Table.selectionBackground", new Color(180, 200, 220));
        UIManager.put("Table.selectionForeground", COLOR_TEXT);
        UIManager.put("TableHeader.background", COLOR_TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", COLOR_TABLE_HEADER_TEXT);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);

        // TextField
        UIManager.put("TextField.background", COLOR_BG_INPUT);
        UIManager.put("TextField.borderColor", COLOR_BORDER);
        UIManager.put("TextField.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));

        // ComboBox
        UIManager.put("ComboBox.background", COLOR_BG_INPUT);
        UIManager.put("ComboBox.borderColor", COLOR_BORDER);
        UIManager.put("ComboBox.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("ComboBox.buttonBackground", COLOR_PRIMARY);
        UIManager.put("ComboBox.buttonArrowColor", Color.WHITE);

        // PasswordField
        UIManager.put("PasswordField.background", COLOR_BG_INPUT);
        UIManager.put("PasswordField.borderColor", COLOR_BORDER);
        UIManager.put("PasswordField.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));

        // ScrollPane
        UIManager.put("ScrollPane.background", COLOR_BG_CARD);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(COLOR_BORDER_LIGHT, 1));

        // Panel
        UIManager.put("Panel.background", COLOR_BG);

        // List
        UIManager.put("List.background", COLOR_BG_CARD);
        UIManager.put("List.selectionBackground", new Color(200, 215, 230));
        UIManager.put("List.selectionForeground", COLOR_TEXT);

        // ProgressBar
        UIManager.put("ProgressBar.background", COLOR_BORDER_LIGHT);
        UIManager.put("ProgressBar.foreground", COLOR_PRIMARY_LIGHT);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);

        // TitledBorder
        UIManager.put("TitledBorder.titleColor", COLOR_PRIMARY);

        // ToolTip
        UIManager.put("ToolTip.background", COLOR_HEADER_BG);
        UIManager.put("ToolTip.foreground", COLOR_HEADER_TEXT);

        // SplitPane
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.background", COLOR_BORDER_LIGHT);

        // 额外
        UIManager.put("Component.borderColor", COLOR_BORDER);
        UIManager.put("TextField.selectionBackground", COLOR_PRIMARY_LIGHT);
        UIManager.put("TextField.selectionForeground", Color.WHITE);

        // Menu
        UIManager.put("Menu.background", COLOR_MENU_BG);
        UIManager.put("Menu.selectionBackground", COLOR_MENU_HOVER);
        UIManager.put("Menu.selectionForeground", COLOR_TEXT);
        UIManager.put("MenuBar.background", COLOR_MENU_BG);
    }
}
