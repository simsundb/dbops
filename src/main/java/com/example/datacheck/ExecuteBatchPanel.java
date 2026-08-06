package com.example.datacheck;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.utils.ThemeUtils;
import com.example.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行批次面板 - 美化版
 */
public class ExecuteBatchPanel extends JPanel {

    // ==================== 组件定义 ====================
    private JComboBox<String> batchCombo;
    private JButton executeBtn, refreshBtn;
    private JTextArea logArea;
    private DataCheckDetailDao detailDao = new DataCheckDetailDao();
    private DataCheckProcedureDao procDao = new DataCheckProcedureDao();

    private JComboBox<String> dataSourceCombo;
    private List<DataSource> allDataSources = new ArrayList<>();
    private JLabel statusLabel;

    // ==================== 构造方法 ====================

    public ExecuteBatchPanel() {
        initLayout();
        initTopPanel();
        initLogArea();
        initStatusBar();
        loadDataSources();
    }

    // ==================== 布局初始化 ====================

    private void initLayout() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeUtils.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * 顶部面板：数据源 + 批次选择
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

        // 参数面板
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
        dataSourceCombo.setPreferredSize(new Dimension(220, 32));
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

        // 执行按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        executeBtn = createPrimaryButton("执行批次", "play");
        executeBtn.addActionListener(e -> executeSelectedBatch());
        paramPanel.add(executeBtn, gbc);

        // 刷新按钮
        gbc.gridx = 5;
        gbc.weightx = 0;
        refreshBtn = createStyledButton("刷新列表", "refresh");
        refreshBtn.addActionListener(e -> loadBatchIds());
        paramPanel.add(refreshBtn, gbc);

        topPanel.add(paramPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * 创建标签
     */
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeUtils.FONT_NORMAL);
        label.setForeground(ThemeUtils.COLOR_TEXT);
        return label;
    }

    /**
     * 创建主要按钮
     */
    private JButton createPrimaryButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(ThemeUtils.COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 34));
        return btn;
    }

    /**
     * 创建样式按钮（刷新）
     */
    private JButton createStyledButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.get(icon, 16, ThemeUtils.COLOR_PRIMARY));
        btn.setBackground(ThemeUtils.COLOR_BG_CARD);
        btn.setForeground(ThemeUtils.COLOR_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 34));
        return btn;
    }

    /**
     * 日志区域
     */
    private void initLogArea() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBackground(new Color(248, 245, 240));
        logArea.setForeground(ThemeUtils.COLOR_TEXT);
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);

        // 滚动条策略
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 滚动速度
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        // 美化滚动条
        JScrollBar verticalBar = scroll.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(10, 0));
        verticalBar.setBackground(new Color(248, 245, 240));
        verticalBar.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar horizontalBar = scroll.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 10));
        horizontalBar.setBackground(new Color(248, 245, 240));
        horizontalBar.setBorder(BorderFactory.createEmptyBorder());

        // 滚动面板边框
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

        add(scroll, BorderLayout.CENTER);
    }

    /**
     * 底部状态栏
     */
    private void initStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("就绪");
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
        logArea.setText("");
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
                        // 默认选中第一个
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
                    "  等待=" + String.format("%-3d", checkWaiting) + "     │\n");
            logArea.append("  │  清洗阶段: 成功=" + String.format("%-3d", cleanSuccess) +
                    "  失败=" + String.format("%-3d", cleanError) +
                    "  等待=" + String.format("%-3d", cleanWaiting) + "     │\n");
            logArea.append("  └─────────────────────────────────────────────────┘\n");
        } catch (SQLException e) {
            logArea.append("获取明细汇总失败: " + e.getMessage() + "\n");
        }
    }
}