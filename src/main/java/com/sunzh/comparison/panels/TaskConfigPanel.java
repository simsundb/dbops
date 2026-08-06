package com.sunzh.comparison.panels;

import com.sunzh.comparison.ComparisonDialog;
import com.sunzh.comparison.model.ComparisonTask;
import com.sunzh.comparison.model.ComparisonTaskConfig;
import com.sunzh.comparison.ComparisonService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskConfigPanel extends JPanel {
    private final ComparisonDialog parent;
    private DefaultTableModel configModel;
    private DefaultTableModel taskModel;
    private JTable configTable;
    private JTable taskTable;

    private static final String[] TASK_COLUMNS = {
            "JOB_ID", "任务名称", "任务描述", "源模式", "目标模式", "对比类型",
            "启用标志", "执行状态", "开始时间", "结束时间", "耗时(秒)", "错误信息",
            "表状态", "列状态", "索引状态", "序列状态", "同义词状态",
            "表错误", "列错误", "索引错误", "序列错误", "同义词错误"
    };

    public TaskConfigPanel(ComparisonDialog parent) {
        this.parent = parent;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initUI();
        SwingUtilities.invokeLater(this::refreshData);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 10));

        // ============ 配置表区域 ============
        JPanel configPanel = new JPanel(new BorderLayout(5, 5));
        configModel = new DefaultTableModel(new String[]{"源模式", "目标模式", "启用标志"}, 0);
        configTable = new JTable(configModel);
        beautifyConfigTable(configTable);
        JScrollPane configScroll = new JScrollPane(configTable);
        configScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(76, 110, 138), 1),
                "📋 gk_sjdb_task_config（配置模板表）",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), new Color(76, 110, 138)));
        configScroll.setPreferredSize(new Dimension(600, 150));
        configPanel.add(configScroll, BorderLayout.CENTER);

        JPanel configBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        configBtnPanel.setBackground(new Color(245, 248, 252));
        configBtnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(185, 195, 210), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        JButton btnRefreshConfig = createStyledButton("刷新", new Color(76, 110, 138));
        JButton btnSaveConfig = createStyledButton("保存修改", new Color(76, 110, 138));
        JButton btnAddConfig = createStyledButton("新增配置", new Color(76, 110, 138));
        JButton btnDelConfig = createStyledButton("删除配置", new Color(190, 100, 90));
        configBtnPanel.add(btnRefreshConfig);
        configBtnPanel.add(btnSaveConfig);
        configBtnPanel.add(btnAddConfig);
        configBtnPanel.add(btnDelConfig);
        configPanel.add(configBtnPanel, BorderLayout.SOUTH);
        mainPanel.add(configPanel, BorderLayout.NORTH);

        // ============ 任务表区域 ============
        JPanel taskPanel = new JPanel(new BorderLayout(5, 5));
        taskModel = new DefaultTableModel(TASK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // JOB_ID 不可编辑
            }
        };
        taskTable = new JTable(taskModel);
        beautifyTaskTable(taskTable);
        JScrollPane taskScroll = new JScrollPane(taskTable);
        taskScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(76, 110, 138), 1),
                "📋 gk_sjdb_task（任务执行表）",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), new Color(76, 110, 138)));
        taskScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        taskScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        taskScroll.setPreferredSize(new Dimension(600, 250));
        taskPanel.add(taskScroll, BorderLayout.CENTER);

        JPanel taskBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        taskBtnPanel.setBackground(new Color(245, 248, 252));
        taskBtnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(185, 195, 210), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        JButton btnRefreshTask = createStyledButton("刷新", new Color(76, 110, 138));
        JButton btnSaveTask = createStyledButton("保存任务修改", new Color(76, 110, 138));
        JButton btnAddTask = createStyledButton("新增任务", new Color(76, 110, 138));
        JButton btnDelTask = createStyledButton("删除任务", new Color(190, 100, 90));
        JButton btnInitConfig = createStyledButton("初始化配置", new Color(190, 130, 70));
        taskBtnPanel.add(btnRefreshTask);
        taskBtnPanel.add(btnSaveTask);
        taskBtnPanel.add(btnAddTask);
        taskBtnPanel.add(btnDelTask);
        taskBtnPanel.add(btnInitConfig);
        taskPanel.add(taskBtnPanel, BorderLayout.SOUTH);
        mainPanel.add(taskPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮事件
        btnRefreshConfig.addActionListener(e -> refreshData());
        btnSaveConfig.addActionListener(e -> saveConfigData());
        btnAddConfig.addActionListener(e -> configModel.addRow(new Object[]{"", "", "Y"}));
        btnDelConfig.addActionListener(e -> {
            int row = configTable.getSelectedRow();
            if (row >= 0) configModel.removeRow(row);
        });
        btnRefreshTask.addActionListener(e -> refreshData());
        btnSaveTask.addActionListener(e -> saveTaskData());
        btnAddTask.addActionListener(e -> addTask());
        btnDelTask.addActionListener(e -> {
            int row = taskTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "请先选择要删除的任务行", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String jobId = (String) taskModel.getValueAt(row, 0);
            deleteTask(jobId);
        });
        btnInitConfig.addActionListener(e -> initConfig());
    }

    // ---------- 辅助方法 ----------
    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.CENTER);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.brighter());
                btn.repaint();
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
                btn.repaint();
            }
            public void mousePressed(MouseEvent e) {
                btn.setBackground(bgColor.darker());
                btn.repaint();
            }
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(bgColor.brighter());
                btn.repaint();
            }
        });
        return btn;
    }

    private void beautifyConfigTable(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(242, 245, 249) : Color.WHITE);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setForeground(Color.WHITE);
        header.setBackground(new Color(76, 110, 138));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 82, 105), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        table.setRowHeight(24);
        table.setGridColor(new Color(205, 210, 218));
        table.setSelectionBackground(new Color(180, 200, 220));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void beautifyTaskTable(JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(taskModel);
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setGridColor(new Color(205, 210, 218));
        table.setSelectionBackground(new Color(180, 200, 220));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(242, 245, 249) : Color.WHITE);
                }
                if (column == 7 && value != null) {
                    String status = value.toString();
                    setHorizontalAlignment(SwingConstants.CENTER);
                    if ("IDLE".equals(status) || "待执行".equals(status)) {
                        setForeground(new Color(0, 100, 200));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("RUNNING".equals(status) || "执行中".equals(status)) {
                        setForeground(new Color(255, 140, 0));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("SUCCESS".equals(status) || "已完成".equals(status)) {
                        setForeground(new Color(0, 150, 0));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if ("FAILED".equals(status) || "失败".equals(status)) {
                        setForeground(Color.RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(Color.BLACK);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                } else {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setForeground(Color.WHITE);
        header.setBackground(new Color(76, 110, 138));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 82, 105), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
    }

    private void autoResizeColumns(JTable table) {
        TableColumnModel colModel = table.getColumnModel();
        int rowCount = table.getRowCount();
        int colCount = colModel.getColumnCount();
        for (int col = 0; col < colCount; col++) {
            TableColumn column = colModel.getColumn(col);
            String headerValue = column.getHeaderValue().toString();
            FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
            int maxWidth = headerMetrics.stringWidth(headerValue) + 20;
            for (int row = 0; row < rowCount; row++) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    String str = value.toString();
                    FontMetrics metrics = table.getFontMetrics(table.getFont());
                    int cellWidth = metrics.stringWidth(str) + 20;
                    if (cellWidth > maxWidth) maxWidth = cellWidth;
                }
            }
            maxWidth = Math.min(maxWidth, 500);
            column.setPreferredWidth(maxWidth);
            column.setMinWidth(maxWidth);
        }
    }

    // ===== 刷新数据 =====
    public void refreshData() {
        // 刷新配置表
        configModel.setRowCount(0);
        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            List<ComparisonTaskConfig> configs = service.loadTaskConfigs();
            for (ComparisonTaskConfig tc : configs) {
                configModel.addRow(new Object[]{tc.getSourceSchema(), tc.getTargetSchema(), tc.getEnableFlag()});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 刷新任务表
        taskModel.setRowCount(0);
        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            List<ComparisonTask> tasks = service.loadTasks();
            for (ComparisonTask t : tasks) {
                taskModel.addRow(new Object[]{
                        t.getJobId(), t.getJobName(), t.getJobDesc(),
                        t.getSourceSchema(), t.getTargetSchema(), t.getCompareTypes(),
                        t.getEnableFlag(), t.getExecStatus(),
                        t.getStartTime(), t.getEndTime(), t.getDurationSeconds(),
                        t.getErrorMsg(),
                        t.getTableStatus(), t.getColumnStatus(), t.getIndexStatus(),
                        t.getSequenceStatus(), t.getSynonymStatus(),
                        t.getTableErrorMsg(), t.getColumnErrorMsg(), t.getIndexErrorMsg(),
                        t.getSequenceErrorMsg(), t.getSynonymErrorMsg()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        autoResizeColumns(taskTable);
    }

    // ===== 保存配置表 =====
    private void saveConfigData() {
        List<ComparisonTaskConfig> configs = new ArrayList<>();
        for (int i = 0; i < configModel.getRowCount(); i++) {
            ComparisonTaskConfig tc = new ComparisonTaskConfig();
            tc.setSourceSchema((String) configModel.getValueAt(i, 0));
            tc.setTargetSchema((String) configModel.getValueAt(i, 1));
            tc.setEnableFlag((String) configModel.getValueAt(i, 2));
            configs.add(tc);
        }
        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            service.saveTaskConfigs(configs);
            JOptionPane.showMessageDialog(this, "配置表保存成功");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 保存任务表 =====
    private void saveTaskData() {
        int rowCount = taskModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "任务表为空，无需保存", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要保存任务表的所有修改到数据库吗？", "确认保存", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            for (int row = 0; row < rowCount; row++) {
                String jobId = (String) taskModel.getValueAt(row, 0);
                if (jobId == null || jobId.trim().isEmpty()) continue;
                ComparisonTask t = new ComparisonTask();
                t.setJobId(jobId);
                t.setJobName(getStringValue(row, 1));
                t.setJobDesc(getStringValue(row, 2));
                t.setSourceSchema(getStringValue(row, 3));
                t.setTargetSchema(getStringValue(row, 4));
                t.setCompareTypes(getStringValue(row, 5));
                t.setEnableFlag(getStringValue(row, 6));
                t.setExecStatus(getStringValue(row, 7));
                t.setStartTime(getStringValue(row, 8));
                t.setEndTime(getStringValue(row, 9));
                t.setDurationSeconds(parseInteger(getStringValue(row, 10)));
                t.setErrorMsg(getStringValue(row, 11));
                t.setTableStatus(getStringValue(row, 12));
                t.setColumnStatus(getStringValue(row, 13));
                t.setIndexStatus(getStringValue(row, 14));
                t.setSequenceStatus(getStringValue(row, 15));
                t.setSynonymStatus(getStringValue(row, 16));
                t.setTableErrorMsg(getStringValue(row, 17));
                t.setColumnErrorMsg(getStringValue(row, 18));
                t.setIndexErrorMsg(getStringValue(row, 19));
                t.setSequenceErrorMsg(getStringValue(row, 20));
                t.setSynonymErrorMsg(getStringValue(row, 21));
                service.updateTask(t);
            }
            JOptionPane.showMessageDialog(this, "任务表保存成功，共更新 " + rowCount + " 条记录", "完成", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存任务失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getStringValue(int row, int col) {
        Object val = taskModel.getValueAt(row, col);
        return val == null ? "" : val.toString();
    }

    private Integer parseInteger(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    // ===== 新增任务 =====
    private void addTask() {
        JTextField tfId = new JTextField("job_xxx_001");
        JTextField tfName = new JTextField();
        JTextField tfDesc = new JTextField();
        JTextField tfSource = new JTextField();
        JTextField tfTarget = new JTextField();
        JTextField tfTypes = new JTextField("ALL");
        JTextField tfFlag = new JTextField("Y");

        JPanel dialog = new JPanel(new GridLayout(0, 2, 5, 5));
        dialog.add(new JLabel("任务ID")); dialog.add(tfId);
        dialog.add(new JLabel("任务名称")); dialog.add(tfName);
        dialog.add(new JLabel("任务描述")); dialog.add(tfDesc);
        dialog.add(new JLabel("源模式")); dialog.add(tfSource);
        dialog.add(new JLabel("目标模式")); dialog.add(tfTarget);
        dialog.add(new JLabel("对比类型")); dialog.add(tfTypes);
        dialog.add(new JLabel("启用标志")); dialog.add(tfFlag);

        if (JOptionPane.showConfirmDialog(this, dialog, "新增任务", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            ComparisonTask t = new ComparisonTask();
            t.setJobId(tfId.getText().trim());
            t.setJobName(tfName.getText().trim());
            t.setJobDesc(tfDesc.getText().trim());
            t.setSourceSchema(tfSource.getText().trim());
            t.setTargetSchema(tfTarget.getText().trim());
            t.setCompareTypes(tfTypes.getText().trim());
            t.setEnableFlag(tfFlag.getText().trim());
            try (Connection conn = parent.getConnection()) {
                ComparisonService service = new ComparisonService(conn);
                service.insertTask(t);
                JOptionPane.showMessageDialog(this, "任务添加成功");
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "新增失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ===== 删除任务 =====
    private void deleteTask(String jobId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除任务 " + jobId + " 吗？", "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            service.deleteTask(jobId);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "删除失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 初始化配置（生成任务） =====
    private void initConfig() {
        int configCount = configModel.getRowCount();
        if (configCount == 0) {
            JOptionPane.showMessageDialog(this, "配置表为空，请先添加配置数据", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "将根据当前配置表（" + configCount + " 条记录）生成任务，\n现有任务数据将被清空，是否继续？",
                "确认初始化", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = parent.getConnection()) {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            st.executeUpdate("TRUNCATE TABLE gk_sjdb_task");
            String sql =
                    "INSERT INTO gk_sjdb_task (" +
                    "JOB_ID, JOB_NAME, JOB_DESC, COMPARE_SCHEMAS_SOURCE, COMPARE_SCHEMAS_TARGET, " +
                    "COMPARE_TYPES, ENABLE_FLAG, EXEC_STATUS, TABLE_STATUS, COLUMN_STATUS, " +
                    "INDEX_STATUS, SEQUENCE_STATUS, SYNONYM_STATUS, START_TIME) " +
                    "SELECT " +
                    "'job_' || c.COMPARE_SCHEMAS_SOURCE || '_' || " +
                    "LPAD(ROW_NUMBER() OVER (ORDER BY c.COMPARE_SCHEMAS_SOURCE), 3, '0'), " +
                    "'全量对比_' || TO_CHAR(SYSTIMESTAMP, 'YYYYMMDD_HH24MISS'), " +
                    "'由配置表自动生成', " +
                    "c.COMPARE_SCHEMAS_SOURCE, c.COMPARE_SCHEMAS_TARGET, " +
                    "'ALL', c.ENABLE_FLAG, 'IDLE', 'IDLE', 'IDLE', 'IDLE', 'IDLE', 'IDLE', SYSTIMESTAMP " +
                    "FROM gk_sjdb_task_config c WHERE c.ENABLE_FLAG = 'Y'";
            int count = st.executeUpdate(sql);
            conn.commit();
            JOptionPane.showMessageDialog(this, "初始化完成，生成 " + count + " 个任务");
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "初始化失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}