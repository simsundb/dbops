package com.sunzh.utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 主题工具类 — 暗色磨砂海军蓝 · 素雅设计体系
 *
 * 设计理念：
 * - 单一权威色板：所有页面必须从这里取色，禁止散落硬编码颜色
 * - 暗蓝磨砂：主色采用低饱和、偏暗的钢灰蓝（steel navy），摒弃亮眼的天蓝
 * - 素雅克制：冷灰底 + 纯白卡片 + 柔和的低对比边框，层次分明不抢眼
 * - 高一致性与层次感：主/次/提示文字、卡片/背景/边框分级明确
 * - 控件尺寸统一：按钮、输入框、间距全部通过常量约束
 */
public class ThemeUtils {
    private ThemeUtils() {}

    // ═══════════════════════════════════════════════════════════════
    //  品牌色 — 暗色磨砂海军蓝（Steel Navy）
    // ═══════════════════════════════════════════════════════════════
    /** 主色 — 按钮、标题、选中态、链接 */
    public static final Color COLOR_PRIMARY        = new Color(52,  84,  152);
    /** 主色浅 — hover / 进度条 */
    public static final Color COLOR_PRIMARY_LIGHT  = new Color(84,  114, 184);
    /** 主色深 — pressed / active */
    public static final Color COLOR_PRIMARY_DARK   = new Color(38,  62,  116);
    /** 主色极浅 — 悬浮底色、标签底色、选中卡片背景 */
    public static final Color COLOR_PRIMARY_SOFT   = new Color(224, 231, 244);
    /** 主色选中底 — 表格选中、列表选中 */
    public static final Color COLOR_PRIMARY_SELECT = new Color(202, 214, 237);

    // ═══════════════════════════════════════════════════════════════
    //  辅助色
    // ═══════════════════════════════════════════════════════════════
    /** 次要按钮 / 中性操作色 */
    public static final Color COLOR_SECONDARY       = new Color(116, 128, 143);
    /** 次要色浅 */
    public static final Color COLOR_SECONDARY_LIGHT = new Color(142, 153, 168);

    // ═══════════════════════════════════════════════════════════════
    //  中性背景色
    // ═══════════════════════════════════════════════════════════════
    /** 全局背景 — 冷调浅灰蓝 */
    public static final Color COLOR_BG          = new Color(240, 243, 248);
    /** 卡片 / 面板背景 — 纯白 */
    public static final Color COLOR_BG_CARD     = new Color(255, 255, 255);
    /** 表格交替行 */
    public static final Color COLOR_BG_ALTERNATE = new Color(243, 246, 251);
    /** 输入框背景 — 纯白 */
    public static final Color COLOR_BG_INPUT    = new Color(255, 255, 255);
    /** 悬浮底色 */
    public static final Color COLOR_BG_HOVER    = new Color(234, 240, 250);

    // ═══════════════════════════════════════════════════════════════
    //  边框和分隔线
    // ═══════════════════════════════════════════════════════════════
    /** 默认边框 */
    public static final Color COLOR_BORDER       = new Color(214, 221, 231);
    /** 浅边框 — 分隔线、卡片边界 */
    public static final Color COLOR_BORDER_LIGHT = new Color(227, 232, 240);
    /** 分隔线 */
    public static final Color COLOR_DIVIDER      = new Color(223, 229, 238);

    // ═══════════════════════════════════════════════════════════════
    //  文字颜色
    // ═══════════════════════════════════════════════════════════════
    /** 主文字 — 深石板灰 */
    public static final Color COLOR_TEXT           = new Color(35,  46,  60);
    /** 次要文字 */
    public static final Color COLOR_TEXT_SECONDARY = new Color(94,  106, 121);
    /** 提示文字 */
    public static final Color COLOR_TEXT_HINT      = new Color(151, 161, 176);
    /** 浅色底上的文字 — 白色 */
    public static final Color COLOR_TEXT_LIGHT     = new Color(255, 255, 255);

