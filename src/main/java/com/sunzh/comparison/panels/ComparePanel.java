package com.sunzh.comparison.panels;

import com.sunzh.comparison.ComparisonDialog;
import com.sunzh.comparison.model.ComparisonTask;
import com.sunzh.comparison.ComparisonService;
import com.sunzh.ui.components.WidgetFactory;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ComparePanel extends JPanel {
    // 主题色映射 - 统一到 ThemeUtils
    private static final Color ACCENT = ThemeUtils.COLOR_PRIMARY;
    private static final Color ACCENT_DARK = ThemeUtils.COLOR_PRIMARY_DARK;
    private static final Color ACCENT_LIGHT = ThemeUtils.COLOR_PRIMARY_LIGHT;
    private static final Color BTN_COMPARE = ThemeUtils.COLOR_PRIMARY;
    private static final Color BTN_RESET = ThemeUtils.COLOR_WARNING;
    private static final Color ROW_ALT = ThemeUtils.COLOR_BG_ALTERNATE;
    private static final Color BORDER_ACCENT = ThemeUtils.COLOR_PRIMARY;

    private final ComparisonDialog parent;
    private DefaultTableModel taskModel;
    private DefaultTableModel summaryModel;
    private JTable taskTable;
    private JTable summaryTable;
    private JCheckBox chkSelectAll;
    private JButton btnCompare;
    private JButton btnReset;

    private static final String[] TASK_COLUMNS = {
            "选择", "JOB_ID", "任务名称", "任务描述", "源模式", "目标模式", "对比类型",
            "启用标志", "执行状态", "开始时间", "结束时间", "耗时(秒)", "错误信息",
            "表状态", "列状态", "索引状态", "序列状态", "同义词状态",
            "表错误", "列错误", "索引错误", "序列错误", "同义词错误"
    };

    public ComparePanel(ComparisonDialog parent) {
        this.parent = parent;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initUI();
        SwingUtilities.invokeLater(this::refreshData);
    }

    private void initUI() {
        // ---- 任务列表 ----
        taskModel = new DefaultTableModel(TASK_COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
        };
        taskTable = new JTable(taskModel);
        beautifyTaskTable(taskTable);

        JPanel taskPanel = new JPanel(new BorderLayout(5, 5));
        taskPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                "任务列表（可多选）",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SMALL_BOLD, ACCENT));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setOpaque(false);
        chkSelectAll = new JCheckBox("全选/取消全选");
        chkSelectAll.setFont(ThemeUtils.FONT_SMALL);
        chkSelectAll.addActionListener(e -> {
            boolean sel = chkSelectAll.isSelected();
            for (int i = 0; i < taskModel.getRowCount(); i++) {
                taskModel.setValueAt(sel, i, 0);
            }
        });
        toolBar.add(chkSelectAll);
        taskPanel.add(toolBar, BorderLayout.NORTH);

        JScrollPane taskScroll = new JScrollPane(taskTable);
        taskScroll.setPreferredSize(new Dimension(800, 200));
        taskScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        taskScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        taskPanel.add(taskScroll, BorderLayout.CENTER);
        add(taskPanel, BorderLayout.NORTH);

        // ---- 下半部分 ----
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(ThemeUtils.COLOR_BG);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));

        btnCompare = createStyledButton("执行选中对比", ACCENT);
        btnCompare.addActionListener(e -> doCompare());

        btnReset = createStyledButton("重置状态", BTN_RESET);
        btnReset.addActionListener(e -> resetStatus());

        btnPanel.add(btnCompare);
        btnPanel.add(btnReset);
        bottomPanel.add(btnPanel, BorderLayout.NORTH);

        summaryModel = new DefaultTableModel(
                new String[]{"对象类型", "总数", "仅源端", "仅目标端", "不匹配"}, 0);
        summaryTable = new JTable(summaryModel);
        beautifySummaryTable(summaryTable);

        JScrollPane summaryScroll = new JScrollPane(summaryTable);
        summaryScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                "整体情况",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SMALL_BOLD, ACCENT));
        summaryScroll.setPreferredSize(new Dimension(800, 120));
        bottomPanel.add(summaryScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.CENTER);
    }

    // ----- 样式辅助 -----
    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 25, 10, 25)));
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

    private void beautifyTaskTable(JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(taskModel);
        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setFont(ThemeUtils.FONT_SMALL);
        table.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        table.setSelectionBackground(ACCENT_LIGHT);
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader header = table.getTableHeader();
        header.setFont(ThemeUtils.FONT_SMALL_BOLD);
        header.setForeground(Color.WHITE);
        header.setBackground(ACCENT);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ROW_ALT : Color.WHITE);
                }
                if (column == 8 && value != null) {
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
                } else if (column == 0) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(value != null && (Boolean) value);
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                checkBox.setBackground(row % 2 == 0 ? ROW_ALT : Color.WHITE);
                return checkBox;
            }
        });
    }

    private void beautifySummaryTable(JTable table) {
        table.setRowHeight(24);
        table.setFont(ThemeUtils.FONT_SMALL);
        table.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        table.setSelectionBackground(ACCENT_LIGHT);
        table.setSelectionForeground(Color.WHITE);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(100);
        colModel.getColumn(1).setPreferredWidth(80);
        colModel.getColumn(2).setPreferredWidth(80);
        colModel.getColumn(3).setPreferredWidth(80);
        colModel.getColumn(4).setPreferredWidth(80);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ROW_ALT : Color.WHITE);
                }
                if (column >= 1 && column <= 4) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.CENTER);
                }
                setForeground(Color.BLACK);
                setFont(getFont().deriveFont(Font.PLAIN));
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(ThemeUtils.FONT_SMALL_BOLD);
        header.setForeground(Color.WHITE);
        header.setBackground(ACCENT);
    }

    // ----- 刷新数据 -----
    public void refreshData() {
        taskModel.setRowCount(0);
        try (Connection conn = parent.getConnection()) {
            ComparisonService service = new ComparisonService(conn);
            List<ComparisonTask> tasks = service.loadTasks();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (ComparisonTask task : tasks) {
                taskModel.addRow(new Object[]{
                        Boolean.FALSE,
                        task.getJobId(),
                        task.getJobName(),
                        task.getJobDesc(),
                        task.getSourceSchema(),
                        task.getTargetSchema(),
                        task.getCompareTypes(),
                        task.getEnableFlag(),
                        task.getExecStatus(),
                        task.getStartTime(),
                        task.getEndTime(),
                        task.getDurationSeconds(),
                        task.getErrorMsg(),
                        task.getTableStatus(),
                        task.getColumnStatus(),
                        task.getIndexStatus(),
                        task.getSequenceStatus(),
                        task.getSynonymStatus(),
                        task.getTableErrorMsg(),
                        task.getColumnErrorMsg(),
                        task.getIndexErrorMsg(),
                        task.getSequenceErrorMsg(),
                        task.getSynonymErrorMsg()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "刷新任务列表失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
        chkSelectAll.setSelected(false);
        autoResizeColumns(taskTable);
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
            maxWidth = Math.min(maxWidth, 400);
            column.setPreferredWidth(maxWidth);
            column.setMinWidth(maxWidth);
        }
    }

    private List<String> getSelectedJobIds() {
        List<String> jobIds = new ArrayList<>();
        for (int i = 0; i < taskModel.getRowCount(); i++) {
            if ((Boolean) taskModel.getValueAt(i, 0)) {
                jobIds.add((String) taskModel.getValueAt(i, 1));
            }
        }
        return jobIds;
    }

    // ----- 执行对比 -----
    private void doCompare() {
        List<String> jobIds = getSelectedJobIds();
        if (jobIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一个任务", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnCompare.setEnabled(false);
        btnReset.setEnabled(false);
        btnCompare.setText("执行中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = parent.getConnection()) {
                    ComparisonService service = new ComparisonService(conn);
                    // 依次执行对比
                    for (String jobId : jobIds) {
                        service.generateCompare(jobId);
                    }

                    // 获取第一个任务的汇总结果（若有多个，只展示第一个）
                    String firstJob = jobIds.get(0);
                    List<Object[]> summaryData = service.getSummaryForJob(firstJob);
                    summaryModel.setRowCount(0);
                    for (Object[] row : summaryData) {
                        summaryModel.addRow(row);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(ComparePanel.this,
                                    "对比失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                btnCompare.setEnabled(true);
                btnReset.setEnabled(true);
                btnCompare.setText("执行选中对比");
                refreshData();
            }
        }.execute();
    }

    // ----- 重置状态 -----
    private void resetStatus() {
        List<String> jobIds = getSelectedJobIds();
        if (jobIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一个任务", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要重置选中任务的执行状态吗？\n这将清空所有状态字段、结束时间和耗时，便于重新对比。",
                "确认重置", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        btnCompare.setEnabled(false);
        btnReset.setEnabled(false);
        btnReset.setText("重置中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = parent.getConnection()) {
                    ComparisonService service = new ComparisonService(conn);
                    service.resetTaskStatuses(jobIds);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(ComparePanel.this,
                                    "重置状态失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                btnCompare.setEnabled(true);
                btnReset.setEnabled(true);
                btnReset.setText("重置状态");
                refreshData();
                JOptionPane.showMessageDialog(ComparePanel.this,
                        "状态重置成功！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }
}