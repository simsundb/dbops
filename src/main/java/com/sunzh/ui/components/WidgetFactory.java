package com.sunzh.ui.components;

import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Cursor;

/**
 * 界面工厂 — 统一所有 UI 组件的创建，保证全局风格一致。
 *
 * 设计原则：
 * - 所有颜色、字体取自 {@link ThemeUtils}，禁止硬编码
 * - 按钮/输入框/表格等统一尺寸规范
 * - 组件创建收敛到一个地方，便于后续主题调整
 */
public class WidgetFactory {

    private WidgetFactory() {}

    // ================================================================
    //  尺寸规范
    // ================================================================
    /** 大按钮 */
    public static final Dimension SIZE_BTN_LARGE  = new Dimension(110, 34);
    /** 标准按钮 */
    public static final Dimension SIZE_BTN_STANDARD = new Dimension(90, 34);
    /** 小按钮 */
    public static final Dimension SIZE_BTN_SMALL   = new Dimension(80, 30);
    /** 工具栏按钮 */
    public static final Dimension SIZE_BTN_TOOLBAR = new Dimension(72, 28);
    /** 输入框高度 */
    public static final int FIELD_HEIGHT = 30;

    // ================================================================
    //  按钮工厂
    // ================================================================

    /** 主操作按钮（科技蓝） */
    public static JButton primaryButton(String text) {
        return primaryButton(text, null);
    }

