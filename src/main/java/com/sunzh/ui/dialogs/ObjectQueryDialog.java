package com.sunzh.ui.dialogs;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * 数据库对象查询对话框
 */
public class ObjectQueryDialog extends BaseDialog {

    private JComboBox<String> dataSourceCombo;
    private JTextField objectNameField;
    private JButton queryButton;
    private JButton closeButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private List<DataSource> dataSources;

    public ObjectQueryDialog(JFrame owner) {
        super(owner, "🔍 数据库对象查询");
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(ThemeUtils.COLOR_BG);
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        // ---- 顶部：查询条件 ----
        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBackground(ThemeUtils.COLOR_BG_CARD);
        queryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "查询条件",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 数据源
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel dsLabel = new JLabel("数据源:");
        dsLabel.setFont(ThemeUtils.FONT_NORMAL);
        dsLabel.setForeground(ThemeUtils.COLOR_TEXT);
        queryPanel.add(dsLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setFont(ThemeUtils.FONT_NORMAL);
        dataSourceCombo.setPreferredSize(new Dimension(200, 30));
        queryPanel.add(dataSourceCombo, gbc);

        // 对象名称
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel("对象名称:");
        nameLabel.setFont(ThemeUtils.FONT_NORMAL);
        nameLabel.setForeground(ThemeUtils.COLOR_TEXT);
        queryPanel.add(nameLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.45;
        objectNameField = new JTextField();
        objectNameField.setFont(ThemeUtils.FONT_NORMAL);
        objectNameField.setPreferredSize(new Dimension(280, 30));
        objectNameField.addActionListener(e -> doQuery());
        queryPanel.add(objectNameField, gbc);

        // 查询按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        queryButton = new JButton("🔍 查询");
        queryButton.setFont(ThemeUtils.FONT_BOLD);
        queryButton.setBackground(ThemeUtils.COLOR_PRIMARY);
        queryButton.setForeground(Color.WHITE);
        queryButton.setFocusPainted(false);
        queryButton.setBorderPainted(false);
        queryButton.setPreferredSize(new Dimension(100, 32));
        queryButton.addActionListener(e -> doQuery());
        queryPanel.add(queryButton, gbc);

        add(queryPanel, BorderLayout.NORTH);

        // ---- 中间：结果表格（占满剩余空间） ----
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(ThemeUtils.COLOR_BG_CARD);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "查询结果",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.setRowHeight(26);
        resultTable.setFont(ThemeUtils.FONT_NORMAL);
        resultTable.getTableHeader().setFont(ThemeUtils.FONT_SMALL_BOLD);
        resultTable.getTableHeader().setBackground(ThemeUtils.COLOR_PRIMARY);
        resultTable.getTableHeader().setForeground(Color.WHITE);
        resultTable.setGridColor(ThemeUtils.COLOR_BORDER_LIGHT);
        resultTable.setSelectionBackground(ThemeUtils.COLOR_PRIMARY);
        resultTable.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // ★ 移除固定尺寸，让表格自动填满

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        // ---- 底部：状态 + 关闭按钮 ----
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(8, 4, 4, 4));

        statusLabel = new JLabel("就绪，请输入对象名称");
        statusLabel.setFont(ThemeUtils.FONT_SMALL);
        statusLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(120, 18));
        progressBar.setStringPainted(true);
        progressBar.setFont(ThemeUtils.FONT_SMALL);
        progressBar.setVisible(false);
        rightPanel.add(progressBar);

        closeButton = new JButton("关闭");
        closeButton.setFont(ThemeUtils.FONT_NORMAL);
        closeButton.setBackground(ThemeUtils.COLOR_SECONDARY);
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setPreferredSize(new Dimension(80, 30));
        closeButton.addActionListener(e -> dispose());
        rightPanel.add(closeButton);

        bottomPanel.add(rightPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 加载数据源
        loadDataSources();

        // ★ 设置窗口大小，默认较大，且可调整
        setSize(1200, 750);
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private void loadDataSources() {
        dataSources = DataSourceStore.load();
        dataSourceCombo.removeAllItems();
        if (dataSources.isEmpty()) {
            dataSourceCombo.addItem("请先在数据源配置中添加数据源");
            dataSourceCombo.setEnabled(false);
        } else {
            for (DataSource ds : dataSources) {
                dataSourceCombo.addItem(ds.getName());
            }
            dataSourceCombo.setSelectedIndex(0);
            dataSourceCombo.setEnabled(true);
        }
    }

    private void doQuery() {
        String selectedName = (String) dataSourceCombo.getSelectedItem();
        if (selectedName == null || dataSources.isEmpty()) {
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

        String objectName = objectNameField.getText().trim();
        if (objectName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入要查询的对象名称！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setUIEnabled(false);
        queryButton.setText("查询中...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("正在查询...");

        final DataSource finalDs = ds;
        final String finalObjectName = objectName;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                Connection conn = null;
                try {
                    // ★★★ 注册 JDBC 驱动 ★★★
                    if ("ORACLE".equalsIgnoreCase(finalDs.getType())) {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } else if ("GAUSSDB".equalsIgnoreCase(finalDs.getType())) {
                        try {
                            Class.forName("com.huawei.gaussdb.jdbc.Driver");
                        } catch (ClassNotFoundException e1) {
                            try {
                                Class.forName("com.huawei.gauss.jdbc.Driver");
                            } catch (ClassNotFoundException e2) {
                                Class.forName("org.postgresql.Driver");
                            }
                        }
                    } else {
                        throw new SQLException("不支持的数据源类型: " + finalDs.getType());
                    }

                    conn = DriverManager.getConnection(
                            finalDs.buildUrl(), finalDs.getUser(), finalDs.getPassword());

                    String sql = "SELECT * FROM v_szh_db_objects WHERE object_name = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, finalObjectName);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            ResultSetMetaData meta = rs.getMetaData();
                            int columnCount = meta.getColumnCount();

                            String[] columnNames = new String[columnCount];
                            for (int i = 0; i < columnCount; i++) {
                                columnNames[i] = meta.getColumnName(i + 1);
                            }

                            java.util.List<Object[]> rows = new java.util.ArrayList<>();
                            while (rs.next()) {
                                Object[] row = new Object[columnCount];
                                for (int i = 0; i < columnCount; i++) {
                                    row[i] = rs.getObject(i + 1);
                                }
                                rows.add(row);
                            }

                            SwingUtilities.invokeLater(() -> {
                                tableModel.setDataVector(rows.toArray(new Object[0][]), columnNames);
                                autoResizeColumns(resultTable);
                                statusLabel.setText("查询完成，共 " + rows.size() + " 行");
                            });
                        }
                    }
                } catch (ClassNotFoundException e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("驱动加载失败: " + e.getMessage());
                        JOptionPane.showMessageDialog(ObjectQueryDialog.this,
                                "数据库驱动加载失败，请检查依赖:\n" + e.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    });
                    e.printStackTrace();
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("查询失败: " + e.getMessage());
                        JOptionPane.showMessageDialog(ObjectQueryDialog.this,
                                "查询失败: " + e.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    });
                    e.printStackTrace();
                } finally {
                    if (conn != null) {
                        try { conn.close(); } catch (SQLException ignored) {}
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setUIEnabled(true);
                queryButton.setText("🔍 查询");
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
            }
        }.execute();
    }

    private void setUIEnabled(boolean enabled) {
        dataSourceCombo.setEnabled(enabled);
        objectNameField.setEnabled(enabled);
        queryButton.setEnabled(enabled);
    }

    /**
     * 自动调整表格列宽，使其填满可见区域，但列宽不超过一定比例
     */
    private void autoResizeColumns(JTable table) {
        if (table.getColumnCount() == 0) return;
        
        // 先获取表格可见宽度
        int tableWidth = table.getVisibleRect().width;
        if (tableWidth <= 0) {
            // 如果表格尚未显示，使用父容器宽度
            Container parent = table.getParent();
            while (parent != null && !(parent instanceof JViewport)) {
                parent = parent.getParent();
            }
            if (parent instanceof JViewport) {
                tableWidth = parent.getWidth();
            } else {
                tableWidth = 1000; // 默认
            }
        }

        TableColumnModel colModel = table.getColumnModel();
        int colCount = colModel.getColumnCount();
        
        // 计算各列所需宽度（根据表头和数据）
        Font headerFont = table.getTableHeader().getFont();
        Font dataFont = table.getFont();
        int[] colWidths = new int[colCount];
        int totalPreferred = 0;

        for (int col = 0; col < colCount; col++) {
            int maxWidth = 0;
            TableColumn column = colModel.getColumn(col);

            Object headerValue = column.getHeaderValue();
            if (headerValue != null) {
                FontMetrics fm = table.getFontMetrics(headerFont);
                int width = fm.stringWidth(headerValue.toString()) + 30;
                maxWidth = Math.max(maxWidth, width);
            }

            int rowCount = Math.min(table.getRowCount(), 200);
            for (int row = 0; row < rowCount; row++) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    FontMetrics fm = table.getFontMetrics(dataFont);
                    int width = fm.stringWidth(value.toString()) + 30;
                    maxWidth = Math.max(maxWidth, width);
                }
            }

            // 限制最大宽度，防止列过宽
            maxWidth = Math.min(maxWidth, 400);
            colWidths[col] = maxWidth;
            totalPreferred += maxWidth;
        }

        // 如果总宽度小于表格宽度，则按比例扩展列宽
        if (totalPreferred < tableWidth) {
            int extra = tableWidth - totalPreferred;
            int base = extra / colCount;
            for (int i = 0; i < colCount; i++) {
                colWidths[i] += base;
            }
            // 把多余的像素给第一列（或最后一列）
            int remain = extra % colCount;
            if (remain > 0) {
                colWidths[0] += remain;
            }
        }

        // 应用列宽
        for (int col = 0; col < colCount; col++) {
            TableColumn column = colModel.getColumn(col);
            int width = Math.max(colWidths[col], 40);
            column.setPreferredWidth(width);
            column.setMinWidth(width);
            column.setMaxWidth(width);
        }

        table.getTableHeader().resizeAndRepaint();
        table.revalidate();
        table.repaint();
    }

    @Override
    public void refresh() {
        loadDataSources();
    }
}