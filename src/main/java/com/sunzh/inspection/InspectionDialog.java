package com.sunzh.inspection;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.utils.SvgIconUtils;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class InspectionDialog extends BaseDialog {
    // ---- 外部配置目录（JAR 同级 ./conf/inspection/）----
    private static final String EXTERNAL_CONFIG_DIR = "./conf/inspection/";
    private static final String CONFIG_FILE_NAME = "config.yaml";
    private static final String QUERY_DIR_NAME = "query";
    private static final String REPORTS_DIR_NAME = "reports";
    private static final String PREF_REPORTS_PATH = "reports_path";

    private JComboBox<DataSource> dataSourceCombo;
    private JTable taskTable;
    private TaskTableModel tableModel;
    private JTextArea sqlPreviewArea;
    private JTextArea logArea;
    private JButton startButton;
    private JButton refreshButton;
    private JButton editConfigButton;
    private JButton openQueryButton;
    private JButton clearLogButton;
    private JButton openReportsButton;
    private JProgressBar progressBar;

    private JList<String> reportList;
    private DefaultListModel<String> reportListModel;

    private JTextField outputPathField;
    private JButton browseOutputButton;

    private List<InspectionTask> tasks;
    private InspectionService service;

    // ---- 外部文件路径 ----
    private File externalConfigFile;    // ./conf/inspection/config.yaml
    private File externalQueryDir;      // ./conf/inspection/query/
    private File reportsDir;

    private Preferences prefs;

    // ★ 关键修复：确保 startupLogs 始终被初始化 ★
    private List<String> startupLogs = new ArrayList<>();

    public InspectionDialog(JFrame owner) {
        super(owner, "数据库巡检", "search");
    }

    @Override
    protected void initUI() {
        if (service == null) service = new InspectionService();
        if (tasks == null) tasks = new ArrayList<>();
        prefs = Preferences.userNodeForPackage(InspectionDialog.class);

        initPaths();
        initComponents();   // 先创建 UI，此时 logArea 已初始化

        // 安全地输出启动日志
        if (startupLogs != null && !startupLogs.isEmpty()) {
            for (String msg : startupLogs) {
                log(msg);
            }
            startupLogs.clear();
        }

        loadConfig();
        loadDataSources();
        loadReports();
    }

    /**
     * 初始化路径策略（与 StatsQueryDialog 一致）：
     * 1. 优先使用外部目录 ./conf/inspection/
     * 2. 首次运行时自动从 JAR 包复制默认模板到外部目录
     */
    private void initPaths() {
        // 外部配置目录
        externalConfigFile = new File(EXTERNAL_CONFIG_DIR + CONFIG_FILE_NAME);
        externalQueryDir = new File(EXTERNAL_CONFIG_DIR + QUERY_DIR_NAME);

        // 首次运行：自动从 classpath 复制默认配置到外部目录
        initExternalFromClasspath();

        // 报告目录（用户可自定义）
        String savedPath = prefs.get(PREF_REPORTS_PATH, null);
        if (savedPath != null && !savedPath.trim().isEmpty()) {
            reportsDir = new File(savedPath);
        } else {
            reportsDir = new File(EXTERNAL_CONFIG_DIR + REPORTS_DIR_NAME);
        }
        if (!reportsDir.exists()) reportsDir.mkdirs();
    }

    /**
     * 首次运行时，从 classpath 复制默认的 config.yaml 和 query/*.sql 到外部目录。
     * 如果外部目录已有文件则不覆盖（用户已自定义）。
     * 日志暂存到 startupLogs，待 UI 初始化后输出。
     */
    private void initExternalFromClasspath() {
        boolean copied = false;

        // 1. 复制 config.yaml
        if (!externalConfigFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)) {
                if (in != null) {
                    externalConfigFile.getParentFile().mkdirs();
                    Files.copy(in, externalConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    copied = true;
                    addStartupLog("📦 已从 JAR 复制默认配置: " + externalConfigFile.getAbsolutePath());
                }
            } catch (Exception e) {
                addStartupLog("⚠️ 复制 config.yaml 失败: " + e.getMessage());
            }
        }

        // 2. 复制 query 目录下的所有 SQL 文件
        if (!externalQueryDir.exists()) {
            externalQueryDir.mkdirs();
            try {
                String queryResourcePath = "query/";
                URL queryUrl = getClass().getClassLoader().getResource(queryResourcePath);
                if (queryUrl != null) {
                    File classpathQueryDir = new File(queryUrl.toURI());
                    File[] sqlFiles = classpathQueryDir.listFiles((dir, name) -> name.endsWith(".sql"));
                    if (sqlFiles != null) {
                        for (File sqlFile : sqlFiles) {
                            Path target = externalQueryDir.toPath().resolve(sqlFile.getName());
                            Files.copy(sqlFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                            copied = true;
                            addStartupLog("📦 已从 JAR 复制 SQL: " + target);
                        }
                    }
                }
            } catch (Exception e) {
                addStartupLog("⚠️ 复制 query 目录失败: " + e.getMessage());
            }
        }

        if (copied) {
            addStartupLog("📦 首次运行，已将默认配置复制到: " + EXTERNAL_CONFIG_DIR);
        }
    }

    private void addStartupLog(String msg) {
        if (startupLogs == null) {
            startupLogs = new ArrayList<>();
        }
        startupLogs.add(msg);
    }

    private void initComponents() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.setBackground(ThemeUtils.COLOR_BG);

        // ===== 顶部工具栏（白底卡片，两行）=====
        JPanel topCard = ThemeUtils.cardPanel();
        topCard.setLayout(new GridLayout(2, 1, 0, 12));

        // ---- 第一行：数据源 + 输出目录（图标标签 + 统一尺寸输入控件）----
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setBackground(ThemeUtils.COLOR_BG_CARD);
        row1.add(SvgIconUtils.label("database", "数据源:", 14, ThemeUtils.COLOR_TEXT_SECONDARY));
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setPreferredSize(new Dimension(170, ThemeUtils.INPUT_HEIGHT));
        row1.add(dataSourceCombo);
        row1.add(Box.createHorizontalStrut(12));
        row1.add(SvgIconUtils.label("output", "输出目录:", 14, ThemeUtils.COLOR_TEXT_SECONDARY));
        outputPathField = ThemeUtils.field(reportsDir.getAbsolutePath());
        outputPathField.setEditable(false);
        outputPathField.setPreferredSize(new Dimension(300, ThemeUtils.INPUT_HEIGHT));
        row1.add(outputPathField);
        browseOutputButton = SvgIconUtils.outlineButton("folder-open", "浏览...", ThemeUtils.COLOR_SECONDARY);
        browseOutputButton.addActionListener(e -> chooseOutputDirectory());
        row1.add(browseOutputButton);
        topCard.add(row1);

        // ---- 第二行：功能按钮（左侧次要操作，右侧主操作）----
        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setBackground(ThemeUtils.COLOR_BG_CARD);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBtns.setBackground(ThemeUtils.COLOR_BG_CARD);

        refreshButton = SvgIconUtils.outlineButton("refresh", "刷新配置", ThemeUtils.COLOR_SECONDARY);
        refreshButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                loadConfig();
                loadReports();
            });
        });
        leftBtns.add(refreshButton);

        editConfigButton = SvgIconUtils.outlineButton("edit", "编辑 config.yaml", ThemeUtils.COLOR_SECONDARY);
        editConfigButton.addActionListener(e -> editConfigFile());
        leftBtns.add(editConfigButton);

        openQueryButton = SvgIconUtils.outlineButton("folder-open", "打开 query", ThemeUtils.COLOR_SECONDARY);
        openQueryButton.addActionListener(e -> openFolder(externalQueryDir));
        leftBtns.add(openQueryButton);

        openReportsButton = SvgIconUtils.outlineButton("report", "打开报告", ThemeUtils.COLOR_SECONDARY);
        openReportsButton.addActionListener(e -> openFolder(reportsDir));
        leftBtns.add(openReportsButton);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setBackground(ThemeUtils.COLOR_BG_CARD);

        clearLogButton = SvgIconUtils.outlineButton("trash", "清空日志", ThemeUtils.COLOR_DANGER);
        clearLogButton.addActionListener(e -> { if (logArea != null) logArea.setText(""); });
        rightBtns.add(clearLogButton);

        startButton = SvgIconUtils.button("play", "开始巡检", ThemeUtils.COLOR_PRIMARY);
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startInspection());
        rightBtns.add(startButton);

        row2.add(leftBtns, BorderLayout.WEST);
        row2.add(rightBtns, BorderLayout.EAST);
        topCard.add(row2);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.setBorder(ThemeUtils.paddingBorder(10, 12, 4, 12));
        topWrap.add(topCard, BorderLayout.CENTER);
        mainContentPanel.add(topWrap, BorderLayout.NORTH);

        // ===== 中央：任务列表（主）+ SQL 预览（辅）=====
        // 任务列表占 68% 宽度，SQL 预览占 32%；两个区域均以白底卡片包裹
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerLocation(0.68);
        centerSplit.setResizeWeight(0.68);
        centerSplit.setBorder(null);

        tableModel = new TaskTableModel();
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(30);
        taskTable.setFont(ThemeUtils.FONT_SMALL);
        taskTable.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        taskTable.setSelectionBackground(ThemeUtils.COLOR_PRIMARY_SELECT);
        taskTable.setSelectionForeground(ThemeUtils.COLOR_TEXT);
        taskTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row >= 0 && row < tasks.size()) {
                String sql = tasks.get(row).getSql();
                sqlPreviewArea.setText(sql != null ? sql : "");
                sqlPreviewArea.setCaretPosition(0);
            }
        });

        // 状态列着色 + 交替行（列0是布尔复选框，走独立渲染器不受影响）
        taskTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ThemeUtils.COLOR_BG_CARD : ThemeUtils.COLOR_BG_ALTERNATE);
                    setForeground(ThemeUtils.COLOR_TEXT);
                    setFont(table.getFont());
                }
                setHorizontalAlignment(SwingConstants.LEFT);
                if (column == 3 && value != null) {
                    String status = value.toString();
                    switch (status) {
                        case "SUCCESS": setForeground(ThemeUtils.COLOR_SUCCESS); setFont(getFont().deriveFont(Font.BOLD)); break;
                        case "NO_DATA": setForeground(ThemeUtils.COLOR_INFO); break;
                        case "FAILED":  setForeground(ThemeUtils.COLOR_DANGER); setFont(getFont().deriveFont(Font.BOLD)); break;
                        case "SKIPPED": setForeground(ThemeUtils.COLOR_WARNING); break;
                        default:        setForeground(ThemeUtils.COLOR_TEXT_SECONDARY); break;
                    }
                }
                return c;
            }
        });

        JTableHeader header = taskTable.getTableHeader();
        header.setFont(ThemeUtils.FONT_SMALL_BOLD);
        header.setBackground(ThemeUtils.COLOR_TABLE_HEADER_BG);
        header.setForeground(ThemeUtils.COLOR_TABLE_HEADER_TEXT);
        header.setReorderingAllowed(false);

        TableColumnModel columnModel = taskTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(180);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(80);
        columnModel.getColumn(5).setPreferredWidth(240);

        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setMinimumSize(new Dimension(480, 220));
        tableScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER_LIGHT));

        JPanel taskCard = ThemeUtils.cardPanel();
        taskCard.setLayout(new BorderLayout(8, 8));
        taskCard.add(ThemeUtils.sectionHeader("list", "任务列表"), BorderLayout.NORTH);
        taskCard.add(tableScroll, BorderLayout.CENTER);
        taskCard.setMinimumSize(new Dimension(480, 240));

        sqlPreviewArea = new JTextArea();
        sqlPreviewArea.setEditable(false);
        sqlPreviewArea.setFont(ThemeUtils.FONT_LOG);
        sqlPreviewArea.setBackground(ThemeUtils.COLOR_BG_CARD);
        sqlPreviewArea.setForeground(ThemeUtils.COLOR_TEXT);
        sqlPreviewArea.setTabSize(4);
        sqlPreviewArea.setBorder(ThemeUtils.paddingBorder(8, 10, 8, 10));
        JScrollPane previewScroll = new JScrollPane(sqlPreviewArea);
        previewScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER_LIGHT));

        JPanel previewCard = ThemeUtils.cardPanel();
        previewCard.setLayout(new BorderLayout(8, 8));
        previewCard.add(ThemeUtils.sectionHeader("file-code", "SQL 预览"), BorderLayout.NORTH);
        previewCard.add(previewScroll, BorderLayout.CENTER);
        previewCard.setMinimumSize(new Dimension(280, 240));

        centerSplit.setLeftComponent(taskCard);
        centerSplit.setRightComponent(previewCard);
        mainContentPanel.add(centerSplit, BorderLayout.CENTER);

        // ===== 底部：进度 + 日志 + 报告（固定高度，左右并列）=====
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(ThemeUtils.paddingBorder(4, 12, 10, 12));
        bottomPanel.setPreferredSize(new Dimension(0, 245));
        bottomPanel.setMinimumSize(new Dimension(0, 180));

        progressBar = new JProgressBar(0, 1);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 24));
        bottomPanel.add(progressBar, BorderLayout.NORTH);

        // 日志与报告左右并列：日志为主（68%），报告列表为辅（32%）
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setDividerLocation(0.68);
        bottomSplit.setResizeWeight(0.68);
        bottomSplit.setBorder(null);

        // ---- 运行日志卡片（深色终端风格）----
        logArea = new JTextArea(4, 0);
        logArea.setEditable(false);
        logArea.setFont(ThemeUtils.FONT_LOG);
        logArea.setBackground(ThemeUtils.COLOR_LOG_BG);
        logArea.setForeground(ThemeUtils.COLOR_LOG_TEXT);
        logArea.setCaretColor(ThemeUtils.COLOR_LOG_TEXT);
        logArea.setBorder(ThemeUtils.paddingBorder(8, 10, 8, 10));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER_LIGHT));
        logScroll.setMinimumSize(new Dimension(320, 130));

        JPanel logCard = ThemeUtils.cardPanel();
        logCard.setLayout(new BorderLayout(8, 8));
        logCard.add(ThemeUtils.sectionHeader("terminal", "运行日志"), BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);
        logCard.setMinimumSize(new Dimension(320, 150));

        // ---- 已生成报告卡片 ----
        reportListModel = new DefaultListModel<>();
        reportList = new JList<>(reportListModel);
        reportList.setFont(ThemeUtils.FONT_SMALL);
        reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportList.setBackground(ThemeUtils.COLOR_BG_CARD);
        reportList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = reportList.getSelectedValue();
                    if (selected != null) {
                        File reportDir = new File(reportsDir, selected);
                        if (reportDir.exists() && reportDir.isDirectory()) {
                            openFolder(reportDir);
                        }
                    }
                }
            }
        });
        JScrollPane reportScroll = new JScrollPane(reportList);
        reportScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER_LIGHT));
        reportScroll.setMinimumSize(new Dimension(220, 130));

        JPanel reportCard = ThemeUtils.cardPanel();
        reportCard.setLayout(new BorderLayout(8, 8));
        reportCard.add(ThemeUtils.sectionHeader("report", "已生成的报告（双击打开）"), BorderLayout.NORTH);
        reportCard.add(reportScroll, BorderLayout.CENTER);
        reportCard.setMinimumSize(new Dimension(220, 150));

        bottomSplit.setLeftComponent(logCard);
        bottomSplit.setRightComponent(reportCard);
        bottomPanel.add(bottomSplit, BorderLayout.CENTER);

        mainContentPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // ===== 输出目录选择 =====
    private void chooseOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择巡检结果输出目录");
        if (reportsDir.exists()) {
            chooser.setCurrentDirectory(reportsDir);
        } else {
            chooser.setCurrentDirectory(new File(EXTERNAL_CONFIG_DIR));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null) {
                reportsDir = selected;
                if (!reportsDir.exists()) reportsDir.mkdirs();
                outputPathField.setText(reportsDir.getAbsolutePath());
                prefs.put(PREF_REPORTS_PATH, reportsDir.getAbsolutePath());
                loadReports();
                log("📂 输出目录已切换至: " + reportsDir.getAbsolutePath());
            }
        }
    }

    // ===== 加载报告列表 =====
    private void loadReports() {
        reportListModel.clear();
        if (reportsDir == null || !reportsDir.exists()) return;
        File[] subDirs = reportsDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) return;
        List<File> sorted = Arrays.stream(subDirs)
                .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                .collect(Collectors.toList());
        for (File f : sorted) {
            reportListModel.addElement(f.getName());
        }
    }

    // ===== 数据源加载 =====
    private void loadDataSources() {
        dataSourceCombo.removeAllItems();
        List<DataSource> sources = DataSourceStore.load();
        if (sources.isEmpty()) {
            dataSourceCombo.addItem(null);
            dataSourceCombo.setEnabled(false);
            startButton.setEnabled(false);
            return;
        }
        for (DataSource ds : sources) {
            dataSourceCombo.addItem(ds);
        }
        dataSourceCombo.setEnabled(true);
        startButton.setEnabled(tasks != null && !tasks.isEmpty());
    }

    // ===== 加载配置（优先外部，回退 classpath） =====
    private void loadConfig() {
        if (service == null) service = new InspectionService();
        if (tasks == null) tasks = new ArrayList<>();

        // 优先使用外部配置文件
        File activeConfigFile = externalConfigFile.exists() ? externalConfigFile : null;

        // 如果外部不存在，尝试从 classpath 加载
        if (activeConfigFile == null) {
            try (InputStream is = getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)) {
                if (is != null) {
                    File tempFile = File.createTempFile("config", ".yaml");
                    tempFile.deleteOnExit();
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    activeConfigFile = tempFile;
                }
            } catch (Exception e) {
                // 忽略
            }
        }

        if (activeConfigFile == null || !activeConfigFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "配置文件 " + CONFIG_FILE_NAME + " 不存在。\n" +
                    "请在 " + EXTERNAL_CONFIG_DIR + " 目录下创建，或确保 JAR 包内有默认配置。",
                    "错误", JOptionPane.ERROR_MESSAGE);
            tableModel.fireTableDataChanged();
            return;
        }

        try {
            File activeQueryDir = externalQueryDir.exists() ? externalQueryDir : null;
            if (activeQueryDir == null) {
                activeQueryDir = createTempQueryDirFromClasspath();
            }

            List<InspectionTask> loaded = service.loadTasks(activeConfigFile, activeQueryDir);
            tasks = (loaded != null) ? loaded : new ArrayList<>();
            tableModel.fireTableDataChanged();
            startButton.setEnabled(!tasks.isEmpty() && dataSourceCombo.getSelectedItem() != null);

            String sourceDesc = externalConfigFile.exists() ?
                    "外部配置: " + externalConfigFile.getAbsolutePath() :
                    "内置配置 (classpath)";
            log("✅ 加载配置成功，共 " + tasks.size() + " 个任务。来源: " + sourceDesc);
            sqlPreviewArea.setText("");
        } catch (Exception e) {
            tasks = new ArrayList<>();
            tableModel.fireTableDataChanged();
            JOptionPane.showMessageDialog(this,
                    "加载配置失败: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private File createTempQueryDirFromClasspath() throws IOException {
        File tempDir = Files.createTempDirectory("inspection_query_").toFile();
        tempDir.deleteOnExit();
        String resourcePath = "query/";
        URL queryUrl = getClass().getClassLoader().getResource(resourcePath);
        if (queryUrl != null) {
            try {
                File classpathQueryDir = new File(queryUrl.toURI());
                File[] sqlFiles = classpathQueryDir.listFiles((dir, name) -> name.endsWith(".sql"));
                if (sqlFiles != null) {
                    for (File sqlFile : sqlFiles) {
                        Path target = tempDir.toPath().resolve(sqlFile.getName());
                        Files.copy(sqlFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception e) {
                throw new IOException("无法从 classpath 复制 query 文件", e);
            }
        }
        return tempDir;
    }

    // ===== 编辑外部 config.yaml =====
    private void editConfigFile() {
        if (!externalConfigFile.exists()) {
            try {
                externalConfigFile.getParentFile().mkdirs();
                try (InputStream in = getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)) {
                    if (in != null) {
                        Files.copy(in, externalConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        log("📄 已从 JAR 复制默认配置到: " + externalConfigFile.getAbsolutePath());
                    } else {
                        externalConfigFile.createNewFile();
                    }
                } catch (Exception e) {
                    externalConfigFile.createNewFile();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "创建 config.yaml 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        try {
            Desktop.getDesktop().edit(externalConfigFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法打开编辑器: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openFolder(File folder) {
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                log("创建文件夹: " + folder.getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(this, "无法创建文件夹: " + folder.getAbsolutePath(), "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法打开文件夹: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Connection getConnection(DataSource ds) throws SQLException, ClassNotFoundException {
        if ("ORACLE".equalsIgnoreCase(ds.getType())) {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } else if ("GAUSSDB".equalsIgnoreCase(ds.getType())) {
            Class.forName("com.huawei.gaussdb.jdbc.Driver");
        } else {
            throw new SQLException("不支持的数据库类型: " + ds.getType());
        }
        String url = ds.buildUrl();
        String user = ds.getUser();
        String password = ds.getPassword();
        return DriverManager.getConnection(url, user, password);
    }

    // ===== 开始巡检 =====
    private void startInspection() {
        DataSource ds = (DataSource) dataSourceCombo.getSelectedItem();
        if (ds == null) {
            JOptionPane.showMessageDialog(this, "请选择有效的数据源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可执行的任务", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean hasEnabled = tasks.stream().anyMatch(InspectionTask::isEnabled);
        if (!hasEnabled) {
            JOptionPane.showMessageDialog(this, "没有启用的任务，请勾选至少一个任务。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setUIEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setMaximum((int) tasks.stream().filter(InspectionTask::isEnabled).count());

        for (InspectionTask t : tasks) {
            t.setStatus(InspectionTask.Status.PENDING);
            t.setOutputFileName(null);
            t.setRowCount(0);
            t.setErrorMessage(null);
        }
        tableModel.fireTableDataChanged();

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Connection conn = null;
                try {
                    conn = getConnection(ds);
                    if (conn == null) {
                        publish("❌ 无法获取数据库连接");
                        return null;
                    }
                    publish("✅ 数据库连接成功");

                    InspectionService.ProgressListener listener = new InspectionService.ProgressListener() {
                        @Override
                        public void onTaskStart(InspectionTask task, int index, int total) {
                            publish(String.format("▶ [%d/%d] %s", index, total, task.getDescription()));
                        }

                        @Override
                        public void onTaskComplete(InspectionTask task, long elapsedSeconds) {
                            String statusStr;
                            switch (task.getStatus()) {
                                case SUCCESS:
                                    statusStr = String.format("✓ 成功 (记录数: %d, 文件: %s)", task.getRowCount(), task.getOutputFileName());
                                    break;
                                case NO_DATA:
                                    statusStr = "ℹ 无数据";
                                    break;
                                case FAILED:
                                    statusStr = "✗ 失败: " + task.getErrorMessage();
                                    break;
                                case SKIPPED:
                                    statusStr = "⏭ 跳过 (未启用)";
                                    break;
                                default: statusStr = "未知";
                            }
                            publish(String.format("  ⏱ %ds → %s", elapsedSeconds, statusStr));
                        }

                        @Override
                        public void onLog(String message) {
                            publish("[LOG] " + message);
                        }

                        @Override
                        public void onProgress(int current, int total) {
                            setProgress((current * 100) / total);
                        }

                        @Override
                        public void onFinished(int totalSuccess, int totalNoData, int totalFailed, int totalSkipped, long totalSeconds) {
                            publish("\n========== 执行完毕 ==========");
                            publish(String.format("成功: %d, 无数据: %d, 失败: %d, 跳过: %d, 总耗时: %d 秒",
                                    totalSuccess, totalNoData, totalFailed, totalSkipped, totalSeconds));
                        }
                    };

                    service.runInspection(conn, tasks, reportsDir, listener);

                } catch (ClassNotFoundException | SQLException e) {
                    publish("❌ 连接失败: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    publish("❌ 执行出错: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) log(msg);
                tableModel.fireTableDataChanged();
            }

            @Override
            protected void done() {
                setUIEnabled(true);
                progressBar.setValue(progressBar.getMaximum());
                logArea.setCaretPosition(logArea.getDocument().getLength());
                loadReports();
                JOptionPane.showMessageDialog(InspectionDialog.this,
                        "巡检任务执行完毕，请查看日志和结果文件。",
                        "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });

        worker.execute();
    }

    // ===== UI 启用/禁用 =====
    private void setUIEnabled(boolean enabled) {
        dataSourceCombo.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        editConfigButton.setEnabled(enabled);
        openQueryButton.setEnabled(enabled);
        startButton.setEnabled(enabled);
        clearLogButton.setEnabled(enabled);
        openReportsButton.setEnabled(enabled);
        browseOutputButton.setEnabled(enabled);
        taskTable.setEnabled(enabled);

        SwingUtilities.invokeLater(() -> {
            for (Component comp : new Component[]{refreshButton, editConfigButton, openQueryButton,
                    startButton, clearLogButton, openReportsButton, browseOutputButton}) {
                comp.repaint();
                comp.revalidate();
            }
        });
    }

    private void log(String msg) {
        if (logArea != null) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            if (startupLogs == null) {
                startupLogs = new ArrayList<>();
            }
            startupLogs.add(msg);
        }
    }

    @Override
    public void refresh() {
        loadConfig();
        loadDataSources();
        loadReports();
        outputPathField.setText(reportsDir.getAbsolutePath());
    }

    // ===== 表格模型 =====
    private class TaskTableModel extends AbstractTableModel {
        private final String[] columns = {"启用", "描述", "SQL文件", "状态", "记录数", "输出文件"};

        @Override
        public int getRowCount() {
            return (tasks == null) ? 0 : tasks.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return (col == 0) ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (tasks == null || row < 0 || row >= tasks.size()) return null;
            InspectionTask task = tasks.get(row);
            switch (col) {
                case 0: return task.isEnabled();
                case 1: return task.getDescription();
                case 2: return task.getSqlFile() != null ? task.getSqlFile() : "(内嵌SQL)";
                case 3: return task.getStatus().name();
                case 4: return task.getRowCount() > 0 ? task.getRowCount() : "";
                case 5: return task.getOutputFileName() != null ? task.getOutputFileName() : "";
                default: return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0 && tasks != null && row >= 0 && row < tasks.size()) {
                tasks.get(row).setEnabled((Boolean) value);
                fireTableCellUpdated(row, col);
                boolean anyEnabled = tasks.stream().anyMatch(InspectionTask::isEnabled);
                if (dataSourceCombo != null) {
                    startButton.setEnabled(anyEnabled && dataSourceCombo.getSelectedItem() != null);
                }
            }
        }
    }
}