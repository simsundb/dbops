package com.sunzh.datacheck;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.components.WidgetFactory;
import com.sunzh.utils.ThemeUtils;
import com.sunzh.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据校验规则配置面板 - 美化版
 * 支持点击列头排序
 */
public class RuleConfigPanel extends JPanel {

    // ==================== 组件定义 ====================
    private JTable table;
    private ConfigTableModel tableModel;
    private DataCheckConfigDao dao = new DataCheckConfigDao();

    private JComboBox<String> dataSourceCombo;
    private List<DataSource> allDataSources = new ArrayList<>();
    private JButton refreshBtn;
    private JLabel statusLabel;

    // ==================== 构造方法 ====================

    public RuleConfigPanel() {
        initLayout();
        initTopPanel();
        initTable();
        initStatusBar();
        initActions();
        loadDataSources();
    }

    // ==================== 布局初始化 ====================

    private void initLayout() {
        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeUtils.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * 顶部面板：数据源选择 + 操作按钮
     */
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "数据源 & 操作",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        ThemeUtils.FONT_SUBTITLE,
                        ThemeUtils.COLOR_PRIMARY
                ),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        topPanel.add(buildLeftPanel(), BorderLayout.WEST);
        topPanel.add(buildRightPanel(), BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    /**
     * 左侧：数据源选择 + 刷新按钮
     */
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel("数据源:");
        label.setFont(ThemeUtils.FONT_NORMAL);
        label.setForeground(ThemeUtils.COLOR_TEXT);
        panel.add(label);

        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setPreferredSize(new Dimension(250, 32));
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setBackground(Color.WHITE);
        dataSourceCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        dataSourceCombo.addActionListener(e -> refreshData());
        panel.add(dataSourceCombo);

        refreshBtn = createStyledButton("刷新", "refresh");
        refreshBtn.addActionListener(e -> refreshData());
        panel.add(refreshBtn);

        return panel;
    }

    /**
     * 右侧：新增、编辑、删除按钮
     */
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton addBtn = WidgetFactory.primaryButton("新增", "plus");
        JButton editBtn = WidgetFactory.primaryButton("编辑", "edit");
        JButton delBtn = WidgetFactory.dangerButton("删除", "trash");

        addBtn.addActionListener(e -> showConfigDialog(null));
        editBtn.addActionListener(this::handleEdit);
        delBtn.addActionListener(this::handleDelete);

        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(delBtn);

