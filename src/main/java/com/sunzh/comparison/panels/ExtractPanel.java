package com.sunzh.comparison.panels;

import com.sunzh.comparison.ComparisonDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.text.SimpleDateFormat;

public class ExtractPanel extends JPanel {
    // 岩系冷调配色 - 提取面板专用色
    private static final Color ACCENT = new Color(76, 110, 138);
    private static final Color ACCENT_DARK = new Color(56, 82, 105);
    private static final Color ACCENT_LIGHT = new Color(180, 200, 220);
    private static final Color BG_PANEL = new Color(245, 248, 252);
    private static final Color BORDER_COLOR = new Color(190, 200, 215);
    private static final Color ROW_ALT = new Color(242, 245, 249);
    private static final Color BTN_EXTRACT = new Color(90, 150, 120);
    private static final Color BTN_VIEW = ACCENT;
    private static final Color BTN_CLEAR = new Color(190, 100, 90);
    private static final Color BTN_CLEAN = new Color(180, 80, 70);

    private final ComparisonDialog parent;
    private final boolean isSource;
    private JTextArea logArea;
    private JCheckBox cbAll, cbTable, cbColumn, cbIndex, cbSequence, cbSynonym;
    private JCheckBox cbAllSchema;
    private java.util.Map<String, JCheckBox> schemaCheckBoxes = new java.util.LinkedHashMap<>();
    private JPanel schemaCheckboxPanel;
    private DefaultTableModel logTableModel;
    private JTable logTable;

    public ExtractPanel(ComparisonDialog parent, boolean isSource) {
        this.parent = parent;
        this.isSource = isSource;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initUI();
        // 不在打开对话框时自动连接数据库，由用户选择数据源/点击刷新后连接
    }

