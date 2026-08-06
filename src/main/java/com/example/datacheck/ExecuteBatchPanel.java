package com.example.datacheck;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.utils.ThemeUtils;
import com.example.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行批次面板 - 美化版（含明细查询）
 * 布局：明细表格 60% + 执行日志 40%，日志默认可见
 * 支持点击列头排序，支持横向滚动，列宽自适应
 */
public class ExecuteBatchPanel extends JPanel {

    // ==================== 组件定义 ====================
    private JComboBox<String> batchCombo;
    private JButton executeBtn, queryBtn;
    private JTextArea logArea;
    private JTable detailTable;
    private DetailTableModel detailTableModel;
    private DataCheckDetailDao detailDao = new DataCheckDetailDao();
    private DataCheckProcedureDao procDao = new DataCheckProcedureDao();

    private JComboBox<String> dataSourceCombo;
    private List<DataSource> allDataSources = new ArrayList<>();
    private JLabel statusLabel;
    private String currentBatchId;
    private JSplitPane splitPane;
    private JScrollPane tableScrollPane;

    // ==================== 构造方法 ====================

    public ExecuteBatchPanel() {
        initLayout();
        initTopPanel();
        initContentPanel();
        initStatusBar();
        loadDataSources();
    }

    // ==================== 布局初始化 ====================

