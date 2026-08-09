package com.sunzh.ui;

import com.sunzh.comparison.ComparisonDialog;
import com.sunzh.core.DataSourceStore;
import com.sunzh.datasource.DataSourceDialog;
import com.sunzh.inspection.InspectionDialog;
import com.sunzh.scriptrunner.ScriptRunnerDialog;
import com.sunzh.ssh.SshBackupDialog;
import com.sunzh.stats.StatsQueryDialog;
import com.sunzh.sync.DataSyncDialog;
import com.sunzh.ui.components.StatusBar;
import com.sunzh.ui.dialogs.ObjectQueryDialog;
import com.sunzh.ui.dialogs.SchemaCompareDialog;
import com.sunzh.ui.dialogs.SettingsDialog;
import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;

// ========== 新增：数据质量规则引擎对话框 ==========
import com.sunzh.datacheck.RuleConfigDialog;
import com.sunzh.datacheck.GenerateScriptDialog;
import com.sunzh.datacheck.ExecuteBatchDialog;

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
        // 原生标题栏文字不含 emoji（Windows 标题栏字体无彩色 emoji 字形，会显示为方框），
        // 图标统一由 applyWindowIcon 渲染的窗口图标提供（标题栏左上角 + 任务栏）。
        // setTitle 被注释以去掉原生标题栏文字与顶栏品牌区的重复（见 createHeaderContent）；
        // 居中由 applyAdaptiveSize() 末尾的 setLocationRelativeTo(null) 完成。
