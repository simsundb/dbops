package com.example.comparison.panels;

import com.example.comparison.ComparisonDialog;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;

public class DetailPanel extends JPanel {
    private final ComparisonDialog parent;
    private JComboBox<String> typeCombo;
    private JTextField tfJobId;
    private DefaultTableModel detailModel;
    private JTable detailTable;
    private JButton btnQuery;
    private JButton btnExportCurrent;
    private JButton btnExportAll;

    // 使用 ThemeUtils 岩系冷调配色
    private static final java.awt.Color THEME_ACCENT = new java.awt.Color(76, 110, 138);
    private static final java.awt.Color THEME_ACCENT_DARK = new java.awt.Color(56, 82, 105);
    private static final java.awt.Color BG_ALTERNATE = new java.awt.Color(242, 245, 249);
    private static final java.awt.Color BG_PANEL = new java.awt.Color(245, 248, 252);

    public DetailPanel(ComparisonDialog parent) {
        this.parent = parent;
        setLayout(new java.awt.BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initUI();
        SwingUtilities.invokeLater(this::refreshData);
    }

    private void initUI() {
        // ---- 查询条件区域 ----
        JPanel topPanel = new JPanel(new java.awt.GridBagLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(190, 200, 215), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        topPanel.setBackground(BG_PANEL);

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 8, 5, 8);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        java.awt.Font labelFont = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12);

        // 对象类型
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel lblType = new JLabel("对象类型:");
        lblType.setFont(labelFont);
        topPanel.add(lblType, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        typeCombo = new JComboBox<>(new String[]{"表", "列", "索引", "序列", "同义词"});
        typeCombo.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        typeCombo.setBackground(java.awt.Color.WHITE);
        topPanel.add(typeCombo, gbc);

        // JOB_ID
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel lblJobId = new JLabel("JOB_ID (可选):");
        lblJobId.setFont(labelFont);
        topPanel.add(lblJobId, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        tfJobId = new JTextField(15);
        tfJobId.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        tfJobId.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(190, 200, 215), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        topPanel.add(tfJobId, gbc);

        // 查询按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        btnQuery = createStyledButton("查询", THEME_ACCENT);
        btnQuery.addActionListener(e -> doQuery());
        topPanel.add(btnQuery, gbc);

        // 导出当前类型
        gbc.gridx = 5;
        gbc.weightx = 0;
        btnExportCurrent = createStyledButton("导出当前类型", THEME_ACCENT);
        btnExportCurrent.addActionListener(e -> exportCurrentType());
        topPanel.add(btnExportCurrent, gbc);

        // 导出全部类型
        gbc.gridx = 6;
        gbc.weightx = 0;
        btnExportAll = createStyledButton("导出全部类型", THEME_ACCENT);
        btnExportAll.addActionListener(e -> exportAllTypes());
        topPanel.add(btnExportAll, gbc);

        add(topPanel, java.awt.BorderLayout.NORTH);

        // ---- 明细表格 ----
        detailModel = new DefaultTableModel();
        detailTable = new JTable(detailModel);
        beautifyDetailTable(detailTable);

        JScrollPane scrollPane = new JScrollPane(detailTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(THEME_ACCENT, 1),
                "对比明细结果",
                TitledBorder.LEFT, TitledBorder.TOP,
                new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12), THEME_ACCENT));
        scrollPane.setPreferredSize(new java.awt.Dimension(800, 300));

        add(scrollPane, java.awt.BorderLayout.CENTER);
    }