        return panel;
    }

    /**
     * 创建统一样式的按钮（主要按钮）
     */
    private JButton createPrimaryButton(String text, String icon) {
        return WidgetFactory.primaryButton(text, icon);
    }

    /**
     * 创建危险按钮（删除）
     */
    private JButton createDangerButton(String text, String icon) {
        return WidgetFactory.dangerButton(text, icon);
    }

    /**
     * 创建样式按钮（刷新）
     */
    private JButton createStyledButton(String text, String icon) {
        return WidgetFactory.outlineButton(text, icon, ThemeUtils.COLOR_PRIMARY);
    }

    /**
     * 表格区域 - 增加滚动条和列排序
     */
    private void initTable() {
        tableModel = new ConfigTableModel();
        table = new JTable(tableModel);

        // ★★★ 启用列排序 ★★★
        table.setAutoCreateRowSorter(true);

        // 表格基础样式
        table.setRowHeight(30);
        table.setFont(ThemeUtils.FONT_NORMAL);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(220, 235, 250));
        table.setSelectionForeground(ThemeUtils.COLOR_TEXT);
        table.setGridColor(new Color(230, 235, 240));
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(10, 2));

        // 表头样式
        table.getTableHeader().setFont(ThemeUtils.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(ThemeUtils.COLOR_TABLE_HEADER_BG);
        table.getTableHeader().setForeground(ThemeUtils.COLOR_TABLE_HEADER_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // 对齐方式
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(150);
        table.getColumnModel().getColumn(7).setPreferredWidth(150);
        table.getColumnModel().getColumn(8).setPreferredWidth(50);
        table.getColumnModel().getColumn(9).setPreferredWidth(120);

        // ==================== 滚动面板配置 ====================
        JScrollPane scroll = new JScrollPane(table);

        // 【1】设置滚动条策略
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 【2】设置滚动条速度
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        // 【3】美化垂直滚动条
        JScrollBar verticalBar = scroll.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(10, 0));
        verticalBar.setBackground(ThemeUtils.COLOR_BG);
        verticalBar.setBorder(BorderFactory.createEmptyBorder());

        // 【4】美化水平滚动条
        JScrollBar horizontalBar = scroll.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 10));
        horizontalBar.setBackground(ThemeUtils.COLOR_BG);
        horizontalBar.setBorder(BorderFactory.createEmptyBorder());

        // 【5】滚动面板边框（增加排序提示）
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                        "📋 规则列表（点击列头排序）",
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

    // ==================== 事件处理 ====================

    private void initActions() {
        // 事件已在按钮创建时绑定
    }

    private void handleEdit(ActionEvent e) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择一条记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // ★★★ 将视图索引转换为模型索引 ★★★
        int modelRow = table.convertRowIndexToModel(viewRow);
        showConfigDialog(tableModel.getRowData(modelRow));
    }

    private void handleDelete(ActionEvent e) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择一条记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ★★★ 将视图索引转换为模型索引 ★★★
        int modelRow = table.convertRowIndexToModel(viewRow);
        DataCheckConfig config = tableModel.getRowData(modelRow);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确定删除规则ID = " + config.getRuleId() + " ？",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setStatus("正在删除...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DataSource ds = getSelectedDataSource();
                dao.delete(config.getRuleId(), ds);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("删除成功");
                    refreshData();
                } catch (Exception ex) {
                    setStatus("删除失败");
                    JOptionPane.showMessageDialog(RuleConfigPanel.this,
                            "删除失败: " + ex.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
        tableModel.setData(new ArrayList<>());
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

    /**
     * 刷新表格数据（异步加载）
     */
    private void refreshData() {
        DataSource ds = getSelectedDataSource();
        if (ds == null) {
            tableModel.setData(new ArrayList<>());
            setStatus("请选择数据源");
            return;
        }

        setStatus("正在加载数据...");
        refreshBtn.setEnabled(false);

        new SwingWorker<List<DataCheckConfig>, Void>() {
            @Override
            protected List<DataCheckConfig> doInBackground() throws Exception {
                return dao.findAll(ds);
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                try {
                    List<DataCheckConfig> list = get();
                    tableModel.setData(list);
                    setStatus("加载成功，共 " + list.size() + " 条记录");
                } catch (Exception e) {
                    setStatus("加载失败");
                    JOptionPane.showMessageDialog(
                            RuleConfigPanel.this,
                            "加载数据失败: " + e.getCause().getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE
                    );
                    tableModel.setData(new ArrayList<>());
                }
            }
        }.execute();
    }

    private void showConfigDialog(DataCheckConfig config) {
        ConfigDialog dialog = new ConfigDialog(SwingUtilities.getWindowAncestor(this), config);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    // ================================================================
    //  表格模型
    // ================================================================

    private static class ConfigTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {
                "规则ID", "数据库类型", "规则类型", "规则名称", "启用",
                "数据类型", "检查条件", "清洗表达式", "优先级", "描述"
        };

        private List<DataCheckConfig> data = new ArrayList<>();

        void setData(List<DataCheckConfig> list) {
            this.data = list;
            fireTableDataChanged();
        }

        DataCheckConfig getRowData(int row) {
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
            DataCheckConfig c = data.get(row);
            switch (col) {
                case 0:  return c.getRuleId();
                case 1:  return c.getDbType();
                case 2:  return c.getRuleType();
                case 3:  return c.getRuleName();
                case 4:  return c.getExecFlag();
                case 5:  return c.getApplyDataType();
                case 6:  return c.getCheckCondition();
                case 7:  return c.getCleanExpression();
                case 8:  return c.getPriority();
                case 9:  return c.getRuleDesc();
                default: return null;
            }
        }
    }

    // ================================================================
    //  编辑对话框（保持不变）
    // ================================================================

    private class ConfigDialog extends JDialog {

        private DataCheckConfig config;
        private boolean saved = false;

        private JTextField ruleIdField;
        private JTextField ruleTypeField;
        private JTextField ruleNameField;
        private JTextField checkConditionField;
        private JTextField cleanExpressionField;
        private JTextField priorityField;
        private JTextField ruleDescField;

        private JComboBox<String> dbTypeCombo;
        private JComboBox<String> execFlagCombo;
        private JComboBox<String> applyDataTypeCombo;

        public ConfigDialog(Window owner, DataCheckConfig config) {
            super(owner, config == null ? "新增规则" : "编辑规则", ModalityType.APPLICATION_MODAL);
            this.config = config != null ? config : new DataCheckConfig();

            setBackground(Color.WHITE);
            setLayout(new BorderLayout(10, 10));

            JPanel contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0;

            buildForm(contentPanel, gbc);

            JPanel btnPanel = buildButtonPanel();
            add(contentPanel, BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);

            populateFormData();
            pack();
            setMinimumSize(new Dimension(550, 450));
            setLocationRelativeTo(owner);
        }

        private void buildForm(JPanel panel, GridBagConstraints gbc) {
            int row = 0;

            gbc.gridx = 0; gbc.gridy = row;
            panel.add(createLabel("规则ID:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            ruleIdField = createTextField(12, false);
            panel.add(ruleIdField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("数据库类型:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            dbTypeCombo = new JComboBox<>(new String[]{"ORACLE", "GAUSSDB"});
            dbTypeCombo.setFont(ThemeUtils.FONT_NORMAL);
            dbTypeCombo.setBackground(Color.WHITE);
            panel.add(dbTypeCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("规则类型:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            ruleTypeField = createTextField(15, true);
            panel.add(ruleTypeField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("规则名称:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            ruleNameField = createTextField(15, true);
            panel.add(ruleNameField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("执行标志:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            execFlagCombo = new JComboBox<>(new String[]{"Y", "N"});
            execFlagCombo.setFont(ThemeUtils.FONT_NORMAL);
            execFlagCombo.setBackground(Color.WHITE);
            panel.add(execFlagCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("适用数据类型:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            applyDataTypeCombo = new JComboBox<>(new String[]{"STRING", "NUMBER", "DATE", "ALL"});
            applyDataTypeCombo.setFont(ThemeUtils.FONT_NORMAL);
            applyDataTypeCombo.setBackground(Color.WHITE);
            panel.add(applyDataTypeCombo, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("检查条件:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            checkConditionField = createTextField(20, true);
            panel.add(checkConditionField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("清洗表达式:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            cleanExpressionField = createTextField(20, true);
            panel.add(cleanExpressionField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("优先级:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            priorityField = createTextField(5, true);
            panel.add(priorityField, gbc);

            gbc.gridx = 0; gbc.gridy = ++row; gbc.weightx = 0;
            panel.add(createLabel("描述:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            ruleDescField = createTextField(20, true);
            panel.add(ruleDescField, gbc);
        }

        private JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setFont(ThemeUtils.FONT_NORMAL);
            label.setForeground(ThemeUtils.COLOR_TEXT);
            return label;
        }

        private JTextField createTextField(int columns, boolean editable) {
            JTextField field = new JTextField(columns);
            field.setFont(ThemeUtils.FONT_NORMAL);
            field.setEditable(editable);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            return field;
        }

        private JPanel buildButtonPanel() {
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
            btnPanel.setBackground(Color.WHITE);
            btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeUtils.COLOR_BORDER));

            JButton okBtn = new JButton("保存");
            okBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            okBtn.setBackground(ThemeUtils.COLOR_PRIMARY);
            okBtn.setForeground(Color.WHITE);
            okBtn.setFocusPainted(false);
            okBtn.setPreferredSize(new Dimension(100, 36));
            okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            okBtn.addActionListener(e -> save());

            JButton cancelBtn = new JButton("取消");
            cancelBtn.setFont(ThemeUtils.FONT_NORMAL);
            cancelBtn.setBackground(ThemeUtils.COLOR_BG_CARD);
            cancelBtn.setForeground(ThemeUtils.COLOR_TEXT);
            cancelBtn.setFocusPainted(false);
            cancelBtn.setPreferredSize(new Dimension(100, 36));
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.addActionListener(e -> dispose());

            btnPanel.add(okBtn);
            btnPanel.add(cancelBtn);
            return btnPanel;
        }

        private void populateFormData() {
            if (config == null) {
                ruleIdField.setText("自动");
                return;
            }
            ruleIdField.setText(String.valueOf(config.getRuleId()));
            dbTypeCombo.setSelectedItem(config.getDbType());
            ruleTypeField.setText(config.getRuleType());
            ruleNameField.setText(config.getRuleName());
            execFlagCombo.setSelectedItem(config.getExecFlag());
            applyDataTypeCombo.setSelectedItem(config.getApplyDataType());
            checkConditionField.setText(config.getCheckCondition());
            cleanExpressionField.setText(config.getCleanExpression());
            priorityField.setText(config.getPriority() != null ? String.valueOf(config.getPriority()) : "");
            ruleDescField.setText(config.getRuleDesc());
        }

        private void save() {
            try {
                DataCheckConfig c = new DataCheckConfig();
                if (config != null && config.getRuleId() != null) {
                    c.setRuleId(config.getRuleId());
                } else {
                    c.setRuleId(null);
                }

                c.setDbType((String) dbTypeCombo.getSelectedItem());
                c.setRuleType(ruleTypeField.getText().trim());
                c.setRuleName(ruleNameField.getText().trim());
                c.setExecFlag((String) execFlagCombo.getSelectedItem());
                c.setApplyDataType((String) applyDataTypeCombo.getSelectedItem());
                c.setCheckCondition(checkConditionField.getText().trim());
                c.setCleanExpression(cleanExpressionField.getText().trim());

                String priorityText = priorityField.getText().trim();
                c.setPriority(priorityText.isEmpty() ? 99 : Integer.parseInt(priorityText));

                c.setRuleDesc(ruleDescField.getText().trim());

                DataSource ds = getSelectedDataSource();
                if (ds == null) {
                    JOptionPane.showMessageDialog(this, "请先选择数据源", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (config != null && config.getRuleId() != null) {
                    dao.update(c, ds);
                } else {
                    dao.insert(c, ds);
                }

                saved = true;
                dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "优先级请输入数字", "错误", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}