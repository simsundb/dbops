package com.example.datacheck;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.utils.ThemeUtils;
import com.example.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成检查脚本面板 - 美化版
 */
public class GenerateScriptPanel extends JPanel {

    // ==================== 组件定义 ====================
    private JTextField ownerField, tableNameField;
    private JComboBox<String> dbTypeCombo;
    private JButton generateBtn;
    private JTable detailTable;
    private DetailTableModel detailTableModel;
    private DataCheckDetailDao detailDao = new DataCheckDetailDao();
    private DataCheckProcedureDao procDao = new DataCheckProcedureDao();

    private JComboBox<String> dataSourceCombo;
    private List<DataSource> allDataSources = new ArrayList<>();
    private String currentBatchId;
    private JLabel statusLabel;

    // ==================== 构造方法 ====================

    public GenerateScriptPanel() {
        initLayout();
        initTopPanel();
        initTable();
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
     * 顶部面板：数据源 + 参数输入
     */
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "📝 生成脚本参数",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        ThemeUtils.FONT_SUBTITLE,
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        // 参数输入面板
        JPanel paramPanel = new JPanel(new GridBagLayout());
        paramPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // ========== 第1行：数据源 + 表所有者 + 表名 ==========
        // 数据源标签
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        paramPanel.add(createLabel("数据源:"), gbc);

        // 数据源下拉框
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setPreferredSize(new Dimension(180, 32));
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setBackground(Color.WHITE);
        dataSourceCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        dataSourceCombo.addActionListener(e -> loadLatestBatch());
        paramPanel.add(dataSourceCombo, gbc);

        // 表所有者标签
        gbc.gridx = 2;
        gbc.weightx = 0;
        paramPanel.add(createLabel("表所有者:"), gbc);

        // 表所有者输入框
        gbc.gridx = 3;
        gbc.weightx = 1.2;
        ownerField = new JTextField(20);
        ownerField.setFont(ThemeUtils.FONT_NORMAL);
        ownerField.setPreferredSize(new Dimension(180, 32));
        ownerField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        paramPanel.add(ownerField, gbc);

        // 表名标签
        gbc.gridx = 4;
        gbc.weightx = 0;
        paramPanel.add(createLabel("表名:"), gbc);

        // 表名输入框
        gbc.gridx = 5;
        gbc.weightx = 1.5;
        tableNameField = new JTextField(20);
        tableNameField.setFont(ThemeUtils.FONT_NORMAL);
        tableNameField.setPreferredSize(new Dimension(200, 32));
        tableNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        paramPanel.add(tableNameField, gbc);

        // ========== 第2行：数据库类型 + 按钮 ==========
        row++;

        // 数据库类型标签
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        paramPanel.add(createLabel("数据库类型:"), gbc);

        // 数据库类型下拉框
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        dbTypeCombo = new JComboBox<>(new String[]{"ORACLE", "GAUSSDB"});
        dbTypeCombo.setPreferredSize(new Dimension(180, 32));
        dbTypeCombo.setFont(ThemeUtils.FONT_NORMAL);
        dbTypeCombo.setBackground(Color.WHITE);
        dbTypeCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        paramPanel.add(dbTypeCombo, gbc);

        // 占位
        gbc.gridx = 2;
        gbc.weightx = 0.2;
        paramPanel.add(Box.createHorizontalStrut(20), gbc);

        // 生成按钮
        gbc.gridx = 3;
        gbc.weightx = 0;
        generateBtn = createPrimaryButton("生成脚本", "play");
        generateBtn.setPreferredSize(new Dimension(120, 34));
        generateBtn.addActionListener(e -> generateScript());
        paramPanel.add(generateBtn, gbc);

        // 刷新按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton refreshBtn = createStyledButton("刷新明细", "refresh");
        refreshBtn.setPreferredSize(new Dimension(120, 34));
        refreshBtn.addActionListener(e -> loadLatestBatch());
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
        label.setPreferredSize(new Dimension(70, 28));
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
        return btn;
    }