    // ═══════════════════════════════════════════════════════════════
    //  状态颜色
    // ═══════════════════════════════════════════════════════════════
    /** 成功 — 翠绿 */
    public static final Color COLOR_SUCCESS       = new Color(46,  158, 102);
    /** 成功浅 */
    public static final Color COLOR_SUCCESS_LIGHT = new Color(76,  181, 126);
    /** 成功底色 */
    public static final Color COLOR_SUCCESS_SOFT  = new Color(232, 246, 238);
    /** 警告 — 琥珀 */
    public static final Color COLOR_WARNING       = new Color(222, 143, 31);
    /** 警告浅 */
    public static final Color COLOR_WARNING_LIGHT = new Color(233, 170, 77);
    /** 警告底色 */
    public static final Color COLOR_WARNING_SOFT  = new Color(252, 243, 226);
    /** 危险 — 珊瑚红 */
    public static final Color COLOR_DANGER        = new Color(226, 73,  77);
    /** 危险浅 */
    public static final Color COLOR_DANGER_LIGHT  = new Color(239, 112, 115);
    /** 危险底色 */
    public static final Color COLOR_DANGER_SOFT   = new Color(252, 233, 233);
    /** 信息 — 暗蓝（同主色） */
    public static final Color COLOR_INFO          = new Color(52,  84,  152);
    /** 信息浅 */
    public static final Color COLOR_INFO_LIGHT    = new Color(84,  114, 184);

    // ═══════════════════════════════════════════════════════════════
    //  标题栏和菜单
    // ═══════════════════════════════════════════════════════════════
    /** 顶部标题栏渐变起始 — 深海军蓝 */
    public static final Color COLOR_HEADER_BG_START = new Color(22,  38,  74);
    /** 顶部标题栏渐变结束 — 磨砂海军蓝 */
    public static final Color COLOR_HEADER_BG_END   = new Color(43,  70,  136);
    /** 顶部标题栏背景（兼容旧引用） */
    public static final Color COLOR_HEADER_BG       = COLOR_HEADER_BG_START;
    /** 顶部标题栏文字 */
    public static final Color COLOR_HEADER_TEXT     = new Color(242, 246, 253);
    /** 菜单栏背景 — 纯白 */
    public static final Color COLOR_MENU_BG         = new Color(255, 255, 255);
    /** 菜单悬浮 — 极浅靛蓝 */
    public static final Color COLOR_MENU_HOVER      = new Color(224, 231, 244);

    // ═══════════════════════════════════════════════════════════════
    //  表格
    // ═══════════════════════════════════════════════════════════════
    /** 表头背景 — 浅灰蓝（现代浅色表头） */
    public static final Color COLOR_TABLE_HEADER_BG   = new Color(237, 241, 248);
    /** 表头文字 — 深石板灰 */
    public static final Color COLOR_TABLE_HEADER_TEXT = new Color(58,  70,  87);
    /** 表格交替行 */
    public static final Color COLOR_TABLE_ROW_ALT     = COLOR_BG_ALTERNATE;
    /** 表格选中背景 */
    public static final Color COLOR_TABLE_SELECTION   = COLOR_PRIMARY_SELECT;

    // ═══════════════════════════════════════════════════════════════
    //  日志 / 控制台（深色终端）
    // ═══════════════════════════════════════════════════════════════
    /** 日志面板背景 — 深石板蓝黑 */
    public static final Color COLOR_LOG_BG      = new Color(29,  36,  51);
    /** 日志文字 */
    public static final Color COLOR_LOG_TEXT    = new Color(199, 208, 221);
    /** 日志信息 — 亮蓝 */
    public static final Color COLOR_LOG_INFO    = new Color(106, 166, 240);
    /** 日志成功 — 翠绿 */
    public static final Color COLOR_LOG_SUCCESS = new Color(94,  198, 146);
    /** 日志错误 — 珊瑚红 */
    public static final Color COLOR_LOG_ERROR   = new Color(240, 112, 107);
    /** 日志警告 — 琥珀 */
    public static final Color COLOR_LOG_WARN    = new Color(240, 190, 92);