    // ----- 创建统一样式按钮 -----
    private JButton createStyledButton(String text, java.awt.Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(java.awt.Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
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

    // ----- 美化明细表格 -----
    private void beautifyDetailTable(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        table.setGridColor(new java.awt.Color(205, 210, 218));
        table.setSelectionBackground(new java.awt.Color(180, 200, 220));
        table.setSelectionForeground(java.awt.Color.BLACK);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_ALTERNATE : java.awt.Color.WHITE);
                }
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        header.setForeground(java.awt.Color.WHITE);
        header.setBackground(THEME_ACCENT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(THEME_ACCENT_DARK, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        table.setRowHeight(24);
        table.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        table.setGridColor(new java.awt.Color(205, 210, 218));
        table.setSelectionBackground(new java.awt.Color(180, 200, 220));
        table.setSelectionForeground(java.awt.Color.BLACK);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_ALTERNATE : java.awt.Color.WHITE);
                }
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        JTableHeader header2 = table.getTableHeader();
        header2.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        header2.setForeground(java.awt.Color.WHITE);
        header2.setBackground(THEME_ACCENT);
        header2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(THEME_ACCENT_DARK, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        ((DefaultTableCellRenderer) header2.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
    }

    // ----- 刷新数据 -----
    public void refreshData() {
        // 无自动刷新，用户需手动点击查询按钮
    }

    // ----- 自动调整列宽 -----
    private void autoResizeColumns(JTable table) {
        if (table.getColumnCount() == 0) return;
        TableColumnModel colModel = table.getColumnModel();
        java.awt.Font headerFont = table.getTableHeader().getFont();
        java.awt.Font dataFont = table.getFont();

        for (int col = 0; col < table.getColumnCount(); col++) {
            int maxWidth = 0;
            TableColumn column = colModel.getColumn(col);

            Object headerValue = column.getHeaderValue();
            if (headerValue != null) {
                java.awt.FontMetrics fm = table.getFontMetrics(headerFont);
                int width = fm.stringWidth(headerValue.toString()) + 20;
                maxWidth = Math.max(maxWidth, width);
            }

            int rowCount = Math.min(table.getRowCount(), 1000);
            for (int row = 0; row < rowCount; row++) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    java.awt.FontMetrics fm = table.getFontMetrics(dataFont);
                    int width = fm.stringWidth(value.toString()) + 20;
                    maxWidth = Math.max(maxWidth, width);
                }
            }

            int preferred = Math.min(Math.max(maxWidth, 50), 500);
            column.setPreferredWidth(preferred);
            column.setMinWidth(preferred);
            column.setMaxWidth(preferred);
        }

        table.getTableHeader().resizeAndRepaint();
        table.revalidate();
        table.repaint();
    }

    // ----- 查询 -----
    private void doQuery() {
        String type = (String) typeCombo.getSelectedItem();
        String jobId = tfJobId.getText().trim();

        String tableName = getTableName(type);
        if (tableName == null) return;

        try (Connection conn = parent.getConnection()) {
            String sql = "SELECT * FROM " + tableName;
            if (!jobId.isEmpty()) sql += " WHERE job_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            if (!jobId.isEmpty()) ps.setString(1, jobId);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            String[] colNames = new String[cols];
            for (int i = 0; i < cols; i++) {
                colNames[i] = meta.getColumnName(i + 1);
            }
            detailModel.setDataVector(new Object[][]{}, colNames);

            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                detailModel.addRow(row);
            }
            rs.close();
            ps.close();

            SwingUtilities.invokeLater(() -> autoResizeColumns(detailTable));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "查询失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getTableName(String type) {
        switch (type) {
            case "表": return "gk_sjdb_table_jg";
            case "列": return "gk_sjdb_column_jg";
            case "索引": return "gk_sjdb_index_jg";
            case "序列": return "gk_sjdb_sequence_jg";
            case "同义词": return "gk_sjdb_synonym_jg";
            default: return null;
        }
    }

    // ----- 检查是否有数据 -----
    private boolean hasData(String tableName, String jobIdFilter) {
        try (Connection conn = parent.getConnection()) {
            String sql = "SELECT COUNT(*) FROM " + tableName;
            if (!jobIdFilter.isEmpty()) sql += " WHERE job_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!jobIdFilter.isEmpty()) ps.setString(1, jobIdFilter);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            ps.close();
        } catch (Exception ex) {
            // 表可能不存在
        }
        return false;
    }