    /**
     * 创建样式按钮
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
        return btn;
    }

    /**
     * 表格区域
     */
    private void initTable() {
        detailTableModel = new DetailTableModel();
        detailTable = new JTable(detailTableModel);

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

        // 对齐方式
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < detailTable.getColumnCount(); i++) {
            detailTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 设置列宽
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        detailTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        detailTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        detailTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        detailTable.getColumnModel().getColumn(8).setPreferredWidth(80);

        // ==================== 表格交互：切换执行标志 ====================
        initTableActions();

        // ==================== 滚动面板配置 ====================
        JScrollPane scroll = new JScrollPane(detailTable);

        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        JScrollBar verticalBar = scroll.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(10, 0));
        verticalBar.setBackground(new Color(248, 245, 240));
        verticalBar.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar horizontalBar = scroll.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 10));
        horizontalBar.setBackground(new Color(248, 245, 240));
        horizontalBar.setBorder(BorderFactory.createEmptyBorder());

        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "📋 明细记录（双击行或单击执行标志列快速切换）",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Microsoft YaHei", Font.BOLD, 14),
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getViewport().setOpaque(true);

        add(scroll, BorderLayout.CENTER);

        // ==================== 底部操作按钮 ====================
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton editBtn = createPrimaryButton("编辑选中", "edit");
        JButton deleteBtn = createDangerButton("删除选中", "trash");
        JButton saveBtn = createSuccessButton("保存修改", "save");
        JButton saveRefreshBtn = createSaveRefreshButton("保存并刷新", "save");

        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        saveBtn.addActionListener(e -> saveChanges(false));
        saveRefreshBtn.addActionListener(e -> saveChanges(true));

        bottomPanel.add(editBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(saveBtn);
        bottomPanel.add(saveRefreshBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 表格交互：双击行或单击执行标志列切换 Y/N
     */
    private void initTableActions() {
        // 方式1：双击行切换执行标志
        detailTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = detailTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        toggleExecFlag(row);
                    }
                }
            }
        });

        // 方式2：单击执行标志列（第9列，索引8）切换
        detailTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = detailTable.rowAtPoint(e.getPoint());
                int col = detailTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 8) {
                    toggleExecFlag(row);
                }
            }
        });
    }

    /**
     * 切换指定行的执行标志 (Y ↔ N)
     */
    private void toggleExecFlag(int row) {
        DataCheckDetail detail = detailTableModel.getRowData(row);
        String currentFlag = detail.getExecFlag();
        String newFlag = "Y".equals(currentFlag) ? "N" : "Y";
        detail.setExecFlag(newFlag);
        detailTableModel.fireTableRowsUpdated(row, row);
        setStatus("执行标志已切换: " + currentFlag + " → " + newFlag + "（请点击保存按钮生效）");
    }

    /**
     * 创建成功按钮
     */
    private JButton createSuccessButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(ThemeUtils.COLOR_SUCCESS);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 32));
        return btn;
    }

    /**
     * 创建保存并刷新按钮（金色）
     */
    private JButton createSaveRefreshButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(new Color(200, 155, 75)); // 藤黄色
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 32));
        return btn;
    }

    /**
     * 创建危险按钮
     */
    private JButton createDangerButton(String text, String icon) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btn.setIcon(SvgIconUtils.getWhite(icon, 14));
        btn.setBackground(new Color(190, 65, 55));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 32));
        return btn;
    }

    /**
     * 底部状态栏
     */
    private void initStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("💡 双击行或单击执行标志列切换 Y/N，点击保存按钮生效");
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

    private void generateScript() {
        String owner = ownerField.getText().trim().toUpperCase();
        String table = tableNameField.getText().trim().toUpperCase();
        String dbType = (String) dbTypeCombo.getSelectedItem();

        if (owner.isEmpty() || table.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入表所有者和表名", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            JOptionPane.showMessageDialog(this, "请选择数据源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStatus("正在生成脚本...");
        generateBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                procDao.generateScript(owner, table, dbType, ds);
                return null;
            }

            @Override
            protected void done() {
                generateBtn.setEnabled(true);
                try {
                    get();
                    setStatus("生成成功");
                    JOptionPane.showMessageDialog(GenerateScriptPanel.this, "脚本生成成功！");
                    loadLatestBatch();
                } catch (Exception ex) {
                    setStatus("生成失败");
                    JOptionPane.showMessageDialog(GenerateScriptPanel.this,
                            "生成失败: " + ex.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadLatestBatch() {
        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            detailTableModel.setData(new ArrayList<>());
            setStatus("请选择数据源");
            return;
        }

        setStatus("正在加载明细...");
        new SwingWorker<List<DataCheckDetail>, Void>() {
            @Override
            protected List<DataCheckDetail> doInBackground() throws Exception {
                String batch = detailDao.getLatestBatchId(ds);
                if (batch != null) {
                    currentBatchId = batch;
                    return detailDao.findByBatchId(batch, ds);
                } else {
                    return new ArrayList<>();
                }
            }

            @Override
            protected void done() {
                try {
                    List<DataCheckDetail> list = get();
                    detailTableModel.setData(list);
                    setStatus("加载成功，共 " + list.size() + " 条记录" +
                            (currentBatchId != null ? " (批次: " + currentBatchId + ")" : ""));
                } catch (Exception e) {
                    setStatus("加载失败");
                    JOptionPane.showMessageDialog(GenerateScriptPanel.this,
                            "加载明细失败: " + e.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    detailTableModel.setData(new ArrayList<>());
                }
            }
        }.execute();
    }

    private void editSelected() {
        int row = detailTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择一条记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DataCheckDetail detail = detailTableModel.getRowData(row);
        String newFlag = (String) JOptionPane.showInputDialog(this,
                "修改执行标志 (Y/N):",
                "编辑执行标志",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Y", "N"},
                detail.getExecFlag());

        if (newFlag != null) {
            detail.setExecFlag(newFlag);
            detailTableModel.fireTableRowsUpdated(row, row);
            setStatus("已修改执行标志（请点击保存按钮生效）");
        }
    }

    private void deleteSelected() {
        int row = detailTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择一条记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DataCheckDetail detail = detailTableModel.getRowData(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定删除 log_id = " + detail.getLogId() + " ？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setStatus("正在删除...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DataSource ds = getSelectedDataSource();
                detailDao.delete(detail.getLogId(), ds);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("删除成功");
                    loadLatestBatch();
                } catch (Exception ex) {
                    setStatus("删除失败");
                    JOptionPane.showMessageDialog(GenerateScriptPanel.this,
                            "删除失败: " + ex.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * 保存修改
     * @param refresh 是否在保存后刷新数据
     */
    private void saveChanges(boolean refresh) {
        List<DataCheckDetail> list = detailTableModel.getData();
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有需要保存的数据", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            JOptionPane.showMessageDialog(this, "请选择数据源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStatus(refresh ? "正在保存并刷新..." : "正在保存...");
        // 禁用所有底部按钮
        setBottomButtonsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (DataCheckDetail d : list) {
                    detailDao.update(d, ds);
                }
                return null;
            }

            @Override
            protected void done() {
                setBottomButtonsEnabled(true);
                try {
                    get();
                    if (refresh) {
                        setStatus("保存成功，已刷新数据");
                        JOptionPane.showMessageDialog(GenerateScriptPanel.this, "保存成功！数据已刷新");
                        loadLatestBatch();
                    } else {
                        setStatus("保存成功");
                        JOptionPane.showMessageDialog(GenerateScriptPanel.this, "修改已保存！");
                    }
                } catch (Exception ex) {
                    setStatus("保存失败");
                    JOptionPane.showMessageDialog(GenerateScriptPanel.this,
                            "保存失败: " + ex.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * 控制底部按钮启用/禁用
     */
    private void setBottomButtonsEnabled(boolean enabled) {
        // 获取底部面板（索引2是底部面板）
        Component bottomComp = getComponent(2);
        if (bottomComp instanceof JPanel) {
            Component[] components = ((JPanel) bottomComp).getComponents();
            for (Component comp : components) {
                if (comp instanceof JButton) {
                    comp.setEnabled(enabled);
                }
            }
        }
    }

    // ================================================================
    //  表格模型
    // ================================================================

    private static class DetailTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "log_id", "列名", "规则名称", "优先级", "检查状态", "异常行数", "清洗状态", "清洗行数", "执行标志"
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

        void removeRow(int row) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
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
                case 0:
                    return d.getLogId();
                case 1:
                    return d.getColumnName();
                case 2:
                    return d.getRuleName();
                case 3:
                    return d.getPriority();
                case 4:
                    return d.getCheckStatus();
                case 5:
                    return d.getCheckRowCount();
                case 6:
                    return d.getCleanStatus();
                case 7:
                    return d.getCleanRowCount();
                case 8:
                    return d.getExecFlag();
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 8;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 8) {
                DataCheckDetail d = data.get(row);
                d.setExecFlag((String) value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}