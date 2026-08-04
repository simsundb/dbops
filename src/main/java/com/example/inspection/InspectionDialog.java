package com.example.inspection;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.ui.BaseDialog;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
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
        mainContentPanel.setLayout(new BorderLayout(10, 10));

        // ===== 顶部工具栏 (两行) =====
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // ---- 第一行：数据源 + 输出目录 (左对齐) ----
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.add(new JLabel("数据源:"));
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setPreferredSize(new Dimension(150, 28));
        row1.add(dataSourceCombo);

        row1.add(new JLabel("输出目录:"));
        outputPathField = new JTextField(reportsDir.getAbsolutePath(), 20);
        outputPathField.setEditable(false);
        row1.add(outputPathField);

        browseOutputButton = new JButton("浏览...");
        browseOutputButton.addActionListener(e -> chooseOutputDirectory());
        row1.add(browseOutputButton);

        topPanel.add(row1);

        // ---- 第二行：所有功能按钮 (右对齐) ----
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        Dimension btnSize = new Dimension(115, 30);

        refreshButton = new JButton("🔄 刷新配置");
        refreshButton.setPreferredSize(btnSize);
        refreshButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                loadConfig();
                loadReports();
            });
        });
        row2.add(refreshButton);

        editConfigButton = new JButton("📝 编辑 config.yaml");
        editConfigButton.setPreferredSize(btnSize);
        editConfigButton.addActionListener(e -> editConfigFile());
        row2.add(editConfigButton);

        openQueryButton = new JButton("📂 打开 query");
        openQueryButton.setPreferredSize(btnSize);
        openQueryButton.addActionListener(e -> openFolder(externalQueryDir));
        row2.add(openQueryButton);

        // 分隔
        row2.add(Box.createHorizontalStrut(20));

        startButton = new JButton("▶ 开始巡检");
        startButton.setPreferredSize(btnSize);
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startInspection());
        row2.add(startButton);

        clearLogButton = new JButton("🗑 清空日志");
        clearLogButton.setPreferredSize(btnSize);
        clearLogButton.addActionListener(e -> { if (logArea != null) logArea.setText(""); });
        row2.add(clearLogButton);

        openReportsButton = new JButton("📁 打开报告");
        openReportsButton.setPreferredSize(btnSize);
        openReportsButton.addActionListener(e -> openFolder(reportsDir));
        row2.add(openReportsButton);

        topPanel.add(row2);
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        // ===== 中央：任务列表 + SQL预览 =====
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerLocation(480);
        centerSplit.setResizeWeight(0.42);

        tableModel = new TaskTableModel();
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(25);
        taskTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row >= 0 && row < tasks.size()) {
                String sql = tasks.get(row).getSql();
                sqlPreviewArea.setText(sql != null ? sql : "");
                sqlPreviewArea.setCaretPosition(0);
            }
        });

        TableColumnModel columnModel = taskTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(55);
        columnModel.getColumn(1).setPreferredWidth(260);
        columnModel.getColumn(2).setPreferredWidth(160);
        columnModel.getColumn(3).setPreferredWidth(90);
        columnModel.getColumn(4).setPreferredWidth(75);
        columnModel.getColumn(5).setPreferredWidth(220);
        taskTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createTitledBorder("任务列表"));

        sqlPreviewArea = new JTextArea();
        sqlPreviewArea.setEditable(false);
        sqlPreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        sqlPreviewArea.setTabSize(4);
        JScrollPane previewScroll = new JScrollPane(sqlPreviewArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("SQL 预览"));

        centerSplit.setLeftComponent(tableScroll);
        centerSplit.setRightComponent(previewScroll);
        mainContentPanel.add(centerSplit, BorderLayout.CENTER);

        // ===== 底部：进度 + 日志 + 报告列表 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 1);
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(progressPanel, BorderLayout.NORTH);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bottomSplit.setDividerLocation(155);
        bottomSplit.setResizeWeight(0.58);

        logArea = new JTextArea(6, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("运行日志"));
        bottomSplit.setTopComponent(logScroll);

        reportListModel = new DefaultListModel<>();
        reportList = new JList<>(reportListModel);
        reportList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        reportScroll.setBorder(BorderFactory.createTitledBorder("已生成的报告（双击打开目录）"));
        bottomSplit.setBottomComponent(reportScroll);

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