    // ----- 导出当前类型 -----
    private void exportCurrentType() {
        String currentType = (String) typeCombo.getSelectedItem();
        String tableName = getTableName(currentType);
        if (tableName == null) {
            JOptionPane.showMessageDialog(this, "无效的对象类型", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String jobId = tfJobId.getText().trim();
        String fileName = currentType + "_明细_" + System.currentTimeMillis();

        if (!hasData(tableName, jobId)) {
            JOptionPane.showMessageDialog(this, "当前类型 [" + currentType + "] 没有数据可导出", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        doExport(fileName, currentType, new String[]{currentType}, jobId);
    }

    // ----- 导出全部类型 -----
    private void exportAllTypes() {
        String jobId = tfJobId.getText().trim();
        String fileName = "全部类型_明细_" + System.currentTimeMillis();
        String[] types = {"表", "列", "索引", "序列", "同义词"};

        boolean hasAnyData = false;
        for (String type : types) {
            String tableName = getTableName(type);
            if (tableName != null && hasData(tableName, jobId)) {
                hasAnyData = true;
                break;
            }
        }

        if (!hasAnyData) {
            JOptionPane.showMessageDialog(this, "没有任何类型有数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        doExport(fileName, "全部类型", types, jobId);
    }

    // ----- 核心导出 -----
    private void doExport(String fileName, String sheetPrefix, String[] types, String jobIdFilter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存 Excel 文件");
        fileChooser.setSelectedFile(new File(fileName + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 文件 (*.xlsx)", "xlsx"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xlsx")) {
            filePath += ".xlsx";
        }
        final String finalFilePath = filePath;

        btnExportCurrent.setEnabled(false);
        btnExportAll.setEnabled(false);
        btnExportCurrent.setText("导出中...");
        btnExportAll.setText("导出中...");

        final String finalJobId = jobIdFilter;
        final String[] finalTypes = types;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                    for (String type : finalTypes) {
                        String tableName = getTableName(type);
                        if (tableName != null) {
                            exportSheet(workbook, type, tableName, finalJobId);
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(finalFilePath)) {
                        workbook.write(fos);
                        fos.flush();
                    }

                    File file = new File(finalFilePath);
                    return file.exists() && file.length() > 0;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(DetailPanel.this,
                                    "导出失败: " + ex.getMessage() + "\n目标路径: " + finalFilePath,
                                    "错误", JOptionPane.ERROR_MESSAGE)
                    );
                    return false;
                }
            }

            @Override
            protected void done() {
                btnExportCurrent.setEnabled(true);
                btnExportAll.setEnabled(true);
                btnExportCurrent.setText("导出当前类型");
                btnExportAll.setText("导出全部类型");

                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(DetailPanel.this,
                                "导出成功！\n文件保存至: " + finalFilePath,
                                "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DetailPanel.this,
                            "导出过程发生异常: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ----- 导出Sheet -----
    private void exportSheet(Workbook workbook, String sheetName, String tableName, String jobIdFilter) {
        try (Connection conn = parent.getConnection()) {
            String sql = "SELECT * FROM " + tableName;
            if (!jobIdFilter.isEmpty()) sql += " WHERE job_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            if (!jobIdFilter.isEmpty()) ps.setString(1, jobIdFilter);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < colCount; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(meta.getColumnName(i + 1));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < colCount; i++) {
                    Object val = rs.getObject(i + 1);
                    Cell cell = row.createCell(i);
                    if (val == null) {
                        cell.setCellValue("");
                    } else if (val instanceof String) {
                        cell.setCellValue((String) val);
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else if (val instanceof Date) {
                        cell.setCellValue((Date) val);
                    } else if (val instanceof Timestamp) {
                        cell.setCellValue(((Timestamp) val).toString());
                    } else if (val instanceof Boolean) {
                        cell.setCellValue((Boolean) val);
                    } else {
                        cell.setCellValue(val.toString());
                    }
                }
            }

            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
            }

            rs.close();
            ps.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}