    /** 主操作按钮（科技蓝），带图标 */
    public static JButton primaryButton(String text, String iconName) {
        JButton btn = buildColoredButton(text, iconName, ThemeUtils.COLOR_PRIMARY,
                ThemeUtils.COLOR_PRIMARY_LIGHT, ThemeUtils.COLOR_PRIMARY_DARK);
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 大号主操作按钮 */
    public static JButton primaryButtonLarge(String text, String iconName) {
        JButton btn = primaryButton(text, iconName);
        btn.setPreferredSize(SIZE_BTN_LARGE);
        return btn;
    }

    /** 危险操作按钮（红） */
    public static JButton dangerButton(String text, String iconName) {
        JButton btn = buildColoredButton(text, iconName, ThemeUtils.COLOR_DANGER,
                ThemeUtils.COLOR_DANGER_LIGHT, ThemeUtils.COLOR_DANGER.darker());
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 成功按钮（绿） */
    public static JButton successButton(String text, String iconName) {
        JButton btn = buildColoredButton(text, iconName, ThemeUtils.COLOR_SUCCESS,
                ThemeUtils.COLOR_SUCCESS_LIGHT, ThemeUtils.COLOR_SUCCESS.darker());
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 信息按钮（中性灰） */
    public static JButton infoButton(String text, String iconName) {
        JButton btn = buildColoredButton(text, iconName, ThemeUtils.COLOR_INFO,
                ThemeUtils.COLOR_INFO_LIGHT, ThemeUtils.COLOR_INFO.darker());
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 次要操作按钮（中性灰） */
    public static JButton secondaryButton(String text, String iconName) {
        return infoButton(text, iconName);
    }

    /** 警告按钮（橙） */
    public static JButton warningButton(String text, String iconName) {
        JButton btn = buildColoredButton(text, iconName, ThemeUtils.COLOR_WARNING,
                ThemeUtils.COLOR_WARNING_LIGHT, ThemeUtils.COLOR_WARNING.darker());
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 轮廓按钮（白底 + 彩色边框文字） */
    public static JButton outlineButton(String text, String iconName, Color color) {
        ImageIcon icon = iconName != null ? SvgIconUtils.get(iconName, 16, color) : null;
        JButton btn = new JButton(text, icon);
        btn.setFont(ThemeUtils.FONT_BOLD);
        btn.setForeground(color);
        btn.setBackground(ThemeUtils.COLOR_BG_CARD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btn.setIconTextGap(6);
        btn.setPreferredSize(SIZE_BTN_STANDARD);
        return btn;
    }

    /** 工具栏小按钮（无背景描边） */
    public static JButton toolbarButton(String text, String iconName) {
        JButton btn = outlineButton(text, iconName, ThemeUtils.COLOR_PRIMARY);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setPreferredSize(SIZE_BTN_TOOLBAR);
        return btn;
    }

    /** 关闭/取消按钮（灰色中性） */
    public static JButton closeButton(String text) {
        JButton btn = buildColoredButton(text, null, ThemeUtils.COLOR_SECONDARY,
                ThemeUtils.COLOR_SECONDARY_LIGHT, ThemeUtils.COLOR_SECONDARY.darker());
        btn.setPreferredSize(SIZE_BTN_SMALL);
        return btn;
    }

    private static JButton buildColoredButton(String text, String iconName, Color bg,
                                              Color hover, Color pressed) {
        ImageIcon icon = iconName != null ? SvgIconUtils.getWhite(iconName, 16) : null;
        JButton btn = new JButton(text, icon);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.setIconTextGap(6);
        return btn;
    }

    // ================================================================
    //  标签工厂
    // ================================================================

    /** 标准正文标签 */
    public static JLabel label(String text) {
        return label(text, ThemeUtils.FONT_NORMAL, ThemeUtils.COLOR_TEXT);
    }

    /** 加粗标签 */
    public static JLabel boldLabel(String text) {
        return label(text, ThemeUtils.FONT_BOLD, ThemeUtils.COLOR_TEXT);
    }

    /** 小字标签 */
    public static JLabel smallLabel(String text) {
        return label(text, ThemeUtils.FONT_SMALL, ThemeUtils.COLOR_TEXT_SECONDARY);
    }

    /** 标题标签 */
    public static JLabel titleLabel(String text) {
        return label(text, ThemeUtils.FONT_TITLE, ThemeUtils.COLOR_TEXT);
    }

    /** 指定字体颜色的标签 */
    public static JLabel label(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }

    /** 带图标标签（图标主题色 16px） */
    public static JLabel labelWithIcon(String iconName, String text) {
        JLabel lbl = new JLabel(text, SvgIconUtils.get(iconName, 16), SwingConstants.LEADING);
        lbl.setFont(ThemeUtils.FONT_NORMAL);
        lbl.setForeground(ThemeUtils.COLOR_TEXT);
        lbl.setIconTextGap(6);
        return lbl;
    }

    /** 带图标标签（指定图标尺寸和颜色） */
    public static JLabel iconLabelWithText(String iconName, String text, int iconSize, Color color) {
        JLabel lbl = new JLabel(text, SvgIconUtils.get(iconName, iconSize, color), SwingConstants.LEADING);
        lbl.setFont(ThemeUtils.FONT_NORMAL);
        lbl.setForeground(color);
        lbl.setIconTextGap(6);
        return lbl;
    }

    /** 带图标标签（指定字体和颜色） */
    public static JLabel labelWithFont(String iconName, String text, Font font, Color color) {
        int size = font.getSize();
        JLabel lbl = new JLabel(text, SvgIconUtils.get(iconName, size, color), SwingConstants.LEADING);
        lbl.setFont(font);
        lbl.setForeground(color);
        lbl.setIconTextGap(6);
        return lbl;
    }

    /** 纯图标标签 */
    public static JLabel iconLabel(String iconName, int size, Color color) {
        return new JLabel(SvgIconUtils.get(iconName, size, color));
    }

    // ================================================================
    //  输入控件工厂
    // ================================================================

    /** 统一样式文本框 */
    public static JTextField textField() {
        return textField(0);
    }

    /** 统一样式文本框（指定列数） */
    public static JTextField textField(int columns) {
        JTextField tf = columns > 0 ? new JTextField(columns) : new JTextField();
        tf.setFont(ThemeUtils.FONT_NORMAL);
        tf.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
        tf.setBackground(ThemeUtils.COLOR_BG_INPUT);
        return tf;
    }

    /** 统一样式密码框 */
    public static JPasswordField passwordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(ThemeUtils.FONT_NORMAL);
        pf.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
        pf.setBackground(ThemeUtils.COLOR_BG_INPUT);
        return pf;
    }

    /** 统一样式下拉框 */
    public static JComboBox<String> comboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(ThemeUtils.FONT_NORMAL);
        cb.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
        cb.setBackground(ThemeUtils.COLOR_BG_INPUT);
        return cb;
    }

    /** 统一样式复选框 */
    public static JCheckBox checkBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(ThemeUtils.FONT_NORMAL);
        cb.setBackground(ThemeUtils.COLOR_BG_CARD);
        return cb;
    }

    /** 统一样式文本域 */
    public static JTextArea textArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setFont(ThemeUtils.FONT_NORMAL);
        ta.setBackground(ThemeUtils.COLOR_BG_INPUT);
        return ta;
    }

    /** 暗色日志文本域 */
    public static JTextArea logArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setEditable(false);
        ta.setFont(ThemeUtils.FONT_LOG);
        ta.setBackground(ThemeUtils.COLOR_LOG_BG);
        ta.setForeground(ThemeUtils.COLOR_LOG_TEXT);
        return ta;
    }

    // ================================================================
    //  表格工厂
    // ================================================================

    /** 创建只读表格模型 */
    public static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** 创建统一样式表格 */
    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        applyTableStyle(table);
        return table;
    }

