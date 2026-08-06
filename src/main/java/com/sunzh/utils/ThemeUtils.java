package com.sunzh.utils;

import javax.swing.*;
import java.awt.*;

/**
 * 主题工具类 — 现代运维工具风格
 *
 * 设计理念：
 * - 参考 Navicat / DataGrip / DBeaver / Ant Design 等主流数据库运维工具
 * - 科技蓝主色 + 浅灰背景 + 白色卡片，清爽、克制、高对比
 * - 无衬线字体（微软雅黑），贴近现代桌面软件
 *
 * 配色来源：
 * - 主色：#1890ff 科技蓝（Ant Design）
 * - 背景：#f5f7fa 浅灰 / 卡片 #ffffff 纯白
 * - 文字：深灰 #1f2329 / 次级 #646a73 / 弱化 #8a9199
 * - 状态：成功绿 / 警告橙 / 危险红
 */
public class ThemeUtils {
    private ThemeUtils() {}

    // ═══════════════════════════════════════════════════════════════
    //  文字 — 灰阶（从最深到最浅）
    // ═══════════════════════════════════════════════════════════════
    /** 焦墨 — 最深，标题/强调 */
    public static final Color INK_JIAO    = new Color(23, 28, 34);
    /** 浓墨 — 主标题 */
    public static final Color INK_NONG    = new Color(31, 36, 41);
    /** 重墨 — 正文/表格文字 */
    public static final Color INK_ZHONG   = new Color(48, 54, 61);
    /** 淡墨 — 次要文字/提示 */
    public static final Color INK_DAN     = new Color(100, 108, 118);
    /** 清墨 — 最淡，占位/禁用 */
    public static final Color INK_QING    = new Color(174, 180, 188);

    // ═══════════════════════════════════════════════════════════════
    //  主色系 — 科技蓝（Navicat/Ant Design 风格）
    // ═══════════════════════════════════════════════════════════════
    /** 朱砂红 — 危险/删除（保持语义命名，实际为警示红） */
    public static final Color CINNABAR        = new Color(220, 64, 64);
    /** 朱砂红 — 浅色 */
    public static final Color CINNABAR_LIGHT  = new Color(240, 120, 120);
    /** 朱砂红 — 深色 */
    public static final Color CINNABAR_DARK   = new Color(190, 50, 50);

    /** 石青 — 主操作/信息（实际为科技蓝） */
    public static final Color AZURITE         = new Color(24, 144, 255);
    /** 石青 — 浅色 */
    public static final Color AZURITE_LIGHT   = new Color(69, 169, 255);
    /** 石青 — 深色 */
    public static final Color AZURITE_DARK    = new Color(16, 120, 225);

    /** 石绿 — 成功/完成（实际为翠绿） */
    public static final Color MALACHITE       = new Color(72, 179, 98);
    /** 石绿 — 浅色 */
    public static final Color MALACHITE_LIGHT = new Color(110, 205, 135);
    /** 石绿 — 深色 */
    public static final Color MALACHITE_DARK  = new Color(55, 150, 80);

    /** 藤黄 — 警告/关注（实际为琥珀橙） */
    public static final Color GAMBOGE         = new Color(245, 166, 35);
    /** 藤黄 — 浅色 */
    public static final Color GAMBOGE_LIGHT   = new Color(250, 190, 85);

    /** 赭石 — 辅助/中性（实际为中性灰蓝） */
    public static final Color OCHRE           = new Color(134, 144, 156);
    /** 赭石 — 浅色 */
    public static final Color OCHRE_LIGHT     = new Color(160, 170, 182);

    // ═══════════════════════════════════════════════════════════════
    //  背景 — 浅灰/纯白
    // ═══════════════════════════════════════════════════════════════
    /** 主背景 — 浅灰 */
    public static final Color PAPER           = new Color(245, 247, 250);
    /** 卡片/面板背景 — 纯白 */
    public static final Color PAPER_WHITE     = new Color(255, 255, 255);
    /** 特殊背景/悬浮 — 淡蓝灰 */
    public static final Color SILK            = new Color(240, 244, 249);
    /** 金笺 — 强调/装饰（实际为浅金，保留少量点缀） */
    public static final Color GOLD_PAPER      = new Color(255, 214, 102);

