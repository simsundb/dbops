package com.sunzh.ui;

import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * 功能对话框基类 — 自定义标题栏（岩系冷调）
 * 所有功能对话框继承此类，统一管理窗口行为和外观
 * 子类调用 super(owner, title, iconName) 设置标题和图标
 */
public abstract class BaseDialog extends JDialog {
    protected JFrame owner;
    private String titleText;
    private String iconName;
    protected JPanel mainContentPanel;

    public BaseDialog(JFrame owner, String title) {
        this(owner, title, null);
    }

    public BaseDialog(JFrame owner, String title, String iconName) {
        // 非模态（modalityType = MODELESS）：Windows 上模态 JDialog 即使可调整大小
        // 也没有最大化按钮。这里改用"手动模态"（见下方 WindowListener），
        // 既保留"打开时不能操作主窗口"的体验，又获得最小化/最大化/关闭按钮。
        super(owner, title, false);
        this.owner = owner;
        this.titleText = title;
        this.iconName = iconName;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        // 使用原生窗口装饰：标题栏自带 最小化 / 最大化 / 关闭 按钮
        setUndecorated(false);

        // 应用窗口图标（标题栏左上角 + 任务栏）
        SvgIconUtils.applyWindowIcon(this);

        // 手动模态：打开时禁用主窗口，关闭（含点击X/Dispose）时恢复
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (owner != null) owner.setEnabled(false);
            }
            @Override
            public void windowClosed(WindowEvent e) {
                if (owner != null) owner.setEnabled(true);
            }
        });

        buildRootPanel();
        initUI();
        applyAdaptiveSize();
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    /**
     * 统一窗口默认大小：自适应屏幕，小屏不溢出，大屏统一封顶。
     * 所有继承 BaseDialog 的窗口默认居中（构造器末尾 setLocationRelativeTo(owner)）。
     */
    private void applyAdaptiveSize() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1200, (int) (screen.width * 0.85));
        int h = Math.min(800, (int) (screen.height * 0.85));
        setSize(w, h);
        setMinimumSize(new Dimension(Math.min(900, w), Math.min(600, h)));
    }

    private void buildRootPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeUtils.COLOR_BG);

        // 自定义标题栏
        root.add(createTitleBar(), BorderLayout.NORTH);

        // 内容区域（留给子类填充）
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        root.add(mainContentPanel, BorderLayout.CENTER);

        // 外边框阴影效果
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder()));

        setContentPane(root);
    }

    // ---- 自定义标题栏 ----
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(
                        0, 0, ThemeUtils.COLOR_HEADER_BG_START,
                        w, 0, ThemeUtils.COLOR_HEADER_BG_END);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.drawLine(0, h - 1, w, h - 1);
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 48));
        titleBar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 4));

        // 左侧占位（保持标题居中）
        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(new Dimension(40, 46));
        titleBar.add(leftSpacer, BorderLayout.WEST);

        // 中央：图标 + 标题（完全垂直水平居中）
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints cb = new GridBagConstraints();
        cb.gridx = 0;
        cb.gridy = 0;
        cb.anchor = GridBagConstraints.CENTER;
        cb.insets = new Insets(0, 0, 0, 8);

        if (iconName != null && !iconName.trim().isEmpty()) {
            JLabel iconLbl = new JLabel(SvgIconUtils.get(iconName, 20, new Color(210, 224, 240)));
            iconLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            centerPanel.add(iconLbl, cb);
            cb.insets = new Insets(0, 0, 0, 0);
            cb.gridx = 1;
        }

        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(ThemeUtils.FONT_SUBTITLE);
        titleLbl.setForeground(ThemeUtils.COLOR_HEADER_TEXT);
        centerPanel.add(titleLbl, cb);

        titleBar.add(centerPanel, BorderLayout.CENTER);

        // 右侧占位（与左侧等宽，保持标题完全居中）——关闭/最小化/最大化由系统标题栏提供
        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(new Dimension(40, 46));
        titleBar.add(rightSpacer, BorderLayout.EAST);

        return titleBar;
    }

    /** 窗口拖拽监听器 —— 原生窗口装饰下不再需要，已移除 */
    protected abstract void initUI();

    @Override
    public void setVisible(boolean visible) {
        if (visible) refresh();
        super.setVisible(visible);
    }

    public void refresh() {
        // 子类可重写
    }
}
