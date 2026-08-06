package com.sunzh.ui;

import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 功能对话框基类 — 现代圆角 + 轻阴影风格
 * 所有功能对话框继承此类，统一管理窗口行为和外观
 * 子类调用 super(owner, title, iconName) 设置标题和图标
 */
public abstract class BaseDialog extends JDialog {

    protected JFrame owner;
    private final String titleText;
    private final String iconName;
    protected JPanel mainContentPanel;
    private final int cornerRadius = 20;          // 圆角大小
    private final int shadowSize = 8;             // 阴影尺寸

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
        applyAdaptiveSize();
        setLocationRelativeTo(owner);
        setResizable(true);

        // 使窗口透明（用于圆角 + 阴影）
        setBackground(new Color(0, 0, 0, 0));
        // 监听尺寸变化，更新圆角形状
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            }
        });
        // 初始形状
        SwingUtilities.invokeLater(() -> {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        });
    }

    private void buildRootPanel() {
        // 根面板：带阴影的圆角容器
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 绘制阴影（右下方向偏移）
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int shadow = shadowSize;
                int w = getWidth();
                int h = getHeight();
                // 绘制半透明阴影矩形（偏移 4px）
                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.fillRoundRect(shadow / 2, shadow / 2, w - shadow, h - shadow, cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(shadowSize / 2, shadowSize / 2, shadowSize / 2, shadowSize / 2));

        // 主内容面板（带圆角背景）
        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // 白色背景，带微弱渐变
                GradientPaint gp = new GradientPaint(0, 0, new Color(252, 253, 255),
                        0, h, new Color(245, 248, 250));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
                g2d.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 0, 0));

        // 标题栏
        content.add(createTitleBar(), BorderLayout.NORTH);

        // 内容区域（留给子类填充），设置内边距
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        mainContentPanel.setBorder(new EmptyBorder(12, 16, 16, 16));
        content.add(mainContentPanel, BorderLayout.CENTER);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ---- 自适应屏幕尺寸 ----
    private void applyAdaptiveSize() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screen.width * 0.8);
        int height = (int) (screen.height * 0.8);
        setSize(width, height);
        setMinimumSize(new Dimension(900, 650));
    }

    // ---- 自定义标题栏 ----
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // 渐变：深蓝到稍亮蓝（现代运维风格标题栏）
                GradientPaint gp = new GradientPaint(0, 0, ThemeUtils.COLOR_HEADER_BG,
                        0, h, ThemeUtils.COLOR_HEADER_BG.brighter());
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
                // 仅顶部圆角，底部不圆
                g2d.fillRect(0, cornerRadius / 2, w, h - cornerRadius / 2);
                // 底部细发光线条
                g2d.setColor(new Color(160, 190, 220, 80));
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawLine(10, h - 1, w - 10, h - 1);
                g2d.dispose();
            }
        };
        titleBar.setPreferredSize(new Dimension(0, 52));
        titleBar.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 4));
        titleBar.setOpaque(false);

        // 左侧占位（保持标题居中）
        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(new Dimension(40, 52));
        titleBar.add(leftSpacer, BorderLayout.WEST);

        // 中央：图标 + 标题
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints cb = new GridBagConstraints();
        cb.gridx = 0;
        cb.gridy = 0;
        cb.anchor = GridBagConstraints.CENTER;
        cb.insets = new Insets(0, 0, 0, 10);

        if (iconName != null && !iconName.trim().isEmpty()) {
            JLabel iconLbl = new JLabel(SvgIconUtils.get(iconName, 22, new Color(200, 218, 235)));
            iconLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            centerPanel.add(iconLbl, cb);
            cb.insets = new Insets(0, 0, 0, 0);
            cb.gridx = 1;
        }

        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(ThemeUtils.FONT_SUBTITLE);
        titleLbl.setForeground(ThemeUtils.COLOR_HEADER_TEXT);
        // 增加字距
        titleLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        centerPanel.add(titleLbl, cb);

        titleBar.add(centerPanel, BorderLayout.CENTER);

        // 右侧：关闭按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        JButton closeBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 30));
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                }
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(255, 255, 255, 50));
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                }
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        closeBtn.setPreferredSize(new Dimension(36, 36));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setIcon(createCloseIcon(18, new Color(190, 208, 225)));
        closeBtn.setRolloverIcon(createCloseIcon(18, new Color(245, 120, 115)));
        closeBtn.addActionListener(e -> dispose());
        rightPanel.add(closeBtn);

        titleBar.add(rightPanel, BorderLayout.EAST);

        // 拖拽监听
        DragListener dl = new DragListener();
        titleBar.addMouseListener(dl);
        titleBar.addMouseMotionListener(dl);

        return titleBar;
    }

    /** 绘制关闭按钮的 X 图标（更精致） */
    private ImageIcon createCloseIcon(int size, Color color) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        int p = 4;
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

    /** 子类必须实现此方法构建界面 */
    protected abstract void initUI();

    @Override
    public void setVisible(boolean visible) {
        if (visible) refresh();
        super.setVisible(visible);
    }

    /** 子类可重写，用于刷新数据 */
    public void refresh() {
        // 默认无操作
    }
}