    // ═══════════════════════════════════════════════════════════════
    //  边框和分隔线 — 浅灰
    // ═══════════════════════════════════════════════════════════════
    /** 主边框 — 中灰 */
    public static final Color BORDER_MAIN     = new Color(217, 222, 228);
    /** 浅边框 — 更浅 */
    public static final Color BORDER_LIGHT    = new Color(229, 233, 238);
    /** 分隔线 — 最浅 */
    public static final Color DIVIDER         = new Color(240, 242, 245);

    // ═══════════════════════════════════════════════════════════════
    //  状态颜色
    // ═══════════════════════════════════════════════════════════════
    /** 成功 — 绿 */
    public static final Color COLOR_SUCCESS       = MALACHITE;
    public static final Color COLOR_SUCCESS_LIGHT = MALACHITE_LIGHT;
    /** 警告 — 橙 */
    public static final Color COLOR_WARNING       = GAMBOGE;
    public static final Color COLOR_WARNING_LIGHT = GAMBOGE_LIGHT;
    /** 危险 — 红 */
    public static final Color COLOR_DANGER        = CINNABAR;
    public static final Color COLOR_DANGER_LIGHT  = CINNABAR_LIGHT;
    /** 信息 — 蓝 */
    public static final Color COLOR_INFO          = AZURITE;
    public static final Color COLOR_INFO_LIGHT    = AZURITE_LIGHT;

    // ═══════════════════════════════════════════════════════════════
    //  组件背景
    // ═══════════════════════════════════════════════════════════════
    /** 全局背景 — 浅灰 */
    public static final Color COLOR_BG            = PAPER;
    /** 卡片/面板 — 纯白 */
    public static final Color COLOR_BG_CARD       = PAPER_WHITE;
    /** 表格交替行 — 极浅灰蓝 */
    public static final Color COLOR_BG_ALTERNATE  = new Color(248, 250, 253);
    /** 输入框背景 — 纯白 */
    public static final Color COLOR_BG_INPUT      = new Color(255, 255, 255);
    /** 菜单栏 — 纯白 */
    public static final Color COLOR_MENU_BG       = new Color(255, 255, 255);
    /** 菜单悬浮 — 淡蓝 */
    public static final Color COLOR_MENU_HOVER    = new Color(230, 242, 255);

    // ═══════════════════════════════════════════════════════════════
    //  标题栏（科技深蓝）
    // ═══════════════════════════════════════════════════════════════
    /** 标题栏背景 — 深蓝渐变起始 */
    public static final Color COLOR_HEADER_BG     = new Color(28, 52, 84);
    /** 标题栏文字 — 白色 */
    public static final Color COLOR_HEADER_TEXT   = new Color(245, 250, 255);

    // ═══════════════════════════════════════════════════════════════
    //  表格
    // ═══════════════════════════════════════════════════════════════
    /** 表头背景 — 深蓝 */
    public static final Color COLOR_TABLE_HEADER_BG   = new Color(40, 68, 100);
    /** 表头文字 — 白色 */
    public static final Color COLOR_TABLE_HEADER_TEXT = new Color(255, 255, 255);

    // ═══════════════════════════════════════════════════════════════
    //  日志/控制台（深色终端风格）
    // ═══════════════════════════════════════════════════════════════
    /** 日志背景 — 深灰黑 */
    public static final Color COLOR_LOG_BG      = new Color(30, 32, 36);
    /** 日志文字 — 浅灰白 */
    public static final Color COLOR_LOG_TEXT    = new Color(210, 216, 222);
    /** 日志信息 — 浅蓝 */
    public static final Color COLOR_LOG_INFO    = AZURITE_LIGHT;
    /** 日志成功 — 浅绿 */
    public static final Color COLOR_LOG_SUCCESS = MALACHITE_LIGHT;
    /** 日志错误 — 浅红 */
    public static final Color COLOR_LOG_ERROR   = CINNABAR_LIGHT;
    /** 日志警告 — 浅橙 */
    public static final Color COLOR_LOG_WARN    = GAMBOGE_LIGHT;

    // ═══════════════════════════════════════════════════════════════
    //  字体（微软雅黑，现代无衬线）
    // ═══════════════════════════════════════════════════════════════
    /** 大标题 */
    public static final Font FONT_TITLE       = new Font("Microsoft YaHei", Font.BOLD, 20);
    /** 副标题 */
    public static final Font FONT_SUBTITLE    = new Font("Microsoft YaHei", Font.BOLD, 15);
    /** 正文 */
    public static final Font FONT_NORMAL      = new Font("Microsoft YaHei", Font.PLAIN, 14);
    /** 正文加粗 */
    public static final Font FONT_BOLD        = new Font("Microsoft YaHei", Font.BOLD, 14);
    /** 小字 */
    public static final Font FONT_SMALL       = new Font("Microsoft YaHei", Font.PLAIN, 12);
    /** 小字加粗 */
    public static final Font FONT_SMALL_BOLD  = new Font("Microsoft YaHei", Font.BOLD, 12);
    /** 超大标题 */
    public static final Font FONT_ICON        = new Font("Microsoft YaHei", Font.PLAIN, 28);
    /** 日志/代码 — 等宽 */
    public static final Font FONT_LOG         = new Font("Consolas", Font.PLAIN, 13);