    // ═══════════════════════════════════════════════════════════════
    //  字体常量
    // ═══════════════════════════════════════════════════════════════
    public static final Font FONT_TITLE       = new Font("Microsoft YaHei", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE    = new Font("Microsoft YaHei", Font.BOLD, 15);
    public static final Font FONT_NORMAL      = new Font("Microsoft YaHei", Font.PLAIN, 14);
    public static final Font FONT_BOLD        = new Font("Microsoft YaHei", Font.BOLD, 14);
    public static final Font FONT_SMALL       = new Font("Microsoft YaHei", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD  = new Font("Microsoft YaHei", Font.BOLD, 12);
    public static final Font FONT_ICON        = new Font("Segoe UI", Font.PLAIN, 26);
    public static final Font FONT_LOG         = new Font("Consolas", Font.PLAIN, 12);

    // ═══════════════════════════════════════════════════════════════
    //  控件尺寸常量 — 全局统一
    // ═══════════════════════════════════════════════════════════════
    /** 按钮高度 */
    public static final int BTN_HEIGHT       = 36;
    /** 按钮水平内边距 */
    public static final int BTN_PAD_X        = 18;
    /** 按钮垂直内边距 */
    public static final int BTN_PAD_Y        = 8;
    /** 输入框 / 下拉框高度 */
    public static final int INPUT_HEIGHT     = 34;
    /** 输入框水平内边距 */
    public static final int INPUT_PAD_X      = 10;
    /** 输入框垂直内边距 */
    public static final int INPUT_PAD_Y      = 7;
    /** 圆角半径 */
    public static final int RADIUS           = 8;
    /** 表单字段垂直间距 */
    public static final int GAP               = 10;
    /** 卡片内边距 */
    public static final int CARD_PAD          = 18;
    /** 页面留白 */
    public static final int PAGE_PAD          = 22;
    /** 标签文字与控件间距 */
    public static final int LABEL_GAP         = 6;

    // ═══════════════════════════════════════════════════════════════
    //  常用 Border 工厂
    // ═══════════════════════════════════════════════════════════════

    /** 卡片边框：1px 浅边框 + 内边距 */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(CARD_PAD, CARD_PAD, CARD_PAD, CARD_PAD));
    }

