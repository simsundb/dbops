package com.example.ui;

import com.example.utils.SvgIconUtils;
import com.example.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        super(owner, "", true);
        this.owner = owner;
        this.titleText = title;
        this.iconName = iconName;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setUndecorated(true);

        buildRootPanel();
        initUI();                      // 子类构建界面
        applyAdaptiveSize();           // ★ 自适应屏幕尺寸 + 最小尺寸
        setLocationRelativeTo(owner);
        setResizable(true);            // 允许用户调整大小
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

    // ---- 自适应屏幕尺寸 ----
    private void applyAdaptiveSize() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screen.width * 0.8);
        int height = (int) (screen.height * 0.8);
        setSize(width, height);
        // 设置最小尺寸，防止用户拖拽过小导致界面错乱
        setMinimumSize(new Dimension(900, 650));
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
                GradientPaint gp = new GradientPaint(0, 0, new Color(50, 74, 97), w, 0, new Color(72, 102, 132));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.setColor(new Color(130, 170, 210, 50));
                g2d.drawLine(0, h - 1, w, h - 1);
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 46));
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
            JLabel iconLbl = new JLabel(SvgIconUtils.get(iconName, 20, new Color(200, 218, 235)));
            iconLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            centerPanel.add(iconLbl, cb);
            cb.insets = new Insets(0, 0, 0, 0);
            cb.gridx = 1;
        }

        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        titleLbl.setForeground(new Color(232, 240, 248));
        centerPanel.add(titleLbl, cb);

        titleBar.add(centerPanel, BorderLayout.CENTER);

        // 右侧：关闭按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JButton closeBtn = new JButton();
        closeBtn.setPreferredSize(new Dimension(32, 32));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setIcon(createCloseIcon(16, new Color(190, 208, 225)));
        closeBtn.setRolloverIcon(createCloseIcon(16, new Color(245, 100, 95)));
        closeBtn.addActionListener(e -> dispose());
        rightPanel.add(closeBtn);

        titleBar.add(rightPanel, BorderLayout.EAST);

        // 拖动窗口（整个标题栏）
        DragListener dl = new DragListener();
        titleBar.addMouseListener(dl);
        titleBar.addMouseMotionListener(dl);

        return titleBar;
    }

    /** 绘制关闭按钮的 X 图标 */
    private ImageIcon createCloseIcon(int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2));
        g.setColor(color);
        int p = 3;
        g.drawLine(p, p, size - p, size - p);
        g.drawLine(size - p, p, p, size - p);
        g.dispose();
        return new ImageIcon(img);
    }

    /** 窗口拖拽监听器 */
    private class DragListener extends MouseAdapter {
        private int startX, startY;
        @Override
        public void mousePressed(MouseEvent e) {
            startX = e.getX();
            startY = e.getY();
        }
        @Override
        public void mouseDragged(MouseEvent e) {
            Point p = getLocation();
            setLocation(p.x + e.getX() - startX, p.y + e.getY() - startY);
        }
    }

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