package com.example.ui;

import com.example.comparison.ComparisonDialog;
import com.example.core.DataSourceStore;
import com.example.datasource.DataSourceDialog;
import com.example.inspection.InspectionDialog;
import com.example.scriptrunner.ScriptRunnerDialog;
import com.example.ssh.SshBackupDialog;
import com.example.stats.StatsQueryDialog;
import com.example.sync.DataSyncDialog;
import com.example.ui.components.StatusBar;
import com.example.ui.dialogs.ObjectQueryDialog;
import com.example.ui.dialogs.SchemaCompareDialog;
import com.example.ui.dialogs.SettingsDialog;
import com.example.utils.SvgIconUtils;
import com.example.utils.ThemeUtils;

// ========== 新增：数据质量规则引擎对话框 ==========
import com.example.datacheck.RuleConfigDialog;
import com.example.datacheck.GenerateScriptDialog;
import com.example.datacheck.ExecuteBatchDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 主窗口
 * 包含标题栏、菜单栏、状态栏
 * 所有功能通过菜单打开独立对话框
 */
public class MainFrame extends JFrame {
    private StatusBar statusBar;
    private DataSourceStore store;

    public MainFrame() {
        setTitle("🖥️ 资源管控中心 · 数据库运维管理平台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        store = new DataSourceStore();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        ((JPanel) getContentPane()).setBackground(ThemeUtils.COLOR_BG);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMenuBar(), BorderLayout.BEFORE_FIRST_LINE);
        add(createContentPanel(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        applyAdaptiveSize();
    }

    /** 自适应屏幕尺寸 */
    private void applyAdaptiveSize() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screen.width * 0.8);
        int height = (int) (screen.height * 0.8);
        setSize(width, height);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setResizable(true);
    }

    // ---- 顶部标题栏 ----
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(48, 72, 95), w, 0, new Color(70, 100, 130));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.setColor(new Color(130, 170, 210, 60));
                g2d.drawLine(0, h - 1, w, h - 1);
            }
        };
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(SvgIconUtils.getWhite("monitor", 24));
        leftPanel.add(iconLabel);

        JLabel titleLabel = new JLabel("资源管控中心");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        titleLabel.setForeground(new Color(235, 242, 250));
        leftPanel.add(titleLabel);

        JSeparator vSep = new JSeparator(JSeparator.VERTICAL);
        vSep.setForeground(new Color(255, 255, 255, 50));
        vSep.setPreferredSize(new Dimension(1, 22));
        leftPanel.add(vSep);

        JLabel subtitleLabel = new JLabel("数据库运维管理平台");
        subtitleLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(180, 200, 220));
        leftPanel.add(subtitleLabel);

        JLabel versionLabel = new JLabel("v1.0");
        versionLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(160, 185, 210));
        versionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                BorderFactory.createEmptyBorder(1, 8, 1, 8)));
        leftPanel.add(versionLabel);

        header.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        JPanel dotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(120, 200, 160));
                g2d.fillOval(1, 1, 8, 8);
                g2d.setColor(new Color(120, 200, 160, 100));
                g2d.fillOval(-1, -1, 12, 12);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(12, 12); }
        };
        dotPanel.setOpaque(false);
        rightPanel.add(dotPanel);

        JLabel statusLabel = new JLabel("系统就绪");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 215, 230));
        rightPanel.add(statusLabel);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(new Color(255, 255, 255, 40));
        sep.setPreferredSize(new Dimension(1, 18));
        rightPanel.add(sep);

        JLabel timeLabel = new JLabel("👤 " + System.getProperty("user.name"));
        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(190, 205, 220));
        rightPanel.add(timeLabel);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ---- 菜单栏（字体加粗14px，图标16px） ----
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(ThemeUtils.COLOR_MENU_BG);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        Font menuFont = new Font("Microsoft YaHei", Font.BOLD, 14);
        Font itemFont = new Font("Microsoft YaHei", Font.PLAIN, 14);

        JMenu fileMenu = new JMenu("文件");
        fileMenu.setFont(menuFont);
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setFont(itemFont);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu funcMenu = new JMenu("功能");
        funcMenu.setFont(menuFont);

        // 所有图标尺寸 16px，保留彩色
        JMenuItem dsItem = new JMenuItem("1.数据源配置");
        dsItem.setFont(itemFont);
        dsItem.setIcon(SvgIconUtils.get("shujuyuanpeizhi", 16, null));
        dsItem.addActionListener(e -> openDataSourceDialog());
        funcMenu.add(dsItem);
        funcMenu.addSeparator();

        JMenuItem compareItem = new JMenuItem("2.Gausdb数据库结构对比平台");
        compareItem.setFont(itemFont);
        compareItem.setIcon(SvgIconUtils.get("bijiao", 16, null));
        compareItem.addActionListener(e -> openComparisonDialog());
        funcMenu.add(compareItem);

        JMenuItem syncItem = new JMenuItem("3.跨平台数据库数据同步平台");
        syncItem.setFont(itemFont);
        syncItem.setIcon(SvgIconUtils.get("tongbu-4", 16, null));
        syncItem.addActionListener(e -> openDataSyncDialog());
        funcMenu.add(syncItem);

        JMenuItem sqlItem = new JMenuItem("4.SQL脚本执行引擎");
        sqlItem.setFont(itemFont);
        sqlItem.setIcon(SvgIconUtils.get("SQLjiaoben", 16, null));
        sqlItem.addActionListener(e -> openScriptRunnerDialog());
        funcMenu.add(sqlItem);

        funcMenu.addSeparator();

        JMenuItem checkItem = new JMenuItem("5.Gausdb数据库巡检平台");
        checkItem.setFont(itemFont);
        checkItem.setIcon(SvgIconUtils.get("xunjian", 16, null));
        checkItem.addActionListener(e -> openCheckModelDialog());
        funcMenu.add(checkItem);

        JMenuItem objectQueryItem = new JMenuItem("6.Gausdb数据库对象查询");
        objectQueryItem.setFont(itemFont);
        objectQueryItem.setIcon(SvgIconUtils.get("shujuchaxun", 16, null));
        objectQueryItem.addActionListener(e -> openObjectQueryDialog());
        funcMenu.add(objectQueryItem);

        JMenuItem statsItem = new JMenuItem("7.Gausdb自定义查询");
        statsItem.setFont(itemFont);
        statsItem.setIcon(SvgIconUtils.get("shujuchaxun-6", 16, null));
        statsItem.addActionListener(e -> openStatsQueryDialog());
        funcMenu.add(statsItem);

        funcMenu.addSeparator();

        // 新增第8项：GaussDB数据库备份
        JMenuItem sshBackupItem = new JMenuItem("8.GaussDB数据库备份");
        sshBackupItem.setFont(itemFont);
        sshBackupItem.setIcon(SvgIconUtils.get("backup", 16, null));
        sshBackupItem.addActionListener(e -> openSshBackupDialog());
        funcMenu.add(sshBackupItem);

        // ========== 新增：数据质量规则引擎 ==========
        funcMenu.addSeparator();
        JMenu qualityMenu = new JMenu("9.数据质量");
        qualityMenu.setFont(menuFont);
        qualityMenu.setIcon(SvgIconUtils.get("check", 16, null));

        JMenuItem configItem = new JMenuItem("规则配置");
        configItem.setFont(itemFont);
        configItem.setIcon(SvgIconUtils.get("settings", 16, null));
        configItem.addActionListener(e -> openRuleConfigDialog());
        qualityMenu.add(configItem);

        JMenuItem genItem = new JMenuItem("生成检查脚本");
        genItem.setFont(itemFont);
        genItem.setIcon(SvgIconUtils.get("file-code", 16, null));
        genItem.addActionListener(e -> openGenerateScriptDialog());
        qualityMenu.add(genItem);

        JMenuItem execItem = new JMenuItem("执行批次");
        execItem.setFont(itemFont);
        execItem.setIcon(SvgIconUtils.get("play", 16, null));
        execItem.addActionListener(e -> openExecuteBatchDialog());
        qualityMenu.add(execItem);

        funcMenu.add(qualityMenu);
        // =========================================

        menuBar.add(funcMenu);

        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setFont(menuFont);
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.setFont(itemFont);
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    // ---- 首页内容面板 ----
    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                GradientPaint bgGp = new GradientPaint(0, 0, new Color(242, 246, 252),
                        w, h, new Color(225, 232, 240));
                g2d.setPaint(bgGp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 230));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 200, 215, 150), 1),
                BorderFactory.createEmptyBorder(40, 60, 40, 60)));
        card.setMaximumSize(new Dimension(580, 480)); // 略微增高以容纳新按钮

        JLabel bigIcon = new JLabel(SvgIconUtils.get("monitor", 48, ThemeUtils.COLOR_PRIMARY));
        bigIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(bigIcon);
        card.add(Box.createVerticalStrut(18));

        JLabel welcome = new JLabel("欢迎使用资源管控中心·数据库运维管理平台", JLabel.CENTER);
        welcome.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        welcome.setForeground(ThemeUtils.COLOR_TEXT);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(welcome);
        card.add(Box.createVerticalStrut(10));

        JLabel hint = new JLabel("请从[功能菜单]选择要使用的工具", JLabel.CENTER);
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        hint.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(30));

        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        quickBtns.setOpaque(false);
        quickBtns.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ========== 快捷按钮定义（新增第7项：数据质量） ==========
        String[][] features = {
                {"settings", "数据源", "配置数据库源"},
                {"compare", "结构对比", "Gauss数据库结构对比:表/列/索引/序列/同义词"},
                {"transfer", "数据同步", "Excel数据入库和Oracle与Gauss跨库表数据同步"},
                {"file-code", "SQL执行", "数据库脚本x.sql脚本批量执行"},
                {"search", "数据库巡检", "Gauss数据库自定义指标巡检,并生成巡检报告"},
                {"backup", "数据库备份", "通过SSH远程备份GaussDB数据库"}
        };

        for (String[] f : features) {
            JPanel btn = createFeatureButton(f[0], f[1], f[2]);
            quickBtns.add(btn);
        }
        card.add(quickBtns);

        centerWrap.add(card);
        panel.add(centerWrap, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFeatureButton(String iconName, String title, String desc) {
        final JPanel btn = new JPanel();
        btn.setLayout(new BoxLayout(btn, BoxLayout.Y_AXIS));
        btn.setBackground(new Color(250, 252, 255));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(195, 205, 218), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        btn.setPreferredSize(new Dimension(150, 90));
        btn.setMaximumSize(new Dimension(150, 90));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(desc);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(230, 238, 248));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_PRIMARY, 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(250, 252, 255));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(195, 205, 218), 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            }
        });

        ImageIcon svgIcon = SvgIconUtils.get(iconName, 28, ThemeUtils.COLOR_PRIMARY);
        JLabel iconLbl = new JLabel(svgIcon);
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.add(iconLbl);

        // 统一处理快捷按钮点击事件
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                switch (title) {
                    case "数据源": openDataSourceDialog(); break;
                    case "结构对比": openComparisonDialog(); break;
                    case "数据同步": openDataSyncDialog(); break;
                    case "SQL执行": openScriptRunnerDialog(); break;
                    case "数据库巡检": openCheckModelDialog(); break;
                    case "数据库备份": openSshBackupDialog(); break;
                    case "数据质量": openQualitySummaryDialog(); break;   // 新增
                }
            }
        });

        btn.add(Box.createVerticalStrut(6));

        JLabel titleLbl = new JLabel(title, JLabel.CENTER);
        titleLbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        titleLbl.setForeground(ThemeUtils.COLOR_TEXT);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.add(titleLbl);
        btn.add(Box.createVerticalStrut(2));

        JLabel descLbl = new JLabel(desc, JLabel.CENTER);
        descLbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        descLbl.setForeground(ThemeUtils.COLOR_TEXT_HINT);
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.add(descLbl);

        return btn;
    }

    // ---- 底部状态栏 ----
    private JPanel createStatusBar() {
        statusBar = new StatusBar();
        statusBar.setLeftText("资源管控中心 · 数据库运维管理平台 | 系统就绪");
        return statusBar;
    }

    // ============================================================
    // 打开对话框方法
    // ============================================================

    private void openDataSourceDialog() { DataSourceDialog d = new DataSourceDialog(this); d.setVisible(true); }
    private void openSchemaCompareDialog() { SchemaCompareDialog d = new SchemaCompareDialog(this); d.setVisible(true); }
    private void openDataSyncDialog() { DataSyncDialog d = new DataSyncDialog(this); d.setVisible(true); }
    private void openScriptRunnerDialog() { ScriptRunnerDialog d = new ScriptRunnerDialog(this); d.setVisible(true); }
    private void openCheckModelDialog() { InspectionDialog d = new InspectionDialog(this); d.setVisible(true); }
    private void openComparisonDialog() { ComparisonDialog d = new ComparisonDialog(this); d.setVisible(true); }
    private void openSettingsDialog() { SettingsDialog d = new SettingsDialog(this); d.setVisible(true); }

    private void openObjectQueryDialog() {
        ObjectQueryDialog dialog = new ObjectQueryDialog(this);
        dialog.setVisible(true);
    }
    private void openStatsQueryDialog() {
        StatsQueryDialog dialog = new StatsQueryDialog(this);
        dialog.setVisible(true);
    }

    // 新增：打开 SSH 备份对话框
    private void openSshBackupDialog() {
        SshBackupDialog dialog = new SshBackupDialog(this);
        dialog.setVisible(true);
    }

    // ========== 新增：数据质量规则引擎对话框 ==========
    private void openRuleConfigDialog() {
        new RuleConfigDialog(this).setVisible(true);
    }
    private void openGenerateScriptDialog() {
        new GenerateScriptDialog(this).setVisible(true);
    }
    private void openExecuteBatchDialog() {
        new ExecuteBatchDialog(this).setVisible(true);
    }
    // 快捷入口汇总对话框（含三个标签页）
    private void openQualitySummaryDialog() {
        JDialog dialog = new JDialog(this, "数据质量规则引擎", true);
        dialog.setSize(1100, 750);
        dialog.setLocationRelativeTo(this);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1.规则配置", new com.example.datacheck.RuleConfigPanel());
        tabs.addTab("2.生成脚本", new com.example.datacheck.GenerateScriptPanel());
        tabs.addTab("3.执行批次", new com.example.datacheck.ExecuteBatchPanel());
        dialog.add(tabs);
        dialog.setVisible(true);
    }
    // ======================================================

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "资源管控中心 · 数据库运维管理平台\n" +
                        "版本: v1.0\n" +
                        "功能列表:\n" +
                        "  1.数据源配置\n" +
                        "  2.Gausdb数据库结构对比\n" +
                        "  3.跨平台数据同步\n" +
                        "  4.SQL脚本执行引擎\n" +
                        "  5.Gausdb数据库巡检\n" +
                        "  6.数据库对象查询\n" +
                        "  7.自定义查询统计\n" +
                        "  8.GaussDB数据库备份\n" +
                        "  9.数据质量规则引擎\n" +   // 新增
                        "\n作者: 资源管控中心 · SunZhiHui\n" +
                        "时间: 2026-08",
                "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}