//        setTitle("资源管控中心 · 数据库运维管理平台v1.0");
        // 关键：关闭主窗口必须退出程序，否则进程会残留（只能在任务管理器结束）
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLocationRelativeTo(null);

        SvgIconUtils.applyWindowIcon(this);
        store = new DataSourceStore();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        ((JPanel) getContentPane()).setBackground(ThemeUtils.COLOR_BG);

        // 顶栏一体化：深海军蓝渐变下"品牌区 + 菜单条"融为一体，避免白色菜单条与深色表头脱节
        add(createTopBar(), BorderLayout.NORTH);
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

    // ---- 顶部整体：深海军蓝渐变（品牌区 + 菜单条一体） ----
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // 表头渐变统一走主题色板（暗色磨砂海军蓝）
                GradientPaint gp = new GradientPaint(0, 0, ThemeUtils.COLOR_HEADER_BG_START,
                        w, 0, ThemeUtils.COLOR_HEADER_BG_END);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                // 底部分隔线
                g2d.setColor(new Color(130, 170, 210, 60));
                g2d.drawLine(0, h - 1, w, h - 1);
            }
        };
        topBar.add(createHeaderContent(), BorderLayout.NORTH);
        topBar.add(createMenuBar(), BorderLayout.SOUTH);
        return topBar;
    }

    // ---- 顶部品牌区（透明，渐变由 createTopBar 绘制） ----
    private JPanel createHeaderContent() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 52));
        header.setBorder(BorderFactory.createEmptyBorder(6, 22, 2, 22));

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
        // 右上角原"SunZH / 用户名"状态区已按需移除，保持素雅
        return header;
    }

    // ---- 菜单条（融入顶栏渐变：浅色文字 + 顶层图标 + 主题化下拉菜单） ----
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        // 透出顶栏海军蓝渐变
        menuBar.setOpaque(false);
        menuBar.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 16));

        // 字体与全局一致：顶层菜单用粗体14，下拉项用常规14
        Font menuFont = ThemeUtils.FONT_BOLD;
        Font itemFont = ThemeUtils.FONT_NORMAL;

        // —— 文件 ——
        JMenu fileMenu = buildTopMenu("文件", "folder-open", menuFont);
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setFont(itemFont);
        exitItem.setIcon(SvgIconUtils.get("power", 16, ThemeUtils.COLOR_PRIMARY));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // —— 功能 ——
        JMenu funcMenu = buildTopMenu("功能", "apps", menuFont);

        // 下拉菜单项：图标统一用主题主色，素雅一致
        JMenuItem dsItem = new JMenuItem("1.数据源配置");
        dsItem.setFont(itemFont);
        dsItem.setIcon(SvgIconUtils.get("shujuyuanpeizhi", 16, ThemeUtils.COLOR_PRIMARY));
        dsItem.addActionListener(e -> openDataSourceDialog());
        funcMenu.add(dsItem);
        funcMenu.addSeparator();

        JMenuItem compareItem = new JMenuItem("2.Gausdb数据库结构对比平台");
        compareItem.setFont(itemFont);
        compareItem.setIcon(SvgIconUtils.get("bijiao", 16, ThemeUtils.COLOR_PRIMARY));
        compareItem.addActionListener(e -> openComparisonDialog());
        funcMenu.add(compareItem);

        JMenuItem syncItem = new JMenuItem("3.跨平台数据库数据同步平台");
        syncItem.setFont(itemFont);
        syncItem.setIcon(SvgIconUtils.get("tongbu-4", 16, ThemeUtils.COLOR_PRIMARY));
        syncItem.addActionListener(e -> openDataSyncDialog());
        funcMenu.add(syncItem);

        JMenuItem sqlItem = new JMenuItem("4.SQL脚本执行引擎");
        sqlItem.setFont(itemFont);
        sqlItem.setIcon(SvgIconUtils.get("SQLjiaoben", 16, ThemeUtils.COLOR_PRIMARY));
        sqlItem.addActionListener(e -> openScriptRunnerDialog());
        funcMenu.add(sqlItem);

        funcMenu.addSeparator();

        JMenuItem checkItem = new JMenuItem("5.Gausdb数据库巡检平台");
        checkItem.setFont(itemFont);
        checkItem.setIcon(SvgIconUtils.get("xunjian", 16, ThemeUtils.COLOR_PRIMARY));
        checkItem.addActionListener(e -> openCheckModelDialog());
        funcMenu.add(checkItem);

        JMenuItem objectQueryItem = new JMenuItem("6.Gausdb数据库对象查询");
        objectQueryItem.setFont(itemFont);
        objectQueryItem.setIcon(SvgIconUtils.get("shujuchaxun", 16, ThemeUtils.COLOR_PRIMARY));
        objectQueryItem.addActionListener(e -> openObjectQueryDialog());
        funcMenu.add(objectQueryItem);

        JMenuItem statsItem = new JMenuItem("7.Gausdb自定义查询");
        statsItem.setFont(itemFont);
        statsItem.setIcon(SvgIconUtils.get("shujuchaxun-6", 16, ThemeUtils.COLOR_PRIMARY));
        statsItem.addActionListener(e -> openStatsQueryDialog());
        funcMenu.add(statsItem);

        funcMenu.addSeparator();

        // 第8项：GaussDB数据库备份
        JMenuItem sshBackupItem = new JMenuItem("8.GaussDB数据库备份");
        sshBackupItem.setFont(itemFont);
        sshBackupItem.setIcon(SvgIconUtils.get("backup", 16, ThemeUtils.COLOR_PRIMARY));
        sshBackupItem.addActionListener(e -> openSshBackupDialog());
        funcMenu.add(sshBackupItem);

        // ========== 数据质量规则引擎（子菜单） ==========
        funcMenu.addSeparator();
        // 子菜单出现在白色下拉面板里，须与普通菜单项一致（深色文字 + 主色图标），
        // 不能用 buildTopMenu —— 那是给深蓝顶栏用的浅色文字，白底上会看不清。
        JMenu qualityMenu = new JMenu("9.数据质量");
        qualityMenu.setFont(itemFont);
        qualityMenu.setForeground(ThemeUtils.COLOR_TEXT);
        qualityMenu.setIcon(SvgIconUtils.get("check", 16, ThemeUtils.COLOR_PRIMARY));
        qualityMenu.setIconTextGap(8);
        // FlatLaf 子菜单箭头默认取 UIManager 的 Menu.icon.arrowColor（暗色主题下为浅色），
        // 白底上不可见，这里显式置为深色。注意：键名必须用箭头图标的前缀键 icon.arrowColor，
        // 悬停色用 selectionForeground（会同时作用于箭头图标与菜单项选中前景），
        // 直接写 arrowColor / arrowSelectionColor 会被 FlatLaf 判为未知样式并抛异常。
        String menuColor = String.format("#%06x", ThemeUtils.COLOR_TEXT.getRGB() & 0xFFFFFF);
        qualityMenu.putClientProperty("FlatLaf.style",
                "icon.arrowColor: " + menuColor + "; selectionForeground: " + menuColor);

        JMenuItem configItem = new JMenuItem("1.检查和清洗规则配置");
        configItem.setFont(itemFont);
        configItem.setIcon(SvgIconUtils.get("settings", 16, ThemeUtils.COLOR_PRIMARY));
        configItem.addActionListener(e -> openRuleConfigDialog());
        qualityMenu.add(configItem);

        JMenuItem genItem = new JMenuItem("2.针对表生成检查和数据清洗脚本");
        genItem.setFont(itemFont);
        genItem.setIcon(SvgIconUtils.get("file-code", 16, ThemeUtils.COLOR_PRIMARY));
        genItem.addActionListener(e -> openGenerateScriptDialog());
        qualityMenu.add(genItem);

        JMenuItem execItem = new JMenuItem("3.执行检查和数据清洗");
        execItem.setFont(itemFont);
        execItem.setIcon(SvgIconUtils.get("play", 16, ThemeUtils.COLOR_PRIMARY));
        execItem.addActionListener(e -> openExecuteBatchDialog());
        qualityMenu.add(execItem);

        funcMenu.add(qualityMenu);
        // =========================================

        menuBar.add(funcMenu);

        // —— 帮助 ——
        JMenu helpMenu = buildTopMenu("帮助", "zoom-question", menuFont);
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.setFont(itemFont);
        aboutItem.setIcon(SvgIconUtils.get("info-circle", 16, ThemeUtils.COLOR_PRIMARY));
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /** 顶层菜单：浅色文字 + 浅色小图标（hover 时由主题呈现磨砂高亮） */
    private JMenu buildTopMenu(String text, String iconName, Font font) {
        JMenu menu = new JMenu(text);
        menu.setFont(font);
        menu.setForeground(ThemeUtils.COLOR_HEADER_TEXT);
        menu.setIcon(SvgIconUtils.get(iconName, 16, new Color(198, 216, 238)));
        menu.setIconTextGap(8);
        menu.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return menu;
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
                // 首页背景渐变走主题色板（素雅冷灰蓝）
                GradientPaint bgGp = new GradientPaint(0, 0, ThemeUtils.COLOR_BG,
                        w, h, ThemeUtils.COLOR_BG_ALTERNATE);
                g2d.setPaint(bgGp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(28, 60, 28, 60)));
        // 固定卡片尺寸：宽 600（内容区 478，可排 3 个按钮/行 × 2 行），
        // 高度 420 保证在最小窗口（900×650）下也完整可见，最下行图标不再被截断。
        // 不能只设 maximumSize：GridBagLayout 布局时按 preferredSize 取宽，
        // 默认窗口下卡片会按 960（单行 6 按钮）溢出，最右侧被截断。
        card.setPreferredSize(new Dimension(600, 420));
        card.setMaximumSize(new Dimension(600, 420));

        JLabel bigIcon = new JLabel(SvgIconUtils.get("monitor", 36, ThemeUtils.COLOR_PRIMARY));
        bigIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(bigIcon);
        card.add(Box.createVerticalStrut(14));

        // 平台名已显示在顶部品牌区，这里只写"欢迎使用"，避免重复
        JLabel welcome = new JLabel("欢迎使用", JLabel.CENTER);
        welcome.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        welcome.setForeground(ThemeUtils.COLOR_TEXT);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(welcome);
        card.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("请从[功能菜单]选择要使用的工具", JLabel.CENTER);
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        hint.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(24));

        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
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
        btn.setBackground(ThemeUtils.COLOR_BG_CARD);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        btn.setPreferredSize(new Dimension(146, 86));
        btn.setMaximumSize(new Dimension(146, 86));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(desc);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeUtils.COLOR_PRIMARY_SOFT);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_PRIMARY, 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeUtils.COLOR_BG_CARD);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            }
        });

        ImageIcon svgIcon = SvgIconUtils.get(iconName, 20, ThemeUtils.COLOR_PRIMARY);
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
        // 平台名已显示在顶部品牌区，状态栏保持简洁的"就绪"，避免重复
        statusBar.setLeftText("就绪");
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
        // JFrame 原生标题栏才有 最小化/最大化/关闭 三按钮（JDialog 在 Windows 上不显示）
        // + 手动模态：打开禁用主窗口，关闭恢复
        JFrame dialog = new JFrame("数据质量规则引擎");
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        SvgIconUtils.applyWindowIcon(dialog);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) { setEnabled(false); }
            public void windowClosed(java.awt.event.WindowEvent e) {
                setEnabled(true);
                if (getExtendedState() == JFrame.ICONIFIED) setExtendedState(JFrame.NORMAL);
                toFront();
                requestFocus();
            }
        });
        // 统一自适应大小 + 居中
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1200, (int) (screen.width * 0.85));
        int h = Math.min(800, (int) (screen.height * 0.85));
        dialog.setSize(w, h);
        dialog.setMinimumSize(new Dimension(Math.min(900, w), Math.min(600, h)));
        dialog.setLocationRelativeTo(this);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1.规则配置", new com.sunzh.datacheck.RuleConfigPanel());
        tabs.addTab("2.生成脚本", new com.sunzh.datacheck.GenerateScriptPanel());
        tabs.addTab("3.执行批次", new com.sunzh.datacheck.ExecuteBatchPanel());
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
                        "\n作者: 资源管控中心 · SunZh\n" +
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