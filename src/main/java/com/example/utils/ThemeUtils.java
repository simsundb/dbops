package com.example.utils;

import javax.swing.*;
import java.awt.*;

/**
 * 主题工具类 — 中国传统美术风格
 *
 * 设计理念：
 * - 汲取水墨画、工笔画、青绿山水、书法艺术精髓
 * - 以宣纸、绢本、墨色、朱砂、石青、石绿等传统色彩为基调
 * - 营造"雅致、含蓄、沉静"的东方美学氛围
 *
 * 配色来源：
 * - 墨分五色：焦、浓、重、淡、清
 * - 丹青：朱砂、石青、石绿
 * - 宣纸：古纸色、仿古绢
 * - 传统器物：青铜、玉器、漆器
 */
public class ThemeUtils {
    private ThemeUtils() {}

    // ═══════════════════════════════════════════════════════════════
    //  传统色彩 — 墨色系列（墨分五色）
    // ═══════════════════════════════════════════════════════════════
    /** 焦墨 — 最浓墨色，用于标题/强调 */
    public static final Color INK_JIAO    = new Color(30,  28,  26);
    /** 浓墨 — 主文字色 */
    public static final Color INK_NONG    = new Color(50,  48,  45);
    /** 重墨 — 正文/表格文字 */
    public static final Color INK_ZHONG   = new Color(70,  68,  65);
    /** 淡墨 — 次要文字/提示 */
    public static final Color INK_DAN     = new Color(130, 125, 118);
    /** 清墨 — 最淡墨色，用于分隔/占位 */
    public static final Color INK_QING    = new Color(190, 185, 175);

    // ═══════════════════════════════════════════════════════════════
    //  传统色彩 — 丹青系列（矿物颜料）
    // ═══════════════════════════════════════════════════════════════
    /** 朱砂红 — 主要操作/确认（中国传统红） */
    public static final Color CINNABAR        = new Color(190, 65,  55);
    /** 朱砂红 — 浅色 */
    public static final Color CINNABAR_LIGHT  = new Color(215, 100, 85);
    /** 朱砂红 — 深色 */
    public static final Color CINNABAR_DARK   = new Color(155, 50,  42);

    /** 石青 — 次要/信息（传统青色） */
    public static final Color AZURITE         = new Color(60,  110, 150);
    /** 石青 — 浅色 */
    public static final Color AZURITE_LIGHT   = new Color(95,  145, 185);
    /** 石青 — 深色 */
    public static final Color AZURITE_DARK    = new Color(45,  85,  115);

    /** 石绿 — 成功/完成（传统绿色） */
    public static final Color MALACHITE       = new Color(70,  130, 100);
    /** 石绿 — 浅色 */
    public static final Color MALACHITE_LIGHT = new Color(100, 165, 130);
    /** 石绿 — 深色 */
    public static final Color MALACHITE_DARK  = new Color(55,  105, 80);

    /** 藤黄 — 警告/关注（传统黄色） */
    public static final Color GAMBOGE         = new Color(200, 155, 75);
    /** 藤黄 — 浅色 */
    public static final Color GAMBOGE_LIGHT   = new Color(215, 180, 105);

    /** 赭石 — 辅助/次要（传统褐色） */
    public static final Color OCHRE           = new Color(160, 125, 95);
    /** 赭石 — 浅色 */
    public static final Color OCHRE_LIGHT     = new Color(185, 150, 120);

    // ═══════════════════════════════════════════════════════════════
    //  传统色彩 — 纸/绢系列（底色调）
    // ═══════════════════════════════════════════════════════════════
    /** 宣纸色 — 主背景（仿古宣纸） */
    public static final Color PAPER           = new Color(248, 243, 235);
    /** 宣纸白 — 卡片/面板背景 */
    public static final Color PAPER_WHITE     = new Color(252, 249, 243);
    /** 仿古绢本 — 特殊背景/边框 */
    public static final Color SILK            = new Color(235, 225, 210);
    /** 金笺 — 重要/装饰 */
    public static final Color GOLD_PAPER      = new Color(215, 195, 140);

    // ═══════════════════════════════════════════════════════════════
    //  边框和分隔线 — 墨色淡染
    // ═══════════════════════════════════════════════════════════════
    /** 主边框 — 淡墨 */
    public static final Color BORDER_MAIN     = new Color(195, 188, 178);
    /** 浅边框 — 清墨 */
    public static final Color BORDER_LIGHT    = new Color(215, 210, 202);
    /** 分隔线 — 清墨更淡 */
    public static final Color DIVIDER         = new Color(225, 220, 212);