    private void initLayout() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeUtils.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(1100, 750));
    }

    /**
     * 顶部面板：数据源 + 批次选择 + 操作按钮
     */
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "⚙️ 执行控制",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        ThemeUtils.FONT_SUBTITLE,
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel paramPanel = new JPanel(new GridBagLayout());
        paramPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 数据源
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        paramPanel.add(createLabel("数据源:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setPreferredSize(new Dimension(200, 32));
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setBackground(Color.WHITE);
        dataSourceCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        dataSourceCombo.addActionListener(e -> loadBatchIds());
        paramPanel.add(dataSourceCombo, gbc);

        // 批次选择
        gbc.gridx = 2;
        gbc.weightx = 0;
        paramPanel.add(createLabel("选择批次:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.5;
        batchCombo = new JComboBox<>();
        batchCombo.setPreferredSize(new Dimension(280, 32));
        batchCombo.setFont(ThemeUtils.FONT_NORMAL);
        batchCombo.setBackground(Color.WHITE);
        batchCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        paramPanel.add(batchCombo, gbc);

        // 第2行按钮
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        queryBtn = createPrimaryButton("查询明细", "search");
        queryBtn.setPreferredSize(new Dimension(110, 34));
        queryBtn.addActionListener(e -> queryDetail());
        paramPanel.add(queryBtn, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        executeBtn = createSuccessButton("执行批次", "play");
        executeBtn.setPreferredSize(new Dimension(110, 34));
        executeBtn.addActionListener(e -> executeSelectedBatch());
        paramPanel.add(executeBtn, gbc);

        topPanel.add(paramPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * 中间区域：明细表格（上） + 日志（下）
     */
    private void initContentPanel() {
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerLocation(0.6);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setDividerSize(6);

        JPanel tablePanel = createTablePanel();
        JPanel logPanel = createLogPanel();
        logPanel.setMinimumSize(new Dimension(0, 150));

        splitPane.setTopComponent(tablePanel);
        splitPane.setBottomComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * 创建明细表格面板 - 启用列排序，支持横向滚动
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        detailTableModel = new DetailTableModel();
        detailTable = new JTable(detailTableModel);

        // 启用列排序
        detailTable.setAutoCreateRowSorter(true);
        // 关闭自动调整列宽，启用横向滚动
        detailTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 表格基础样式
        detailTable.setRowHeight(30);
        detailTable.setFont(ThemeUtils.FONT_NORMAL);
        detailTable.setBackground(Color.WHITE);
        detailTable.setSelectionBackground(new Color(220, 235, 250));
        detailTable.setSelectionForeground(ThemeUtils.COLOR_TEXT);
        detailTable.setGridColor(new Color(230, 235, 240));
        detailTable.setShowGrid(true);
        detailTable.setShowVerticalLines(false);
        detailTable.setIntercellSpacing(new Dimension(10, 2));

        // 表头样式
        detailTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        detailTable.getTableHeader().setBackground(new Color(240, 243, 248));
        detailTable.getTableHeader().setForeground(ThemeUtils.COLOR_TEXT);
        detailTable.getTableHeader().setPreferredSize(new Dimension(0, 32));
        detailTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ThemeUtils.COLOR_PRIMARY));

        // 居中对齐
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < detailTable.getColumnCount(); i++) {
            detailTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 滚动面板
        tableScrollPane = new JScrollPane(detailTable);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tableScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        JScrollBar verticalBar = tableScrollPane.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(10, 0));
        verticalBar.setBackground(new Color(248, 245, 240));
        verticalBar.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar horizontalBar = tableScrollPane.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 10));
        horizontalBar.setBackground(new Color(248, 245, 240));
        horizontalBar.setBorder(BorderFactory.createEmptyBorder());

        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "📋 批次明细数据（点击列头排序）",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Microsoft YaHei", Font.BOLD, 14),
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        tableScrollPane.getViewport().setOpaque(true);

        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 调整列宽以适应内容
     */
    private void adjustColumnWidths() {
        if (detailTable.getRowCount() == 0) {
            setDefaultColumnWidths();
            return;
        }

        try {
            int minWidth = 60;
            int maxWidth = 300;
            int headerPadding = 15;

            for (int col = 0; col < detailTable.getColumnCount(); col++) {
                TableColumn column = detailTable.getColumnModel().getColumn(col);
                String headerText = detailTable.getColumnName(col);
                int headerWidth = getTextWidth(headerText) + headerPadding;

                int maxDataWidth = headerWidth;
                for (int row = 0; row < Math.min(detailTable.getRowCount(), 200); row++) {
                    Object value = detailTable.getValueAt(row, col);
                    if (value != null) {
                        int textWidth = getTextWidth(value.toString()) + 10;
                        if (textWidth > maxDataWidth) {
                            maxDataWidth = textWidth;
                        }
                    }
                }

                int preferredWidth = Math.min(Math.max(maxDataWidth, minWidth), maxWidth);
                column.setPreferredWidth(preferredWidth);
                column.setMinWidth(Math.min(preferredWidth, 80));
                column.setMaxWidth(maxWidth);
            }
        } catch (Exception e) {
            setDefaultColumnWidths();
        }
    }

    private int getTextWidth(String text) {
        Font font = detailTable.getFont();
        FontMetrics fm = detailTable.getFontMetrics(font);
        return fm.stringWidth(text);
    }

    private void setDefaultColumnWidths() {
        int[] widths = {60, 100, 100, 120, 120, 50, 70, 70, 70, 70, 80};
        for (int i = 0; i < Math.min(widths.length, detailTable.getColumnCount()); i++) {
            TableColumn col = detailTable.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            col.setMinWidth(Math.min(widths[i], 80));
        }
    }

    /**
     * 创建日志面板 - 带初始提示
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMinimumSize(new Dimension(0, 150));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(248, 245, 240));
        logArea.setForeground(ThemeUtils.COLOR_TEXT);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        JScrollBar verticalBar = scroll.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(10, 0));
        verticalBar.setBackground(new Color(248, 245, 240));
        verticalBar.setBorder(BorderFactory.createEmptyBorder());

        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "📋 执行日志",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Microsoft YaHei", Font.BOLD, 14),
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scroll.getViewport().setBackground(new Color(248, 245, 240));
        scroll.getViewport().setOpaque(true);

        // 初始化日志
        logArea.append("═══════════════════════════════════════════════════════════\n");
        logArea.append("  📌 执行日志区域\n");
        logArea.append("  ─────────────────────────────────────────────────────────\n");
        logArea.append("  1. 选择数据源 → 自动加载批次列表\n");
        logArea.append("  2. 选择批次 → 点击「查询明细」查看数据\n");
        logArea.append("  3. 选择批次 → 点击「执行批次」执行检查和清洗\n");
        logArea.append("═══════════════════════════════════════════════════════════\n");

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ==================== 辅助方法 ====================

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeUtils.FONT_NORMAL);
        label.setForeground(ThemeUtils.COLOR_TEXT);
        label.setPreferredSize(new Dimension(70, 28));
        return label;
    }

    private JButton createPrimaryButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(ThemeUtils.COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSuccessButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(ThemeUtils.COLOR_SUCCESS);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * 底部状态栏
     */
    private void initStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("💡 点击列头可排序");
        statusLabel.setFont(ThemeUtils.FONT_SMALL);
        statusLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        statusPanel.add(statusLabel);

        add(statusPanel, BorderLayout.SOUTH);
    }

    // ==================== 数据加载 ====================

    private void loadDataSources() {
        allDataSources = DataSourceStore.load();
        dataSourceCombo.removeAllItems();
        dataSourceCombo.addItem("-- 请选择数据源 --");

        for (DataSource ds : allDataSources) {
            dataSourceCombo.addItem(ds.getName());
        }

        dataSourceCombo.setSelectedIndex(0);
        batchCombo.removeAllItems();
        detailTableModel.setData(new ArrayList<>());
        setStatus("请选择数据源");
    }

    private DataSource getSelectedDataSource() {
        String name = (String) dataSourceCombo.getSelectedItem();
        if (name == null || name.startsWith("--")) {
            return null;
        }
        return allDataSources.stream()
                .filter(ds -> name.equals(ds.getName()))
                .findFirst()
                .orElse(null);
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }

    // ==================== 业务逻辑 ====================

    private void loadBatchIds() {
        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            logArea.append("请选择数据源\n");
            setStatus("请选择数据源");
            return;
        }

        setStatus("正在加载批次...");
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return detailDao.findAllBatchIds(ds);
            }

            @Override
            protected void done() {
                try {
                    List<String> ids = get();
                    batchCombo.removeAllItems();
                    if (ids.isEmpty()) {
                        batchCombo.addItem("-- 暂无批次 --");
                        logArea.append("暂无批次记录\n");
                        setStatus("暂无批次");
                    } else {
                        for (String id : ids) {
                            batchCombo.addItem(id);
                        }
                        logArea.append("已加载 " + ids.size() + " 个批次\n");
                        setStatus("加载成功，共 " + ids.size() + " 个批次");
                        batchCombo.setSelectedIndex(0);
                    }
                } catch (Exception e) {
                    logArea.append("加载批次失败: " + e.getCause().getMessage() + "\n");
                    setStatus("加载失败");
                    JOptionPane.showMessageDialog(ExecuteBatchPanel.this,
                            "加载批次失败: " + e.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void queryDetail() {
        String batchId = (String) batchCombo.getSelectedItem();
        if (batchId == null || batchId.isEmpty() || batchId.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "请先选择一个批次", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentBatchId = batchId;
        loadDetailData(batchId);
    }

    private void loadDetailData(String batchId) {
        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            setStatus("请选择数据源");
            return;
        }

        setStatus("正在加载明细数据...");
        new SwingWorker<List<DataCheckDetail>, Void>() {
            @Override
            protected List<DataCheckDetail> doInBackground() throws Exception {
                return detailDao.findByBatchId(batchId, ds);
            }

            @Override
            protected void done() {
                try {
                    List<DataCheckDetail> list = get();
                    detailTableModel.setData(list);
                    setStatus("加载成功，批次: " + batchId + "，共 " + list.size() + " 条记录");
                    logArea.append("已加载批次 " + batchId + " 的明细数据，共 " + list.size() + " 条记录\n");
                    // 加载完成后调整列宽
                    SwingUtilities.invokeLater(() -> adjustColumnWidths());
                } catch (Exception e) {
                    setStatus("加载失败");
                    logArea.append("加载明细失败: " + e.getCause().getMessage() + "\n");
                    JOptionPane.showMessageDialog(ExecuteBatchPanel.this,
                            "加载明细失败: " + e.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    detailTableModel.setData(new ArrayList<>());
                }
            }
        }.execute();
    }

    private void executeSelectedBatch() {
        String batchId = (String) batchCombo.getSelectedItem();
        if (batchId == null || batchId.isEmpty() || batchId.startsWith("--")) {
            logArea.append("请选择一个批次\n");
            setStatus("请选择批次");
            return;
        }

        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            logArea.append("请选择数据源\n");
            setStatus("请选择数据源");
            return;
        }

        logArea.append("═══════════════════════════════════════════════════════════\n");
        logArea.append("▶ 开始执行批次: " + batchId + "\n");
        logArea.append("───────────────────────────────────────────────────────────\n");
        setStatus("正在执行批次...");
        executeBtn.setEnabled(false);

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                procDao.executeBatch(batchId, ds);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    logArea.append("✅ 批次执行成功！\n");
                    setStatus("执行成功");
                    displayBatchSummary(batchId, ds);
                    loadDetailData(batchId);
                } catch (Exception ex) {
                    logArea.append("❌ 执行失败: " + ex.getCause().getMessage() + "\n");
                    setStatus("执行失败");
                } finally {
                    logArea.append("═══════════════════════════════════════════════════════════\n\n");
                    executeBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void displayBatchSummary(String batchId, DataSource ds) {
        try {
            List<DataCheckDetail> list = detailDao.findByBatchId(batchId, ds);
            long total = list.size();
            long checkSuccess = list.stream().filter(d -> "S".equals(d.getCheckStatus())).count();
            long checkError = list.stream().filter(d -> "E".equals(d.getCheckStatus())).count();
            long checkWaiting = list.stream().filter(d -> "W".equals(d.getCheckStatus())).count();
            long cleanSuccess = list.stream().filter(d -> "S".equals(d.getCleanStatus())).count();
            long cleanError = list.stream().filter(d -> "E".equals(d.getCleanStatus())).count();
            long cleanWaiting = list.stream().filter(d -> "W".equals(d.getCleanStatus())).count();

            logArea.append("\n📊 执行汇总:\n");
            logArea.append("  ┌─────────────────────────────────────────────────┐\n");
            logArea.append("  │  总任务数: " + String.format("%-4d", total) + "                               │\n");
            logArea.append("  ├─────────────────────────────────────────────────┤\n");
            logArea.append("  │  检查阶段: 成功=" + String.format("%-3d", checkSuccess) +
                    "  失败=" + String.format("%-3d", checkError) +
                    "  不需要检查=" + String.format("%-3d", checkWaiting) + "     │\n");
            logArea.append("  │  清洗阶段: 成功=" + String.format("%-3d", cleanSuccess) +
                    "  失败=" + String.format("%-3d", cleanError) +
                    "  不需要检查=" + String.format("%-3d", cleanWaiting) + "     │\n");
            logArea.append("  └─────────────────────────────────────────────────┘\n");
        } catch (SQLException e) {
            logArea.append("获取明细汇总失败: " + e.getMessage() + "\n");
        }
    }

    // ================================================================
    //  表格模型
    // ================================================================

    private static class DetailTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "log_id", "表所有者", "表名", "列名", "规则名称",
                "优先级", "检查状态", "异常行数", "清洗状态", "清洗行数", "执行标志"
        };
        private List<DataCheckDetail> data = new ArrayList<>();

        void setData(List<DataCheckDetail> list) {
            this.data = list;
            fireTableDataChanged();
        }

        List<DataCheckDetail> getData() {
            return data;
        }

        DataCheckDetail getRowData(int row) {
            return data.get(row);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMNS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            DataCheckDetail d = data.get(row);
            switch (col) {
                case 0:  return d.getLogId();
                case 1:  return d.getTableOwner();
                case 2:  return d.getTableName();
                case 3:  return d.getColumnName();
                case 4:  return d.getRuleName();
                case 5:  return d.getPriority();
                case 6:  return d.getCheckStatus();
                case 7:  return d.getCheckRowCount();
                case 8:  return d.getCleanStatus();
                case 9:  return d.getCleanRowCount();
                case 10: return d.getExecFlag();
                default: return null;
            }
        }
    }
}