    /** 卡片边框（自定义内边距） */
    public static Border cardBorder(int pad) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(pad, pad, pad, pad));
    }

    /** 纯内边距边框 */
    public static Border paddingBorder(int t, int l, int b, int r) {
        return new EmptyBorder(t, l, b, r);
    }

    // ═══════════════════════════════════════════════════════════════
    //  通用控件工厂 — 统一尺寸与视觉
    // ═══════════════════════════════════════════════════════════════

    /** 标准输入框（统一高度、圆角、焦点色） */
    public static JTextField field(String text) {
        JTextField f = new JTextField(text);
        f.setFont(FONT_NORMAL);
        f.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        f.setMargin(new Insets(INPUT_PAD_Y, INPUT_PAD_X, INPUT_PAD_Y, INPUT_PAD_X));
        return f;
    }

    /** 标准密码框 */
    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(FONT_NORMAL);
        f.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        f.setMargin(new Insets(INPUT_PAD_Y, INPUT_PAD_X, INPUT_PAD_Y, INPUT_PAD_X));
        return f;
    }

    /** 标准下拉框 */
    public static JComboBox<String> comboBox(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(FONT_NORMAL);
        c.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        return c;
    }

    /** 标准主按钮（靛蓝填充） */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        styleSolid(b, COLOR_PRIMARY, COLOR_PRIMARY_LIGHT, COLOR_PRIMARY_DARK);
        return b;
    }

    /** 标准次要按钮（中性灰填充） */
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        styleSolid(b, COLOR_SECONDARY, COLOR_SECONDARY_LIGHT, COLOR_SECONDARY);
        return b;
    }

    /** 标准成功按钮 */
    public static JButton successButton(String text) {
        JButton b = new JButton(text);
        styleSolid(b, COLOR_SUCCESS, COLOR_SUCCESS_LIGHT, COLOR_SUCCESS);
        return b;
    }

    /** 标准危险按钮 */
    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        styleSolid(b, COLOR_DANGER, COLOR_DANGER_LIGHT, COLOR_DANGER);
        return b;
    }

    /** 描边按钮（白底 + 主色边框文字） */
    public static JButton outlineButton(String text) {
        return outlineButton(text, COLOR_PRIMARY);
    }

    /** 描边按钮（自定义颜色） */
    public static JButton outlineButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setForeground(color);
        b.setBackground(COLOR_BG_CARD);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(BTN_PAD_Y, BTN_PAD_X, BTN_PAD_Y, BTN_PAD_X)));
        fitHeight(b);
        b.setOpaque(true);
        return b;
    }

    /** 实心按钮通用样式（统一尺寸、hover、按下态） */
    private static void styleSolid(JButton b, Color base, Color hover, Color pressed) {
        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(BTN_PAD_Y, BTN_PAD_X, BTN_PAD_Y, BTN_PAD_X));
        fitHeight(b);
        b.setOpaque(true);
        b.putClientProperty("JButton.base", base);
        b.putClientProperty("JButton.hover", hover);
        b.putClientProperty("JButton.pressed", pressed);
    }

    /**
     * 统一按钮高度，宽度按内容自适应。
     * 注意：不能把宽度设为 0，否则在 FlowLayout 中按钮会塌陷成不可见。
     */
    private static void fitHeight(JButton b) {
        Dimension d = b.getPreferredSize();
        d.height = BTN_HEIGHT;
        b.setPreferredSize(d);
    }

    /** 卡片面板（纯白底 + 浅边框） */
    public static JPanel cardPanel() {
        return cardPanel(COLOR_BG_CARD);
    }

    /** 卡片面板（自定义背景） */
    public static JPanel cardPanel(Color bg) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        p.setBorder(cardBorder());
        return p;
    }

    /** 分区标题行：带图标的粗体标题 + 底部浅色分隔线 */
    public static JPanel sectionHeader(String iconName, String text) {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        JLabel icon = new JLabel(SvgIconUtils.get(iconName, 18, COLOR_PRIMARY));
        row.add(icon);
        JLabel title = new JLabel(text);
        title.setFont(FONT_SUBTITLE);
        title.setForeground(COLOR_TEXT);
        row.add(title);
        wrap.add(row, BorderLayout.NORTH);

        JPanel line = new JPanel();
        line.setPreferredSize(new Dimension(0, 1));
        line.setBackground(COLOR_DIVIDER);
        wrap.add(line, BorderLayout.CENTER);
        return wrap;
    }

    /** 表单标签 */
    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_NORMAL);
        l.setForeground(COLOR_TEXT_SECONDARY);
        l.setBorder(BorderFactory.createEmptyBorder(0, 2, LABEL_GAP, 0));
        return l;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FlatLaf 全局主题配置
    // ═══════════════════════════════════════════════════════════════
    public static void applyFlatLafTheme() {
        // Button
        UIManager.put("Button.background", COLOR_PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("Button.margin", new Insets(BTN_PAD_Y, BTN_PAD_X, BTN_PAD_Y, BTN_PAD_X));
        UIManager.put("Button.hoverBackground", COLOR_PRIMARY_LIGHT);
        UIManager.put("Button.pressedBackground", COLOR_PRIMARY_DARK);
        UIManager.put("Button.focusWidth", 0);

        // OptionPane（对话框按钮）
        UIManager.put("OptionPane.background", COLOR_BG_CARD);
        UIManager.put("OptionPane.messageForeground", COLOR_TEXT);
        UIManager.put("OptionPane.buttonBackground", COLOR_PRIMARY);
        UIManager.put("OptionPane.buttonForeground", Color.WHITE);
        UIManager.put("OptionPane.buttonHoverBackground", COLOR_PRIMARY_LIGHT);
        UIManager.put("OptionPane.border", cardBorder(12));

        // TabbedPane
        UIManager.put("TabbedPane.background", COLOR_BG);
        UIManager.put("TabbedPane.selectedBackground", COLOR_BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", COLOR_PRIMARY);
        UIManager.put("TabbedPane.tabInsets", new Insets(10, 18, 10, 18));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabArc", RADIUS);

        // Table
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.alternateRowColor", COLOR_BG_ALTERNATE);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.selectionBackground", COLOR_PRIMARY_SELECT);
        UIManager.put("Table.selectionForeground", COLOR_TEXT);
        UIManager.put("Table.gridColor", COLOR_BORDER_LIGHT);
        UIManager.put("TableHeader.background", COLOR_TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", COLOR_TABLE_HEADER_TEXT);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);
        UIManager.put("TableHeader.height", 34);

        // TextField
        UIManager.put("TextField.background", COLOR_BG_INPUT);
        UIManager.put("TextField.borderColor", COLOR_BORDER);
        UIManager.put("TextField.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("TextField.arc", RADIUS);
        UIManager.put("TextField.margin", new Insets(INPUT_PAD_Y, INPUT_PAD_X, INPUT_PAD_Y, INPUT_PAD_X));

        // ComboBox
        UIManager.put("ComboBox.background", COLOR_BG_INPUT);
        UIManager.put("ComboBox.borderColor", COLOR_BORDER);
        UIManager.put("ComboBox.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("ComboBox.arc", RADIUS);
        UIManager.put("ComboBox.padding", new Insets(INPUT_PAD_Y, INPUT_PAD_X, INPUT_PAD_Y, INPUT_PAD_X));
        UIManager.put("ComboBox.buttonBackground", COLOR_PRIMARY);
        UIManager.put("ComboBox.buttonArrowColor", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", COLOR_PRIMARY_SOFT);

        // PasswordField
        UIManager.put("PasswordField.background", COLOR_BG_INPUT);
        UIManager.put("PasswordField.borderColor", COLOR_BORDER);
        UIManager.put("PasswordField.focusedBorderColor", COLOR_PRIMARY);
        UIManager.put("PasswordField.arc", RADIUS);
        UIManager.put("PasswordField.margin", new Insets(INPUT_PAD_Y, INPUT_PAD_X, INPUT_PAD_Y, INPUT_PAD_X));

        // ScrollPane
        UIManager.put("ScrollPane.background", COLOR_BG_CARD);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(COLOR_BORDER_LIGHT, 1));

        // Panel
        UIManager.put("Panel.background", COLOR_BG);
        UIManager.put("Panel.arc", RADIUS);

        // List
        UIManager.put("List.background", COLOR_BG_CARD);
        UIManager.put("List.selectionBackground", COLOR_PRIMARY_SELECT);
        UIManager.put("List.selectionForeground", COLOR_TEXT);

        // ProgressBar
        UIManager.put("ProgressBar.background", COLOR_BORDER_LIGHT);
        UIManager.put("ProgressBar.foreground", COLOR_PRIMARY_LIGHT);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.arc", 6);

        // TitledBorder
        UIManager.put("TitledBorder.titleColor", COLOR_PRIMARY);
        UIManager.put("TitledBorder.border", BorderFactory.createLineBorder(COLOR_BORDER_LIGHT));

        // ToolTip
        UIManager.put("ToolTip.background", COLOR_HEADER_BG_START);
        UIManager.put("ToolTip.foreground", COLOR_HEADER_TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // SplitPane
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.background", COLOR_BORDER_LIGHT);

        // 额外
        UIManager.put("Component.borderColor", COLOR_BORDER);
        UIManager.put("TextField.selectionBackground", COLOR_PRIMARY_LIGHT);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("CheckBox.focusWidth", 0);
        UIManager.put("RadioButton.focusWidth", 0);

        // 顶层菜单条：浅色文字融入深海军蓝顶栏，hover 呈现磨砂高亮
        UIManager.put("Menu.foreground", COLOR_HEADER_TEXT);
        // 顶栏菜单与白底下拉子菜单共用同一套悬停色（浅蓝底 + 深色字），
        // 与 MenuItem 悬停一致；否则半透明白底在白下拉里悬停时文字会消失。
        UIManager.put("Menu.selectionBackground", COLOR_PRIMARY_SELECT);
        UIManager.put("Menu.selectionForeground", COLOR_TEXT);
        UIManager.put("Menu.background", COLOR_HEADER_BG_START);
        UIManager.put("MenuBar.background", COLOR_HEADER_BG_START);
        UIManager.put("MenuBar.border", BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 下拉菜单项：白底 + 主色悬浮 + 深色文字（素雅一致）
        UIManager.put("MenuItem.foreground", COLOR_TEXT);
        UIManager.put("MenuItem.background", COLOR_BG_CARD);
        UIManager.put("MenuItem.selectionBackground", COLOR_PRIMARY_SELECT);
        UIManager.put("MenuItem.selectionForeground", COLOR_TEXT);
        UIManager.put("MenuItem.acceleratorForeground", COLOR_TEXT_HINT);
        UIManager.put("MenuItem.acceleratorSelectionForeground", COLOR_TEXT);
        UIManager.put("MenuItem.margin", new Insets(9, 18, 9, 18));

        // 弹出菜单容器
        UIManager.put("PopupMenu.background", COLOR_BG_CARD);
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(6, 6, 6, 6)));
    }
}