    private void initUI() {
        // ---- 上半部分：模式选择 + 类型选择 ----
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 15, 0));

        // ============ 左侧：模式选择 ============
        JPanel schemaPanel = new JPanel(new BorderLayout());
        schemaPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 2),
                "选择模式（可多选）",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13),
                ACCENT));

        JPanel schemaTopBar = new JPanel(new BorderLayout());
        cbAllSchema = new JCheckBox("ALL（所有模式）");
        cbAllSchema.setFont(new Font("SansSerif", Font.BOLD, 14));
        schemaTopBar.add(cbAllSchema, BorderLayout.WEST);

        JPanel rightBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightBtnPanel.setOpaque(false);

        JButton btnRefreshSchema = new JButton("刷新");
        btnRefreshSchema.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRefreshSchema.setFocusPainted(false);
        btnRefreshSchema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshSchema.addActionListener(e -> refreshData());

        JButton btnAddSchema = new JButton("添加");
        btnAddSchema.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnAddSchema.setFocusPainted(false);
        btnAddSchema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddSchema.addActionListener(e -> addManualSchema());

        rightBtnPanel.add(btnRefreshSchema);
        rightBtnPanel.add(btnAddSchema);
        schemaTopBar.add(rightBtnPanel, BorderLayout.EAST);

        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(200, 200, 200));

        JPanel schemaHeader = new JPanel(new BorderLayout());
        schemaHeader.add(schemaTopBar, BorderLayout.NORTH);
        schemaHeader.add(sep1, BorderLayout.CENTER);
        schemaPanel.add(schemaHeader, BorderLayout.NORTH);

        schemaCheckboxPanel = new JPanel();
        schemaCheckboxPanel.setLayout(new BoxLayout(schemaCheckboxPanel, BoxLayout.Y_AXIS));
        JScrollPane schemaScroll = new JScrollPane(schemaCheckboxPanel);
        schemaScroll.setPreferredSize(new Dimension(250, 180));
        schemaScroll.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        schemaPanel.add(schemaScroll, BorderLayout.CENTER);
        topPanel.add(schemaPanel);

        // ============ 右侧：抽取类型 ============
        JPanel typePanel = new JPanel(new BorderLayout());
        typePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 2),
                "抽取类型",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13),
                ACCENT));

        JPanel checkboxPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 15, 6, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        cbAll = new JCheckBox("ALL（全类型抽取）");
        cbAll.setFont(new Font("SansSerif", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        checkboxPanel.add(cbAll, gbc);

        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        gbc.insets = new Insets(8, 15, 8, 15);
        checkboxPanel.add(sep2, gbc);

        gbc.insets = new Insets(5, 35, 5, 15);
        cbTable = new JCheckBox("TABLE（表）");
        cbColumn = new JCheckBox("COLUMN（列信息）");
        cbIndex = new JCheckBox("INDEX（索引）");
        cbSequence = new JCheckBox("SEQUENCE（序列）");
        cbSynonym = new JCheckBox("SYNONYM（同义词）");

        Font subFont = new Font("SansSerif", Font.PLAIN, 13);
        cbTable.setFont(subFont);
        cbColumn.setFont(subFont);
        cbIndex.setFont(subFont);
        cbSequence.setFont(subFont);
        cbSynonym.setFont(subFont);

        gbc.gridy = 2;
        checkboxPanel.add(cbTable, gbc);
        gbc.gridy = 3;
        checkboxPanel.add(cbColumn, gbc);
        gbc.gridy = 4;
        checkboxPanel.add(cbIndex, gbc);
        gbc.gridy = 5;
        checkboxPanel.add(cbSequence, gbc);
        gbc.gridy = 6;
        checkboxPanel.add(cbSynonym, gbc);

        cbAll.setSelected(true);
        setSubTypesSelected(true);
        setSubTypesEnabled(false);

        typePanel.add(checkboxPanel, BorderLayout.CENTER);
        topPanel.add(typePanel);
        add(topPanel, BorderLayout.NORTH);

        // ============ 中间：按钮 + 日志表格 ============
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 15, 10));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 25, 12, 25)));

        JButton btnExtract = createStyledButton(isSource ? "源端数据抽取" : "目标端数据抽取", BTN_EXTRACT);
        btnExtract.addActionListener(e -> doExtract());

        JButton btnRefreshLog = createStyledButton("日志查看", BTN_VIEW);
        btnRefreshLog.addActionListener(e -> refreshLogData());

        JButton btnClearLog = createStyledButton("清空执行日志", BTN_CLEAR);
        btnClearLog.addActionListener(e -> logArea.setText(""));

        JButton btnCleanLogTable = createStyledButton("清理日志表", BTN_CLEAN);
        btnCleanLogTable.addActionListener(e -> cleanLogTable());

        btnPanel.add(btnExtract);
        btnPanel.add(btnRefreshLog);
        btnPanel.add(btnClearLog);
        btnPanel.add(btnCleanLogTable);
        centerPanel.add(btnPanel, BorderLayout.NORTH);

        logTableModel = new DefaultTableModel(
                new String[]{"时间", "模式", "类型", "状态", "表", "列", "索引", "序列", "同义词", "耗时(秒)", "错误信息"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(logTableModel);
        beautifyLogTable(logTable);

        JScrollPane logTableScroll = new JScrollPane(logTable);
        logTableScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                "抽取日志 - gk_sjdb_extract_log",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), ACCENT));
        logTableScroll.setPreferredSize(new Dimension(800, 200));
        centerPanel.add(logTableScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        logArea = new JTextArea(5, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(248, 250, 253));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("执行日志"));
        add(logScroll, BorderLayout.SOUTH);

        // ============ 事件绑定 ============
        cbAllSchema.addActionListener(e -> {
            boolean sel = cbAllSchema.isSelected();
            for (JCheckBox cb : schemaCheckBoxes.values()) cb.setSelected(sel);
        });

        cbAll.addActionListener(e -> {
            if (cbAll.isSelected()) {
                setSubTypesSelected(true);
                setSubTypesEnabled(false);
            } else {
                setSubTypesSelected(false);
                setSubTypesEnabled(true);
            }
        });

        ActionListener subTypeListener = e -> {
            if (isAllSubTypesSelected()) {
                cbAll.setSelected(true);
                setSubTypesSelected(true);
                setSubTypesEnabled(false);
            } else {
                if (cbAll.isSelected()) {
                    cbAll.setSelected(false);
                    setSubTypesEnabled(true);
                }
            }
        };
        cbTable.addActionListener(subTypeListener);
        cbColumn.addActionListener(subTypeListener);
        cbIndex.addActionListener(subTypeListener);
        cbSequence.addActionListener(subTypeListener);
        cbSynonym.addActionListener(subTypeListener);
    }

    // ---------- 辅助方法 ----------
    private void setSubTypesSelected(boolean selected) {
        cbTable.setSelected(selected);
        cbColumn.setSelected(selected);
        cbIndex.setSelected(selected);
        cbSequence.setSelected(selected);
        cbSynonym.setSelected(selected);
    }

    private void setSubTypesEnabled(boolean enabled) {
        cbTable.setEnabled(enabled);
        cbColumn.setEnabled(enabled);
        cbIndex.setEnabled(enabled);
        cbSequence.setEnabled(enabled);
        cbSynonym.setEnabled(enabled);
        Color fg = enabled ? Color.BLACK : Color.GRAY;
        cbTable.setForeground(fg);
        cbColumn.setForeground(fg);
        cbIndex.setForeground(fg);
        cbSequence.setForeground(fg);
        cbSynonym.setForeground(fg);
    }

    private boolean isAllSubTypesSelected() {
        return cbTable.isSelected() && cbColumn.isSelected() && cbIndex.isSelected()
                && cbSequence.isSelected() && cbSynonym.isSelected();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
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

    private void beautifyLogTable(JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(logTableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setGridColor(new Color(205, 210, 218));
        table.setSelectionBackground(ACCENT_LIGHT);
        table.setSelectionForeground(Color.BLACK);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ROW_ALT : Color.WHITE);
                }
                if (column == 3 && value != null) {
                    String status = value.toString();
                    String displayText = status;
                    Color color = Color.BLACK;
                    if ("SUCCESS".equals(status)) {
                        displayText = "[成功] " + status;
                        color = new Color(90, 150, 120);
                    } else if ("FAILED".equals(status)) {
                        displayText = "[失败] " + status;
                        color = new Color(190, 90, 85);
                    } else if ("RUNNING".equals(status)) {
                        displayText = "[进行中] " + status;
                        color = new Color(195, 140, 85);
                    }
                    setText(displayText);
                    setForeground(color);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    if (column == 10 && value != null && !value.toString().isEmpty()) {
                        setForeground(Color.RED);
                    } else {
                        setForeground(Color.BLACK);
                    }
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (column != 10) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setForeground(Color.WHITE);
        header.setBackground(ACCENT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_DARK, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
    }

    private void addManualSchema() {
        String schema = JOptionPane.showInputDialog(this, "请输入模式名称：", "手工添加模式", JOptionPane.PLAIN_MESSAGE);
        if (schema == null || schema.trim().isEmpty()) return;
        schema = schema.trim();
        for (String key : schemaCheckBoxes.keySet()) {
            if (key.equalsIgnoreCase(schema)) {
                JOptionPane.showMessageDialog(this, "模式 '" + schema + "' 已存在！", "提示", JOptionPane.INFORMATION_MESSAGE);
                schemaCheckBoxes.get(key).setSelected(true);
                updateAllSchemaState();
                return;
            }
        }
        JCheckBox cb = new JCheckBox(schema);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.addActionListener(e -> updateAllSchemaState());
        schemaCheckBoxes.put(schema, cb);
        schemaCheckboxPanel.add(cb);
        cb.setSelected(true);
        updateAllSchemaState();
        schemaCheckboxPanel.revalidate();
        schemaCheckboxPanel.repaint();
    }

    private void updateAllSchemaState() {
        boolean allSelected = true;
        for (JCheckBox cb : schemaCheckBoxes.values()) {
            if (!cb.isSelected()) { allSelected = false; break; }
        }
        cbAllSchema.setSelected(allSelected);
    }

    private java.util.List<String> getSelectedSchemas() {
        java.util.List<String> selected = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, JCheckBox> entry : schemaCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) selected.add(entry.getKey());
        }
        return selected;
    }

    /** 向日志区追加一行 */
    private void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ===== 刷新数据 =====
    public void refreshData() {
        schemaCheckBoxes.clear();
        schemaCheckboxPanel.removeAll();
        try (Connection conn = parent.getConnection()) {
            String col = isSource ? "compare_schemas_source" : "compare_schemas_target";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT DISTINCT " + col + " FROM gk_sjdb_task_config WHERE " + col + " IS NOT NULL ORDER BY 1");
            while (rs.next()) {
                String schemaName = rs.getString(1);
                if (schemaName.isEmpty()) continue;
                JCheckBox cb = new JCheckBox(schemaName);
                cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
                cb.addActionListener(e -> updateAllSchemaState());
                schemaCheckBoxes.put(schemaName, cb);
                schemaCheckboxPanel.add(cb);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            appendLog("[警告] 刷新模式列表失败：" + e.getMessage());
        }
        updateAllSchemaState();
        schemaCheckboxPanel.revalidate();
        schemaCheckboxPanel.repaint();
        refreshLogData();
    }

    private void refreshLogData() {
        logTableModel.setRowCount(0);
        String targetType = isSource ? "SOURCE" : "TARGET";
        try (Connection conn = parent.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT start_time, schema_name, extract_type, status, " +
                            "total_tables, total_columns, total_indexes, total_sequences, total_synonyms, " +
                            "duration_seconds, error_msg " +
                            "FROM gk_sjdb_extract_log WHERE target_type = ? ORDER BY start_time DESC");
            ps.setString(1, targetType);
            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                String time = ts != null ? sdf.format(ts) : "";
                logTableModel.addRow(new Object[]{
                        time, rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getObject(5), rs.getObject(6), rs.getObject(7), rs.getObject(8),
                        rs.getObject(9), rs.getObject(10), rs.getString(11)
                });
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            appendLog("[警告] 刷新抽取日志失败：" + e.getMessage());
        }
    }

    // ===== 执行抽取 =====
    private void doExtract() {
        java.util.List<String> schemas = getSelectedSchemas();
        if (schemas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择至少一个模式", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.List<String> typeList = new java.util.ArrayList<>();
        if (cbAll.isSelected()) {
            typeList.add("ALL");
        } else {
            if (cbTable.isSelected()) typeList.add("TABLE");
            if (cbColumn.isSelected()) typeList.add("COLUMN");
            if (cbIndex.isSelected()) typeList.add("INDEX");
            if (cbSequence.isSelected()) typeList.add("SEQUENCE");
            if (cbSynonym.isSelected()) typeList.add("SYNONYM");
            if (typeList.isEmpty()) typeList.add("ALL");
        }

        final java.util.List<String> finalTypeList = typeList;
        final boolean isSourceLocal = isSource;

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try (Connection conn = parent.getConnection()) {
                    for (String schema : schemas) {
                        for (String type : finalTypeList) {
                            String time = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                            publish("[" + time + "] 开始处理模式: " + schema + " 类型: " + type);
                            String procName = isSourceLocal ? "sp_extract_source_data" : "sp_extract_target_data";
                            CallableStatement cstmt = conn.prepareCall("{call " + procName + "(?,?)}");
                            cstmt.setString(1, schema);
                            cstmt.setString(2, type);
                            cstmt.execute();
                            cstmt.close();
                            time = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                            publish("[" + time + "] 完成模式: " + schema + " 类型: " + type);
                        }
                    }
                    publish("================== 全部完成 ==================");
                } catch (Exception ex) {
                    publish("[ERROR] " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    logArea.append(msg + "\n");
                }
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                refreshLogData();
            }
        }.execute();
    }

    // ===== 清理日志表 =====
    private void cleanLogTable() {
        String targetType = isSource ? "SOURCE" : "TARGET";
        int totalCount = 0;
        try (Connection conn = parent.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM gk_sjdb_extract_log WHERE target_type = ?");
            ps.setString(1, targetType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) totalCount = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (Exception ex) { /* 忽略 */ }

        if (totalCount == 0) {
            JOptionPane.showMessageDialog(this, "日志表为空，无需清理", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final int finalTotalCount = totalCount;
        final String finalTargetType = targetType;

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel lblInfo = new JLabel("当前日志总数: " + totalCount + " 条 (仅 " + targetType + " 端)");
        lblInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblInfo.setForeground(ACCENT);
        panel.add(lblInfo);

        JComboBox<String> cmbMode = new JComboBox<>(new String[]{
                "清空全部日志（当前端）",
                "按模式名删除",
                "按状态删除",
                "删除 N 天前的日志"
        });
        panel.add(new JLabel("删除方式:"));
        panel.add(cmbMode);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel lblInput = new JLabel("请输入:");
        JTextField tfInput = new JTextField(15);
        final JLabel lblHint = new JLabel("");

        inputPanel.add(lblInput, BorderLayout.WEST);
        inputPanel.add(tfInput, BorderLayout.CENTER);

        JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"SUCCESS", "FAILED", "PARTIAL", "RUNNING"});
        cmbStatus.setVisible(false);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.add(new JLabel("状态:"));
        statusPanel.add(cmbStatus);
        statusPanel.setVisible(false);

        inputPanel.add(statusPanel, BorderLayout.EAST);
        panel.add(inputPanel);
        panel.add(lblHint);

        cmbMode.addActionListener(e -> {
            int idx = cmbMode.getSelectedIndex();
            switch (idx) {
                case 0:
                    lblInput.setVisible(false);
                    tfInput.setVisible(false);
                    statusPanel.setVisible(false);
                    lblHint.setText("将删除全部 " + finalTotalCount + " 条日志记录（仅 " + finalTargetType + " 端）");
                    break;
                case 1:
                    lblInput.setVisible(true);
                    lblInput.setText("模式名:");
                    tfInput.setVisible(true);
                    tfInput.setText("");
                    statusPanel.setVisible(false);
                    lblHint.setText("删除指定模式的所有日志记录");
                    break;
                case 2:
                    lblInput.setVisible(false);
                    tfInput.setVisible(false);
                    statusPanel.setVisible(true);
                    lblHint.setText("删除指定状态的日志记录");
                    break;
                case 3:
                    lblInput.setVisible(true);
                    lblInput.setText("天数:");
                    tfInput.setVisible(true);
                    tfInput.setText("7");
                    statusPanel.setVisible(false);
                    lblHint.setText("删除 N 天前的日志记录");
                    break;
            }
            panel.revalidate();
            panel.repaint();
        });
        cmbMode.setSelectedIndex(0);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "清理日志表",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        int idx = cmbMode.getSelectedIndex();
        String sql = "";
        String desc = "";

        try {
            switch (idx) {
                case 0:
                    sql = "DELETE FROM gk_sjdb_extract_log WHERE target_type = ?";
                    desc = "全部日志（" + targetType + "端）";
                    break;
                case 1:
                    String schema = tfInput.getText().trim();
                    if (schema.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "请输入模式名", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    sql = "DELETE FROM gk_sjdb_extract_log WHERE target_type = ? AND SCHEMA_NAME = ?";
                    desc = "模式名=" + schema + "（" + targetType + "端）";
                    break;
                case 2:
                    String status = (String) cmbStatus.getSelectedItem();
                    sql = "DELETE FROM gk_sjdb_extract_log WHERE target_type = ? AND STATUS = ?";
                    desc = "状态=" + status + "（" + targetType + "端）";
                    break;
                case 3:
                    String dayStr = tfInput.getText().trim();
                    if (dayStr.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "请输入天数", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    try {
                        int days = Integer.parseInt(dayStr);
                        if (days < 0) {
                            JOptionPane.showMessageDialog(this, "天数不能为负数", "提示", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "请输入有效的数字", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    sql = "DELETE FROM gk_sjdb_extract_log WHERE target_type = ? AND START_TIME < SYSTIMESTAMP - INTERVAL ? DAY";
                    desc = dayStr + "天前（" + targetType + "端）";
                    break;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要删除日志记录吗？\n删除条件: " + desc,
                    "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            try (Connection conn = parent.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, targetType);
                switch (idx) {
                    case 0: break;
                    case 1: ps.setString(2, tfInput.getText().trim()); break;
                    case 2: ps.setString(2, (String) cmbStatus.getSelectedItem()); break;
                    case 3: ps.setInt(2, Integer.parseInt(tfInput.getText().trim())); break;
                }
                int count = ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(this, "删除成功，共删除 " + count + " 条记录", "完成",
                        JOptionPane.INFORMATION_MESSAGE);
                refreshLogData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "删除失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}