    // ═══════════════════════════════════════════════════════════════
    //  状态颜色（传统矿物色系）
    // ═══════════════════════════════════════════════════════════════
    /** 成功 — 石绿 */
    public static final Color COLOR_SUCCESS       = MALACHITE;
    public static final Color COLOR_SUCCESS_LIGHT = MALACHITE_LIGHT;
    /** 警告 — 藤黄 */
    public static final Color COLOR_WARNING       = GAMBOGE;
    public static final Color COLOR_WARNING_LIGHT = GAMBOGE_LIGHT;
    /** 危险 — 朱砂 */
    public static final Color COLOR_DANGER        = CINNABAR;
    public static final Color COLOR_DANGER_LIGHT  = CINNABAR_LIGHT;
    /** 信息 — 石青 */
    public static final Color COLOR_INFO          = AZURITE;
    public static final Color COLOR_INFO_LIGHT    = AZURITE_LIGHT;

    // ═══════════════════════════════════════════════════════════════
    //  组件背景（宣纸/绢本质感）
    // ═══════════════════════════════════════════════════════════════
    /** 全局背景 — 宣纸 */
    public static final Color COLOR_BG            = PAPER;
    /** 卡片/面板 — 宣纸白 */
    public static final Color COLOR_BG_CARD       = PAPER_WHITE;
    /** 表格交替行 — 浅宣纸 */
    public static final Color COLOR_BG_ALTERNATE  = new Color(245, 240, 232);
    /** 输入框背景 — 宣纸白偏暖 */
    public static final Color COLOR_BG_INPUT      = new Color(250, 247, 241);
    /** 菜单栏 — 浅宣纸 */
    public static final Color COLOR_MENU_BG       = new Color(243, 238, 230);
    /** 菜单悬浮 — 淡赭石 */
    public static final Color COLOR_MENU_HOVER    = new Color(235, 225, 215);

    // ═══════════════════════════════════════════════════════════════
    //  标题栏（传统漆器/墨色）
    // ═══════════════════════════════════════════════════════════════
    /** 标题栏背景 — 墨色渐变起始 */
    public static final Color COLOR_HEADER_BG     = new Color(65,  60,  55);
    /** 标题栏文字 — 金笺色 */
    public static final Color COLOR_HEADER_TEXT   = GOLD_PAPER;

    // ═══════════════════════════════════════════════════════════════
    //  表格（仿古册页风格）
    // ═══════════════════════════════════════════════════════════════
    /** 表头背景 — 浓墨染 */
    public static final Color COLOR_TABLE_HEADER_BG   = new Color(80,  75,  70);
    /** 表头文字 — 金笺色 */
    public static final Color COLOR_TABLE_HEADER_TEXT = new Color(230, 220, 200);

    // ═══════════════════════════════════════════════════════════════
    //  日志/控制台（仿碑帖墨拓风格）
    // ═══════════════════════════════════════════════════════════════
    /** 日志背景 — 墨拓底色 */
    public static final Color COLOR_LOG_BG      = new Color(40,  38,  35);
    /** 日志文字 — 碑帖拓片色 */
    public static final Color COLOR_LOG_TEXT    = new Color(200, 190, 175);
    /** 日志信息 — 石青色 */
    public static final Color COLOR_LOG_INFO    = AZURITE_LIGHT;
    /** 日志成功 — 石绿色 */
    public static final Color COLOR_LOG_SUCCESS = MALACHITE_LIGHT;
    /** 日志错误 — 朱砂色 */
    public static final Color COLOR_LOG_ERROR   = CINNABAR_LIGHT;
    /** 日志警告 — 藤黄色 */
    public static final Color COLOR_LOG_WARN    = GAMBOGE_LIGHT;

