package com.sunzh.scriptrunner;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.ui.components.WidgetFactory;
import com.sunzh.utils.ThemeUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ScriptRunnerDialog extends BaseDialog {

    // ---- UI组件 ----
    private JTabbedPane tabbedPane;
    private JComboBox<String> sourceCombo;
    private JComboBox<String> failureSourceCombo;
    private JButton btnSelectFiles, btnClearFiles;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private JButton btnExecute;
    private JButton btnInitMigrationTables;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel fileCountLabel;

    private DefaultTableModel failureModel;
    private JTable failureTable;
    private JLabel failureStatsLabel;
    private JButton btnSummary, btnDetail, btnExportExcel;
    private String currentMode = "汇总";

    private List<DataSource> dataSources = new ArrayList<>();
    private PrintWriter logWriter;

    public ScriptRunnerDialog(JFrame owner) {
        super(owner, "SQL脚本执行", "file-code");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        setBackground(ThemeUtils.COLOR_BG);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initLog();
        loadDataSources();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeUtils.FONT_NORMAL);

        tabbedPane.addTab("  脚本执行中心", createExecutionPanel());
        tabbedPane.addTab("  失败记录", createFailurePanel());

        mainContentPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        statusPanel.setBackground(ThemeUtils.COLOR_BG);
        JLabel statusLabel = new JLabel("就绪 | 请选择数据源和SQL文件");
        statusLabel.setFont(ThemeUtils.FONT_SMALL);
        statusLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainContentPanel.add(statusPanel, BorderLayout.SOUTH);
    }

    // ---- 加载数据源 ----
    private void loadDataSources() {
        dataSources = DataSourceStore.load();
        refreshCombo(sourceCombo);
        refreshCombo(failureSourceCombo);
    }

    private void refreshCombo(JComboBox<String> combo) {
        if (combo == null) return;
        combo.removeAllItems();
        for (DataSource ds : dataSources) {
            combo.addItem(ds.getName());
        }
        if (dataSources.isEmpty()) {
            combo.addItem("请先在数据源配置中添加数据源");
            combo.setEnabled(false);
        } else {
            combo.setEnabled(true);
            combo.setSelectedIndex(0);
        }
    }

    // ---- 执行配置面板 ----
    private JPanel createExecutionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(ThemeUtils.COLOR_BG);

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBackground(ThemeUtils.COLOR_BG_CARD);
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "配置",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        sourcePanel.setOpaque(false);
        sourcePanel.add(new JLabel("选择数据源:"));
        sourcePanel.setFont(ThemeUtils.FONT_NORMAL);

        sourceCombo = new JComboBox<>();
        sourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        sourceCombo.setPreferredSize(new Dimension(250, 30));
        sourcePanel.add(sourceCombo);

        JButton btnRefreshDs = WidgetFactory.infoButton("刷新", "refresh");
        btnRefreshDs.setPreferredSize(new Dimension(80, 28));
        btnRefreshDs.addActionListener(e -> loadDataSources());
        sourcePanel.add(btnRefreshDs);

        topPanel.add(sourcePanel, BorderLayout.WEST);

        JPanel fileBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        fileBtnPanel.setOpaque(false);
        btnSelectFiles = WidgetFactory.primaryButton("选择SQL文件", "folder-open");
        btnSelectFiles.setPreferredSize(new Dimension(120, 30));
        btnSelectFiles.addActionListener(e -> selectFiles());
        fileBtnPanel.add(btnSelectFiles);

        btnClearFiles = WidgetFactory.secondaryButton("清空列表", "clear");
        btnClearFiles.setPreferredSize(new Dimension(100, 30));
        btnClearFiles.addActionListener(e -> {
            fileListModel.clear();
            updateFileStats();
        });
        fileBtnPanel.add(btnClearFiles);

        topPanel.add(fileBtnPanel, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel fileListPanel = new JPanel(new BorderLayout());
        fileListPanel.setBackground(ThemeUtils.COLOR_BG_CARD);
        fileListPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "待执行文件列表",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(ThemeUtils.FONT_NORMAL);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!isSelected) {
                    c.setBackground(index % 2 == 0 ? ThemeUtils.COLOR_BG_ALTERNATE : java.awt.Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane fileScroll = new JScrollPane(fileList);
        fileScroll.setBorder(BorderFactory.createEmptyBorder());
        fileScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        fileListPanel.add(fileScroll, BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statsPanel.setOpaque(false);
        fileCountLabel = new JLabel("共 0 个文件");
        fileCountLabel.setFont(ThemeUtils.FONT_SMALL);
        fileCountLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        statsPanel.add(fileCountLabel);
        fileListPanel.add(statsPanel, BorderLayout.SOUTH);

        panel.add(fileListPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 8));
        bottomPanel.setOpaque(false);

        JPanel execPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        execPanel.setOpaque(false);

        btnExecute = WidgetFactory.successButton("执行SQL", "play");
        btnExecute.setPreferredSize(new Dimension(140, 36));
        btnExecute.setFont(ThemeUtils.FONT_BOLD);
        btnExecute.addActionListener(e -> executeSQLFiles());
        execPanel.add(btnExecute);

        btnInitMigrationTables = WidgetFactory.infoButton("基础表初始化", "database");
        btnInitMigrationTables.setPreferredSize(new Dimension(150, 36));
        btnInitMigrationTables.setFont(ThemeUtils.FONT_BOLD);
        btnInitMigrationTables.addActionListener(e -> initMigrationTables());
        execPanel.add(btnInitMigrationTables);

        JButton btnClearLog = WidgetFactory.secondaryButton("清空日志", "clear");
        btnClearLog.setPreferredSize(new Dimension(100, 36));
        btnClearLog.addActionListener(e -> logArea.setText(""));
        execPanel.add(btnClearLog);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(180, 26));
        progressBar.setStringPainted(true);
        progressBar.setFont(ThemeUtils.FONT_SMALL);
        execPanel.add(progressBar);

        bottomPanel.add(execPanel, BorderLayout.NORTH);

        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(ThemeUtils.FONT_LOG);
        logArea.setBackground(ThemeUtils.COLOR_LOG_BG);
        logArea.setForeground(ThemeUtils.COLOR_LOG_TEXT);
        logArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "执行日志",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));
        logScroll.setPreferredSize(new Dimension(800, 180));
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshCombo(sourceCombo);

        return panel;
    }

    // ---- 失败记录面板 ----
    private JPanel createFailurePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(ThemeUtils.COLOR_BG);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setOpaque(false);

        toolBar.add(new JLabel("数据源:"));
        failureSourceCombo = new JComboBox<>();
        failureSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        failureSourceCombo.setPreferredSize(new Dimension(200, 28));
        toolBar.add(failureSourceCombo);

        JButton btnRefresh = WidgetFactory.infoButton("刷新", "refresh");
        btnRefresh.setPreferredSize(new Dimension(80, 30));
        btnRefresh.addActionListener(e -> loadFailureData(currentMode));
        toolBar.add(btnRefresh);

        btnSummary = WidgetFactory.warningButton("汇总", "report");
        btnSummary.setPreferredSize(new Dimension(80, 30));
        btnSummary.addActionListener(e -> loadFailureData("汇总"));
        toolBar.add(btnSummary);

        btnDetail = WidgetFactory.primaryButton("明细", "list");
        btnDetail.setPreferredSize(new Dimension(80, 30));
        btnDetail.addActionListener(e -> loadFailureData("明细"));
        toolBar.add(btnDetail);

        btnExportExcel = WidgetFactory.successButton("导出Excel", "export");
        btnExportExcel.setPreferredSize(new Dimension(100, 30));
        btnExportExcel.addActionListener(e -> exportFailureExcel());
        toolBar.add(btnExportExcel);

        failureStatsLabel = new JLabel("共 0 条记录");
        failureStatsLabel.setFont(ThemeUtils.FONT_SMALL);
        failureStatsLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        toolBar.add(failureStatsLabel);

        panel.add(toolBar, BorderLayout.NORTH);

        refreshCombo(failureSourceCombo);

        failureModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        failureTable = new JTable(failureModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(failureModel);
        failureTable.setRowSorter(sorter);
        beautifyFailureTable(failureTable);

        JScrollPane scrollPane = new JScrollPane(failureTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "失败记录",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));
        scrollPane.setPreferredSize(new Dimension(800, 350));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void beautifyFailureTable(JTable table) {
        table.setRowHeight(24);
        table.setFont(ThemeUtils.FONT_NORMAL);
        table.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        table.setSelectionBackground(ThemeUtils.COLOR_PRIMARY);
        table.setSelectionForeground(java.awt.Color.WHITE);

        table.getTableHeader().setFont(ThemeUtils.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(ThemeUtils.COLOR_PRIMARY);
        table.getTableHeader().setForeground(java.awt.Color.WHITE);

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String status = null;
                    if (table.getColumnCount() > 3 && "明细".equals(currentMode)) {
                        int modelRow = table.convertRowIndexToModel(row);
                        status = (String) table.getModel().getValueAt(modelRow, 3);
                    }
                    if ("FAILED".equals(status)) {
                        c.setForeground(ThemeUtils.COLOR_DANGER);
                        c.setFont(c.getFont().deriveFont(java.awt.Font.BOLD));
                    } else {
                        c.setForeground(ThemeUtils.COLOR_TEXT);
                    }
                    c.setBackground(row % 2 == 0 ? ThemeUtils.COLOR_BG_ALTERNATE : java.awt.Color.WHITE);
                } else {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }
                return c;
            }
        });
    }

    // ---- 失败记录加载 ----
    private void loadFailureData(String mode) {
        currentMode = mode;
        String selectedName = (String) failureSourceCombo.getSelectedItem();
        if (selectedName == null || selectedName.isEmpty() || dataSources.isEmpty()) {
            failureStatsLabel.setText("请先配置数据源");
            failureModel.setRowCount(0);
            return;
        }

        DataSource ds = null;
        for (DataSource d : dataSources) {
            if (d.getName().equals(selectedName)) {
                ds = d;
                break;
            }
        }
        if (ds == null) {
            failureStatsLabel.setText("未找到数据源");
            failureModel.setRowCount(0);
            return;
        }

        final DataSource finalDs = ds;

        btnSummary.setEnabled(false);
        btnDetail.setEnabled(false);
        btnExportExcel.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = DriverManager.getConnection(
                        finalDs.buildUrl(), finalDs.getUser(), finalDs.getPassword())) {
                    if ("汇总".equals(mode)) loadSummaryData(conn);
                    else loadDetailData(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void done() {
                btnSummary.setEnabled(true);
                btnDetail.setEnabled(true);
                btnExportExcel.setEnabled(true);
                failureStatsLabel.setText("共 " + failureModel.getRowCount() + " 条记录");
            }
        }.execute();
    }

    private void loadSummaryData(Connection conn) throws SQLException {
        String sql = "SELECT file_name, COUNT(*) AS total_sql, " +
                "SUM(CASE WHEN exec_flag = 'FAILED' THEN 1 ELSE 0 END) AS failed_count, " +
                "SUM(CASE WHEN exec_flag = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
                "SUM(CASE WHEN exec_flag IS NULL THEN 1 ELSE 0 END) AS pending_count " +
                "FROM general_app_form_parsed GROUP BY file_name " +
                "HAVING SUM(CASE WHEN exec_flag = 'FAILED' THEN 1 ELSE 0 END) > 0 ORDER BY file_name";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            failureModel.setDataVector(new Object[][]{},
                    new String[]{"文件名", "总SQL数", "失败数", "成功数", "待执行数"});
            while (rs.next()) {
                failureModel.addRow(new Object[]{
                        rs.getString("file_name"),
                        rs.getInt("total_sql"),
                        rs.getInt("failed_count"),
                        rs.getInt("success_count"),
                        rs.getInt("pending_count")
                });
            }
        }
        for (int i = 0; i < 5; i++) {
            failureTable.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 150 : 80);
        }
    }

    private void loadDetailData(Connection conn) throws SQLException {
        String sql = "SELECT t.file_name, t.seq_id, t.ddl_sql, t.exec_flag, t.exec_time, t.exec_msg " +
                "FROM general_app_form_parsed t " +
                "WHERE t.file_name IN (SELECT DISTINCT file_name FROM general_app_form_parsed WHERE exec_flag = 'FAILED') " +
                "ORDER BY t.file_name, t.seq_id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            failureModel.setDataVector(new Object[][]{},
                    new String[]{"文件名", "序号", "DDL_SQL", "执行状态", "执行时间", "错误信息"});
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("exec_time");
                String timeStr = ts != null ? sdf.format(ts) : "";
                failureModel.addRow(new Object[]{
                        rs.getString("file_name"),
                        rs.getInt("seq_id"),
                        rs.getString("ddl_sql"),
                        rs.getString("exec_flag"),
                        timeStr,
                        rs.getString("exec_msg")
                });
            }
        }
        int[] widths = {120, 60, 400, 80, 130, 200};
        for (int i = 0; i < 6; i++) {
            failureTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ---- 导出Excel ----
    private void exportFailureExcel() {
        if (failureModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "没有数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle("保存 Excel 文件");
        fileChooser.setSelectedFile(new File("失败记录_" + System.currentTimeMillis() + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 文件 (*.xlsx)", "xlsx"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xlsx")) filePath += ".xlsx";
        final String finalFilePath = filePath;

        btnExportExcel.setEnabled(false);
        btnExportExcel.setText("导出中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("失败记录");
                    Row headerRow = sheet.createRow(0);
                    CellStyle headerStyle = workbook.createCellStyle();
                    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setColor(IndexedColors.WHITE.getIndex());
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setAlignment(HorizontalAlignment.CENTER);
                    int colCount = failureModel.getColumnCount();
                    for (int i = 0; i < colCount; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(failureModel.getColumnName(i));
                        cell.setCellStyle(headerStyle);
                    }
                    for (int row = 0; row < failureModel.getRowCount(); row++) {
                        Row excelRow = sheet.createRow(row + 1);
                        for (int col = 0; col < colCount; col++) {
                            Object value = failureModel.getValueAt(row, col);
                            Cell cell = excelRow.createCell(col);
                            if (value == null) cell.setCellValue("");
                            else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
                            else if (value instanceof java.util.Date) cell.setCellValue((java.util.Date) value);
                            else cell.setCellValue(value.toString());
                        }
                    }
                    for (int i = 0; i < colCount; i++) sheet.autoSizeColumn(i);
                    try (FileOutputStream fos = new FileOutputStream(finalFilePath)) {
                        workbook.write(fos);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(ScriptRunnerDialog.this,
                                    "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }
            @Override
            protected void done() {
                btnExportExcel.setEnabled(true);
                btnExportExcel.setText("导出Excel");
                JOptionPane.showMessageDialog(ScriptRunnerDialog.this,
                        "导出成功！\n文件保存至: " + finalFilePath,
                        "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // ---- 文件选择 ----
    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件 (*.sql, *.txt)", "sql", "txt"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (!fileListModel.contains(file.getAbsolutePath())) {
                    fileListModel.addElement(file.getAbsolutePath());
                }
            }
            updateFileStats();
        }
    }

    private void updateFileStats() {
        if (fileCountLabel != null) {
            fileCountLabel.setText("共 " + fileListModel.size() + " 个文件");
        }
    }

    // ---- 执行SQL ----
    private void executeSQLFiles() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择SQL文件！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedName = (String) sourceCombo.getSelectedItem();
        if (selectedName == null || selectedName.isEmpty() || dataSources.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先配置并选择数据源！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DataSource ds = null;
        for (DataSource d : dataSources) {
            if (d.getName().equals(selectedName)) {
                ds = d;
                break;
            }
        }
        if (ds == null) {
            JOptionPane.showMessageDialog(this, "未找到数据源配置！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要在 [" + ds.getName() + "] (" + ds.getType() + ") 上执行 " +
                        fileListModel.size() + " 个SQL文件吗？\n请确保数据已备份！",
                "确认执行", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        btnExecute.setEnabled(false);
        btnExecute.setText("执行中...");
        progressBar.setValue(0);
        logArea.setText("");

        final DataSource finalDs = ds;

        new SwingWorker<Void, String>() {
            private int totalFiles = fileListModel.size();
            private int completed = 0;

            @Override
            protected Void doInBackground() {
                appendLog("========================================");
                appendLog("开始执行SQL文件迁移");
                appendLog("目标数据源: " + finalDs.getName() + " (" + finalDs.getType() + ")");
                appendLog("文件数量: " + totalFiles);
                appendLog("========================================");

                ScriptRunner runner = new ScriptRunner(msg -> { appendLog(msg); log(msg); });

                try {
                    if ("ORACLE".equalsIgnoreCase(finalDs.getType())) {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } else {
                        try {
                            Class.forName("com.huawei.gaussdb.jdbc.Driver");
                        } catch (ClassNotFoundException e1) {
                            Class.forName("org.postgresql.Driver");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    appendLog("❌ 驱动加载失败: " + e.getMessage());
                    return null;
                }

                try (Connection conn = DriverManager.getConnection(
                        finalDs.buildUrl(), finalDs.getUser(), finalDs.getPassword())) {
                    conn.setAutoCommit(false);
                    for (int i = 0; i < fileListModel.size(); i++) {
                        String filePath = fileListModel.get(i);
                        File file = new File(filePath);
                        appendLog("\n[" + (i + 1) + "/" + totalFiles + "] 处理文件: " + file.getName());
                        try {
                            runner.processFile(conn, file, file.getName());
                            conn.commit();
                            appendLog("✅ 文件处理完成: " + file.getName());
                        } catch (Exception e) {
                            conn.rollback();
                            appendLog("❌ 文件处理失败: " + file.getName() + " - " + e.getMessage());
                            logException(e);
                        }
                        completed = i + 1;
                        int progress = (int) ((double) completed / totalFiles * 100);
                        setProgress(progress);
                        progressBar.setValue(progress);
                    }
                    appendLog("========================================");
                    appendLog("执行完成！共处理 " + totalFiles + " 个文件");
                    appendLog("========================================");
                } catch (SQLException e) {
                    appendLog("❌ 数据库连接失败: " + e.getMessage());
                    logException(e);
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendLog(msg);
            }

            @Override
            protected void done() {
                btnExecute.setEnabled(true);
                btnExecute.setText("执行SQL");
                progressBar.setValue(100);
            }
        }.execute();
    }

    // ============================================================
    // ★★★ 初始化迁移表（独立于 ScriptRunner） ★★★
    // ============================================================

    private void initMigrationTables() {
        String selectedName = (String) sourceCombo.getSelectedItem();
        if (selectedName == null || dataSources.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先选择数据源！",
                    "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        DataSource ds = null;
        for (DataSource d : dataSources) {
            if (d.getName().equals(selectedName)) {
                ds = d;
                break;
            }
        }

        if (ds == null) {
            JOptionPane.showMessageDialog(this,
                    "未找到数据源配置！",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final DataSource finalDs = ds;

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                String sqlFile;
                if ("ORACLE".equalsIgnoreCase(finalDs.getType())) {
                    sqlFile = "sql/oracle_general_app.sql";
                } else {
                    sqlFile = "sql/gaussdb_general_app.sql";
                }

                try {
                    appendLog("================================");
                    appendLog("初始化SQL迁移管理表");
                    appendLog("数据源: " + finalDs.getName());
                    appendLog("数据库类型: " + finalDs.getType());
                    appendLog("脚本: " + sqlFile);

                    // 从 classpath 加载 SQL 内容
                    String sqlScript = loadSqlScriptFromResource(sqlFile);
                    if (sqlScript == null || sqlScript.trim().isEmpty()) {
                        appendLog("❌ 无法加载资源文件: " + sqlFile);
                        return null;
                    }
                    appendLog("加载脚本成功，长度: " + sqlScript.length() + " 字符");

                    try (Connection conn = DriverManager.getConnection(
                            finalDs.buildUrl(), finalDs.getUser(), finalDs.getPassword())) {
                        conn.setAutoCommit(false);

                        // 检查表是否已存在
                        if (migrationTablesExists(conn, finalDs.getType())) {
                            appendLog("表已存在，跳过执行:");
                            appendLog("  GENERAL_APP_FORM");
                            appendLog("  GENERAL_APP_FORM_PARSED");
                            conn.commit();
                            return null;
                        }

                        // ★★★ 执行建表脚本（先执行 CREATE TABLE） ★★★
                        appendLog("开始执行建表脚本...");
                        int executed = executeSqlScript(conn, sqlScript);
                        conn.commit();
                        appendLog("✅ 建表脚本执行成功，共执行 " + executed + " 条语句。");
                    }
                } catch (Exception e) {
                    appendLog("❌ 初始化失败: " + e.getMessage());
                    logException(e);
                }
                return null;
            }
        }.execute();
    }

    /**
     * 从 classpath 加载 SQL 脚本内容
     */
    private String loadSqlScriptFromResource(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                appendLog("❌ classpath 资源不存在: " + resourcePath);
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                 BufferedReader br = new BufferedReader(isr)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 执行 SQL 脚本（自动拆分，先执行 CREATE TABLE，再执行其他）
     */
    // private int executeSqlScript(Connection conn, String sqlScript) throws SQLException {
    //     conn.setAutoCommit(false);
    //     String[] statements = sqlScript.split(";");
    //     int count = 0;
    //     List<String> createTableStmts = new ArrayList<>();
    //     List<String> otherStmts = new ArrayList<>();

    //     // 分类
    //     for (String sql : statements) {
    //         String trimmed = sql.trim();
    //         if (trimmed.isEmpty()) {
    //             continue;
    //         }
    //         // 跳过纯注释行（以 -- 或 /* 开头）
    //         if (trimmed.startsWith("--") || trimmed.startsWith("/*")) {
    //             continue;
    //         }
    //         // 判断是否为 CREATE TABLE
    //         if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
    //             createTableStmts.add(trimmed);
    //         } else {
    //             otherStmts.add(trimmed);
    //         }
    //     }

    //     // 1. 执行所有 CREATE TABLE（忽略“已存在”错误）
    //     try (Statement stmt = conn.createStatement()) {
    //         for (String ddl : createTableStmts) {
    //             String preview = ddl.length() > 80 ? ddl.substring(0, 80) + "..." : ddl;
    //             appendLog("执行 CREATE TABLE: " + preview);
    //             try {
    //                 stmt.execute(ddl);
    //                 count++;
    //                 appendLog("  -> 成功");
    //             } catch (SQLException e) {
    //                 if (e.getMessage().contains("already exists") || e.getMessage().contains("exists")) {
    //                     appendLog("  -> ⚠️ 表已存在，忽略");
    //                 } else {
    //                     appendLog("  -> ❌ 失败: " + e.getMessage());
    //                     throw e;
    //                 }
    //             }
    //         }
    //         conn.commit();
    //     } catch (SQLException e) {
    //         conn.rollback();
    //         throw e;
    //     }

    //     // 2. 执行其他语句（COMMENT, CREATE INDEX 等）
    //     try (Statement stmt = conn.createStatement()) {
    //         for (String sql : otherStmts) {
    //             String preview = sql.length() > 60 ? sql.substring(0, 60) + "..." : sql;
    //             appendLog("执行其他: " + preview);
    //             stmt.execute(sql);
    //             count++;
    //         }
    //         conn.commit();
    //     } catch (SQLException e) {
    //         conn.rollback();
    //         appendLog("❌ 其他语句执行失败: " + e.getMessage());
    //         throw e;
    //     } finally {
    //         conn.setAutoCommit(true);
    //     }

    //     return count;
    // }


private int executeSqlScript(Connection conn, String sqlScript) throws SQLException {
    // 保存原始 autoCommit 状态
    boolean originalAutoCommit = conn.getAutoCommit();
    try {
        conn.setAutoCommit(false);
        String[] statements = sqlScript.split(";");
        int count = 0;
        List<String> createTableStmts = new ArrayList<>();
        List<String> otherStmts = new ArrayList<>();

        // 分类
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 跳过纯注释行
            if (trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                continue;
            }
            // 判断是否为 CREATE TABLE
            if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
                createTableStmts.add(trimmed);
            } else {
                otherStmts.add(trimmed);
            }
        }

        // 1. 执行所有 CREATE TABLE（忽略“已存在”错误）
        try (Statement stmt = conn.createStatement()) {
            for (String ddl : createTableStmts) {
                String preview = ddl.length() > 80 ? ddl.substring(0, 80) + "..." : ddl;
                appendLog("执行 CREATE TABLE: " + preview);
                try {
                    stmt.execute(ddl);
                    count++;
                    appendLog("  -> 成功");
                } catch (SQLException e) {
                    if (e.getMessage().contains("already exists") || e.getMessage().contains("exists")) {
                        appendLog("  -> ⚠️ 表已存在，忽略");
                    } else {
                        appendLog("  -> ❌ 失败: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }

        // 2. 执行其他语句（COMMENT, CREATE INDEX 等）
        try (Statement stmt = conn.createStatement()) {
            for (String sql : otherStmts) {
                String preview = sql.length() > 60 ? sql.substring(0, 60) + "..." : sql;
                appendLog("执行其他: " + preview);
                stmt.execute(sql);
                count++;
            }
        }

        // 统一提交
        conn.commit();
        return count;
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    } finally {
        // 恢复原始 autoCommit 状态
        conn.setAutoCommit(originalAutoCommit);
    }
}

    /**
     * 检查迁移表是否存在（两个表都存在）
     */
    private boolean migrationTablesExists(Connection conn, String dbType) throws SQLException {
        String sql;
        if ("ORACLE".equalsIgnoreCase(dbType)) {
            sql = "SELECT COUNT(*) FROM user_tables " +
                  "WHERE table_name IN ('GENERAL_APP_FORM', 'GENERAL_APP_FORM_PARSED')";
        } else {
            sql = "SELECT COUNT(*) FROM pg_tables " +
                  "WHERE schemaname = current_schema() " +
                  "AND tablename IN ('general_app_form', 'general_app_form_parsed')";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) >= 2;
            }
        }
        return false;
    }

    // ---- 日志工具 ----
    private void initLog() {
        try {
            logWriter = new PrintWriter(new FileWriter("script_runner.log", true), true);
        } catch (IOException e) {
            System.err.println("初始化日志失败: " + e.getMessage());
        }
    }

    private void log(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String logMsg = "[" + timestamp + "] " + msg;
        System.out.println(logMsg);
        if (logWriter != null) {
            logWriter.println(logMsg);
            logWriter.flush();
        }
    }

    private void logException(Exception e) {
        if (logWriter != null) {
            e.printStackTrace(logWriter);
            logWriter.flush();
        }
        e.printStackTrace();
    }

    private void appendLog(String msg) {
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    @Override
    public void refresh() {
        loadDataSources();
    }
}