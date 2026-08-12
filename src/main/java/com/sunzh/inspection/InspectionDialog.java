package com.sunzh.inspection;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.utils.ExternalConfigUtils;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class InspectionDialog extends BaseDialog {
    // ---- 外部配置目录（conf/inspection/，conf 根目录由 ExternalConfigUtils 智能定位），与 StatsQueryDialog 的 conf/stats 一致 ----
    private static final String EXTERNAL_CONFIG_DIR = ExternalConfigUtils.CONF_DIR + "inspection/";
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

    private File configFile;    // ./conf/inspection/config.yaml
    private File queryDir;      // ./conf/inspection/query/
    private File reportsDir;    // ./conf/inspection/reports/（用户可自定义）

    private Preferences prefs;

    // 首次复制默认模板的日志暂存，待 UI 初始化后输出（此时 logArea 还未创建）
    private List<String> startupLogs = new ArrayList<>();

    public InspectionDialog(JFrame owner) {
        super(owner, "数据库巡检", "search");
    }

    @Override
    protected void initUI() {
        // 重要：BaseDialog 构造器在 super() 期间调用本虚方法，此时本类字段初始化器尚未执行，
        // startupLogs 可能为 null（config.yaml 已存在时 addStartupLog 不会被调用）。
        // 这里必须显式初始化，否则下方 isEmpty() 抛 NPE。
        if (startupLogs == null) startupLogs = new ArrayList<>();
        if (service == null) service = new InspectionService();
        if (tasks == null) tasks = new ArrayList<>();
        prefs = Preferences.userNodeForPackage(InspectionDialog.class);

        initPaths();
        initComponents();   // 先创建 UI，此时 logArea 已可用

        // 输出首次复制模板的日志
        if (!startupLogs.isEmpty()) {
            for (String msg : startupLogs) log(msg);
            startupLogs.clear();
        }

        loadConfig();
        loadDataSources();
        loadReports();
    }

    /**
     * 初始化路径（与 StatsQueryDialog 一致）：
     * 1. 优先使用外部目录 ./conf/inspection/（JAR 同级），用户可自由修改/保存
     * 2. 首次运行时自动从 JAR 包复制默认模板到外部目录
     */
    private void initPaths() {
        configFile = new File(EXTERNAL_CONFIG_DIR + CONFIG_FILE_NAME);
        queryDir = new File(EXTERNAL_CONFIG_DIR + QUERY_DIR_NAME);

        // 首次运行：从 classpath（JAR 内）复制默认 config.yaml 到外部 conf/inspection/
        // query/*.sql 的复制在 InspectionService.loadTasks 中按需进行（兼容 JAR 内目录枚举失效）
        initConfigFromClasspath();

        String savedPath = prefs.get(PREF_REPORTS_PATH, null);
        if (savedPath != null && !savedPath.trim().isEmpty()) {
            reportsDir = new File(savedPath);
        } else {
            reportsDir = new File(EXTERNAL_CONFIG_DIR + REPORTS_DIR_NAME);
        }
        if (!reportsDir.exists()) reportsDir.mkdirs();
    }

    /**
     * 如果外部 conf/inspection/config.yaml 不存在，从 classpath（JAR 内或 resources 目录）复制一份出来。
     * 这样打包后首次运行也能有默认配置文件，之后用户可编辑外部文件。
     */
    private void initConfigFromClasspath() {
        boolean existed = configFile.exists();
        File f = ExternalConfigUtils.ensureExternalFile("inspection", CONFIG_FILE_NAME, "/" + CONFIG_FILE_NAME);
        if (!existed && f != null) {
            addStartupLog("📦 已从 JAR 复制默认配置: " + f.getAbsolutePath());
        }
    }

    private void addStartupLog(String msg) {
        if (startupLogs == null) startupLogs = new ArrayList<>();
        startupLogs.add(msg);
    }

    private void initComponents() {
        mainContentPanel.setLayout(new BorderLayout(8, 8));
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ===== 顶部区域 =====
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        infoPanel.add(new JLabel("数据源:"), gbc);

        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setPreferredSize(new Dimension(220, ThemeUtils.INPUT_HEIGHT));
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(dataSourceCombo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        infoPanel.add(new JLabel("输出目录:"), gbc);

        outputPathField = new JTextField(reportsDir.getAbsolutePath());
        outputPathField.setEditable(false);
        outputPathField.setFont(ThemeUtils.FONT_NORMAL);
        gbc.gridx = 3;
        gbc.weightx = 1;
        infoPanel.add(outputPathField, gbc);

        browseOutputButton = ThemeUtils.outlineButton("浏览...");
        browseOutputButton.setPreferredSize(new Dimension(90, ThemeUtils.BTN_HEIGHT));
        browseOutputButton.addActionListener(e -> chooseOutputDirectory());
        gbc.gridx = 4;
        gbc.weightx = 0;
        infoPanel.add(browseOutputButton, gbc);

        topPanel.add(infoPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        Dimension normalBtn = new Dimension(110, ThemeUtils.BTN_HEIGHT);

        refreshButton = ThemeUtils.outlineButton("刷新配置");
        refreshButton.setPreferredSize(normalBtn);
        refreshButton.addActionListener(e -> { loadConfig(); loadReports(); });
        buttonPanel.add(refreshButton);

        editConfigButton = ThemeUtils.outlineButton("编辑 config.yaml");
        editConfigButton.setPreferredSize(new Dimension(145, ThemeUtils.BTN_HEIGHT));
        editConfigButton.addActionListener(e -> editConfigFile());
        buttonPanel.add(editConfigButton);

        openQueryButton = ThemeUtils.outlineButton("打开 query");
        openQueryButton.setPreferredSize(normalBtn);
        openQueryButton.addActionListener(e -> openFolder(queryDir));
        buttonPanel.add(openQueryButton);

        startButton = ThemeUtils.primaryButton("开始巡检");
        startButton.setPreferredSize(normalBtn);
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startInspection());
        buttonPanel.add(startButton);

        clearLogButton = ThemeUtils.outlineButton("清空日志");
        clearLogButton.setPreferredSize(normalBtn);
        clearLogButton.addActionListener(e -> logArea.setText(""));
        buttonPanel.add(clearLogButton);

        openReportsButton = ThemeUtils.outlineButton("打开报告");
        openReportsButton.setPreferredSize(normalBtn);
        openReportsButton.addActionListener(e -> openFolder(reportsDir));
        buttonPanel.add(openReportsButton);

        topPanel.add(buttonPanel, BorderLayout.CENTER);
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        // ===== 中间区域 =====
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.35);
        centerSplit.setDividerLocation(380);
        centerSplit.setOneTouchExpandable(true);

        tableModel = new TaskTableModel();
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(25);
        taskTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row >= 0 && row < tasks.size()) {
                sqlPreviewArea.setText(tasks.get(row).getSql() == null ? "" : tasks.get(row).getSql());
                sqlPreviewArea.setCaretPosition(0);
            }
        });

        JScrollPane tableScroll = new JScrollPane(taskTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("任务列表"));

        sqlPreviewArea = new JTextArea();
        sqlPreviewArea.setEditable(false);
        sqlPreviewArea.setFont(ThemeUtils.FONT_LOG);
        JScrollPane previewScroll = new JScrollPane(sqlPreviewArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("SQL 预览"));

        centerSplit.setLeftComponent(tableScroll);
        centerSplit.setRightComponent(previewScroll);
        mainContentPanel.add(centerSplit, BorderLayout.CENTER);

        // ===== 底部区域 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        progressBar = new JProgressBar(0, 1);
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.NORTH);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bottomSplit.setResizeWeight(0.7);
        bottomSplit.setDividerLocation(170);
        bottomSplit.setOneTouchExpandable(true);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(ThemeUtils.FONT_LOG);
        logArea.setBackground(ThemeUtils.COLOR_LOG_BG);
        logArea.setForeground(ThemeUtils.COLOR_LOG_TEXT);
        bottomSplit.setTopComponent(new JScrollPane(logArea));

        reportListModel = new DefaultListModel<>();
        reportList = new JList<>(reportListModel);
        reportList.setFont(ThemeUtils.FONT_LOG);
        JScrollPane reportScroll = new JScrollPane(reportList);
        reportScroll.setBorder(BorderFactory.createTitledBorder("已生成报告"));
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

    // ===== 加载配置 =====
    private void loadConfig() {
        if (service == null) service = new InspectionService();
        if (tasks == null) tasks = new ArrayList<>();

        if (!configFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "配置文件 " + CONFIG_FILE_NAME + " 不存在，请创建在 " + EXTERNAL_CONFIG_DIR + "。",
                    "错误", JOptionPane.ERROR_MESSAGE);
            tableModel.fireTableDataChanged();
            return;
        }
        try {
            List<InspectionTask> loaded = service.loadTasks(configFile, queryDir);
            tasks = (loaded != null) ? loaded : new ArrayList<>();
            tableModel.fireTableDataChanged();
            startButton.setEnabled(!tasks.isEmpty() && dataSourceCombo.getSelectedItem() != null);
            log("✅ 加载配置成功，共 " + tasks.size() + " 个任务。");
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

    private void editConfigFile() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "创建 config.yaml 失败", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        try {
            Desktop.getDesktop().edit(configFile);
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

        // 强制刷新组件，避免文字消失
        SwingUtilities.invokeLater(() -> {
            for (Component comp : new Component[]{refreshButton, editConfigButton, openQueryButton,
                    startButton, clearLogButton, openReportsButton, browseOutputButton}) {
                comp.repaint();
                comp.revalidate();
            }
        });
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
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