    // ═══════════════════════════════════════════════════════════════
    //  字体（传统书体风格）
    // ═══════════════════════════════════════════════════════════════
    /** 大标题 — 楷体/行书风格 */
    public static final Font FONT_TITLE       = new Font("华文楷体", Font.BOLD, 20);
    /** 副标题 — 楷体 */
    public static final Font FONT_SUBTITLE    = new Font("华文楷体", Font.BOLD, 15);
    /** 正文 — 宋体/楷体 */
    public static final Font FONT_NORMAL      = new Font("华文楷体", Font.PLAIN, 14);
    /** 正文加粗 */
    public static final Font FONT_BOLD        = new Font("华文楷体", Font.BOLD, 14);
    /** 小字 — 更小楷体 */
    public static final Font FONT_SMALL       = new Font("华文楷体", Font.PLAIN, 12);
    /** 小字加粗 */
    public static final Font FONT_SMALL_BOLD  = new Font("华文楷体", Font.BOLD, 12);
    /** 超大标题 */
    public static final Font FONT_ICON        = new Font("华文楷体", Font.PLAIN, 28);
    /** 日志/代码 — 仿碑帖风格 */
    public static final Font FONT_LOG         = new Font("华文楷体", Font.PLAIN, 13);

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
    public static final Color COLOR_TEXT_LIGHT      = new Color(248, 245, 238);

    // ═══════════════════════════════════════════════════════════════
    //  FlatLaf 全局主题配置（传统美术风格）
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

        // ----- 标签页（仿册页） -----
        UIManager.put("TabbedPane.background", PAPER);
        UIManager.put("TabbedPane.selectedBackground", PAPER_WHITE);
        UIManager.put("TabbedPane.selectedForeground", AZURITE);
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 18, 8, 18));
        UIManager.put("TabbedPane.foreground", INK_DAN);
        UIManager.put("TabbedPane.selectionBackground", new Color(180, 210, 230));

        // ----- 表格（仿古册页） -----
        UIManager.put("Table.background", PAPER_WHITE);
        UIManager.put("Table.alternateRowColor", COLOR_BG_ALTERNATE);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.selectionBackground", new Color(190, 210, 225));
        UIManager.put("Table.selectionForeground", INK_ZHONG);
        UIManager.put("TableHeader.background", COLOR_TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", COLOR_TABLE_HEADER_TEXT);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);
        UIManager.put("Table.gridColor", BORDER_LIGHT);

        // ----- 输入框（仿信笺） -----
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
        UIManager.put("ComboBox.selectionBackground", new Color(180, 210, 230));

        // ----- 密码框 -----
        UIManager.put("PasswordField.background", COLOR_BG_INPUT);
        UIManager.put("PasswordField.borderColor", BORDER_MAIN);
        UIManager.put("PasswordField.focusedBorderColor", AZURITE);
        UIManager.put("PasswordField.margin", new Insets(6, 12, 6, 12));

        // ----- 滚动面板（仿绢本装裱） -----
        UIManager.put("ScrollPane.background", PAPER_WHITE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER_LIGHT, 1));

        // ----- 面板（宣纸底） -----
        UIManager.put("Panel.background", PAPER);

        // ----- 列表（仿古卷轴） -----
        UIManager.put("List.background", PAPER_WHITE);
        UIManager.put("List.selectionBackground", new Color(190, 210, 225));
        UIManager.put("List.selectionForeground", INK_ZHONG);

        // ----- 进度条（仿玉器） -----
        UIManager.put("ProgressBar.background", BORDER_LIGHT);
        UIManager.put("ProgressBar.foreground", MALACHITE);
        UIManager.put("ProgressBar.selectionForeground", COLOR_HEADER_TEXT);

        // ----- 带标题边框（仿画卷题跋） -----
        UIManager.put("TitledBorder.titleColor", AZURITE);

        // ----- 提示框（仿便签） -----
        UIManager.put("ToolTip.background", COLOR_HEADER_BG);
        UIManager.put("ToolTip.foreground", COLOR_HEADER_TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(new Color(100, 90, 80), 1));

        // ----- 分隔面板 -----
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.background", BORDER_LIGHT);

        // ----- 滚动条（仿竹简） -----
        UIManager.put("ScrollBar.thumb", new Color(160, 150, 140));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(130, 120, 110));
        UIManager.put("ScrollBar.background", PAPER);
        UIManager.put("ScrollBar.track", PAPER);

        // ----- 菜单（仿书卷） -----
        UIManager.put("Menu.background", COLOR_MENU_BG);
        UIManager.put("Menu.selectionBackground", COLOR_MENU_HOVER);
        UIManager.put("Menu.selectionForeground", INK_ZHONG);
        UIManager.put("MenuBar.background", COLOR_MENU_BG);
        UIManager.put("MenuItem.background", COLOR_MENU_BG);
        UIManager.put("MenuItem.selectionBackground", COLOR_MENU_HOVER);
        UIManager.put("MenuItem.selectionForeground", INK_ZHONG);

        // ----- 选择框/复选框（仿印章） -----
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