    /** 对已有 JTable 应用统一样式 */
    public static void applyTableStyle(JTable table) {
        table.setFont(ThemeUtils.FONT_NORMAL);
        table.setRowHeight(28);
        table.setBackground(ThemeUtils.COLOR_BG_CARD);
        table.setForeground(ThemeUtils.COLOR_TEXT);
        table.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        table.setSelectionBackground(new Color(190, 210, 225));
        table.setSelectionForeground(ThemeUtils.INK_ZHONG);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(10, 2));
        table.getTableHeader().setFont(ThemeUtils.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(ThemeUtils.COLOR_TABLE_HEADER_BG);
        table.getTableHeader().setForeground(ThemeUtils.COLOR_TABLE_HEADER_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
    }

    // ================================================================
    //  面板 / 边框工厂
    // ================================================================

    /** 带标题边框（统一风格） */
    public static Border titledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        );
    }

    /** 卡片面板（纯白底） */
    public static JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER_LIGHT, 1));
        return panel;
    }

    /** 表单面板（GridBagLayout 预设，纵向自动布局） */
    public static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        return panel;
    }

    /** 透明面板 */
    public static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    // ================================================================
    //  其他工厂
    // ================================================================

    /** 统一样式滚动面板 */
    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ThemeUtils.COLOR_BG_CARD);
        return sp;
    }

    /** 统一样式进度条 */
    public static JProgressBar progressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setPreferredSize(new Dimension(120, 18));
        pb.setStringPainted(true);
        pb.setFont(ThemeUtils.FONT_SMALL);
        return pb;
    }

    // ================================================================
    //  全局样式
    // ================================================================

    /**
     * 应用全局默认字体到 UIManager，让未显式设置的组件继承统一字体。
     * 在 FlatLaf 初始化后调用一次。
     */
    public static void applyGlobalStyles() {
        UIManager.put("Button.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("Label.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("TextField.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("TextArea.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("PasswordField.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("ComboBox.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("Table.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("TableHeader.font", ThemeUtils.FONT_SMALL_BOLD);
        UIManager.put("List.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("Menu.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("MenuItem.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("TabbedPane.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("CheckBox.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("RadioButton.font", ThemeUtils.FONT_NORMAL);
        UIManager.put("ProgressBar.font", ThemeUtils.FONT_SMALL);
    }
}