    // ═══════════════════════════════════════════════════════════════
    //  别名（兼容旧代码）
    // ═══════════════════════════════════════════════════════════════
    public static final Color COLOR_PRIMARY         = AZURITE;
    public static final Color COLOR_PRIMARY_LIGHT   = AZURITE_LIGHT;
    public static final Color COLOR_PRIMARY_DARK    = AZURITE_DARK;
    public static final Color COLOR_SECONDARY       = OCHRE;
    public static final Color COLOR_SECONDARY_LIGHT = OCHRE_LIGHT;
    public static final Color COLOR_BORDER          = BORDER_MAIN;
    public static final Color COLOR_BORDER_LIGHT    = BORDER_LIGHT;
    public static final Color COLOR_DIVIDER         = DIVIDER;
    public static final Color COLOR_TEXT            = INK_ZHONG;
    public static final Color COLOR_TEXT_SECONDARY  = INK_DAN;
    public static final Color COLOR_TEXT_HINT       = INK_QING;
    public static final Color COLOR_TEXT_LIGHT      = new Color(255, 255, 255);

    // ═══════════════════════════════════════════════════════════════
    //  FlatLaf 全局主题配置
    // ═══════════════════════════════════════════════════════════════
    public static void applyFlatLafTheme() {
        // ----- 基础颜色 -----
        UIManager.put("Button.background", AZURITE);
        UIManager.put("Button.foreground", COLOR_HEADER_TEXT);
        UIManager.put("Button.arc", 8);
        UIManager.put("Button.margin", new Insets(6, 18, 6, 18));
        UIManager.put("Button.hoverBackground", AZURITE_LIGHT);
        UIManager.put("Button.pressedBackground", AZURITE_DARK);

        // ----- 对话框 -----
        UIManager.put("OptionPane.background", PAPER_WHITE);
        UIManager.put("OptionPane.messageForeground", INK_ZHONG);
        UIManager.put("OptionPane.buttonBackground", AZURITE);
        UIManager.put("OptionPane.buttonForeground", COLOR_HEADER_TEXT);
        UIManager.put("OptionPane.buttonHoverBackground", AZURITE_LIGHT);

        // ----- 标签页 -----
        UIManager.put("TabbedPane.background", PAPER);
        UIManager.put("TabbedPane.selectedBackground", PAPER_WHITE);
        UIManager.put("TabbedPane.selectedForeground", AZURITE);
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 18, 8, 18));
        UIManager.put("TabbedPane.foreground", INK_DAN);
        UIManager.put("TabbedPane.selectionBackground", new Color(220, 238, 255));

        // ----- 表格 -----
        UIManager.put("Table.background", PAPER_WHITE);
        UIManager.put("Table.alternateRowColor", COLOR_BG_ALTERNATE);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.selectionBackground", new Color(220, 238, 255));
        UIManager.put("Table.selectionForeground", INK_ZHONG);
        UIManager.put("TableHeader.background", COLOR_TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", COLOR_TABLE_HEADER_TEXT);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);
        UIManager.put("Table.gridColor", BORDER_LIGHT);

        // ----- 输入框 -----
        UIManager.put("TextField.background", COLOR_BG_INPUT);
        UIManager.put("TextField.borderColor", BORDER_MAIN);
        UIManager.put("TextField.focusedBorderColor", AZURITE);
        UIManager.put("TextField.margin", new Insets(6, 12, 6, 12));
        UIManager.put("TextArea.background", COLOR_BG_INPUT);
        UIManager.put("TextArea.borderColor", BORDER_MAIN);
        UIManager.put("TextArea.focusedBorderColor", AZURITE);
        UIManager.put("TextArea.margin", new Insets(6, 12, 6, 12));

        // ----- 下拉框 -----
        UIManager.put("ComboBox.background", COLOR_BG_INPUT);
        UIManager.put("ComboBox.borderColor", BORDER_MAIN);
        UIManager.put("ComboBox.focusedBorderColor", AZURITE);
        UIManager.put("ComboBox.buttonBackground", AZURITE);
        UIManager.put("ComboBox.buttonArrowColor", COLOR_HEADER_TEXT);
        UIManager.put("ComboBox.selectionBackground", new Color(220, 238, 255));

        // ----- 密码框 -----
        UIManager.put("PasswordField.background", COLOR_BG_INPUT);
        UIManager.put("PasswordField.borderColor", BORDER_MAIN);
        UIManager.put("PasswordField.focusedBorderColor", AZURITE);
        UIManager.put("PasswordField.margin", new Insets(6, 12, 6, 12));

        // ----- 滚动面板 -----
        UIManager.put("ScrollPane.background", PAPER_WHITE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER_LIGHT, 1));

        // ----- 面板 -----
        UIManager.put("Panel.background", PAPER);

        // ----- 列表 -----
        UIManager.put("List.background", PAPER_WHITE);
        UIManager.put("List.selectionBackground", new Color(220, 238, 255));
        UIManager.put("List.selectionForeground", INK_ZHONG);

        // ----- 进度条 -----
        UIManager.put("ProgressBar.background", BORDER_LIGHT);
        UIManager.put("ProgressBar.foreground", MALACHITE);
        UIManager.put("ProgressBar.selectionForeground", COLOR_HEADER_TEXT);

        // ----- 带标题边框 -----
        UIManager.put("TitledBorder.titleColor", AZURITE);

        // ----- 提示框 -----
        UIManager.put("ToolTip.background", new Color(255, 252, 240));
        UIManager.put("ToolTip.foreground", INK_ZHONG);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER_MAIN, 1));

        // ----- 分隔面板 -----
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.background", BORDER_LIGHT);

        // ----- 滚动条 -----
        UIManager.put("ScrollBar.thumb", new Color(196, 203, 212));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(174, 182, 192));
        UIManager.put("ScrollBar.background", PAPER);
        UIManager.put("ScrollBar.track", PAPER);

        // ----- 菜单 -----
        UIManager.put("Menu.background", COLOR_MENU_BG);
        UIManager.put("Menu.selectionBackground", COLOR_MENU_HOVER);
        UIManager.put("Menu.selectionForeground", INK_ZHONG);
        UIManager.put("MenuBar.background", COLOR_MENU_BG);
        UIManager.put("MenuItem.background", COLOR_MENU_BG);
        UIManager.put("MenuItem.selectionBackground", COLOR_MENU_HOVER);
        UIManager.put("MenuItem.selectionForeground", INK_ZHONG);

        // ----- 选择框/复选框 -----
        UIManager.put("CheckBox.background", PAPER_WHITE);
        UIManager.put("CheckBox.focus", AZURITE);
        UIManager.put("RadioButton.background", PAPER_WHITE);
        UIManager.put("RadioButton.focus", AZURITE);

        // ----- 全局额外 -----
        UIManager.put("Component.borderColor", BORDER_MAIN);
        UIManager.put("TextField.selectionBackground", AZURITE_LIGHT);
        UIManager.put("TextField.selectionForeground", COLOR_HEADER_TEXT);
        UIManager.put("FormattedTextField.background", COLOR_BG_INPUT);
        UIManager.put("FormattedTextField.borderColor", BORDER_MAIN);
        UIManager.put("FormattedTextField.focusedBorderColor", AZURITE);

        // 全局字体
        UIManager.put("Button.font", FONT_NORMAL);
        UIManager.put("Label.font", FONT_NORMAL);
        UIManager.put("TextField.font", FONT_NORMAL);
        UIManager.put("TextArea.font", FONT_NORMAL);
        UIManager.put("PasswordField.font", FONT_NORMAL);
        UIManager.put("ComboBox.font", FONT_NORMAL);
        UIManager.put("Table.font", FONT_NORMAL);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);
        UIManager.put("List.font", FONT_NORMAL);
        UIManager.put("Menu.font", FONT_NORMAL);
        UIManager.put("MenuItem.font", FONT_NORMAL);
        UIManager.put("TabbedPane.font", FONT_NORMAL);
        UIManager.put("OptionPane.font", FONT_NORMAL);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("ProgressBar.font", FONT_SMALL);
        UIManager.put("CheckBox.font", FONT_NORMAL);
        UIManager.put("RadioButton.font", FONT_NORMAL);
        UIManager.put("TitledBorder.font", FONT_SUBTITLE);
    }
}