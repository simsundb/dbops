package com.example.stats;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 表格导出工具：将 JTable 数据导出为 Excel 文件
 */
public class TableExportUtil {

    /**
     * 弹出文件保存对话框，将表格数据导出为 .xlsx
     *
     * @param table   源表格
     * @param parent  父组件（用于弹窗定位）
     * @param sheetName Sheet 名称（默认取任务描述）
     */
    public static void exportToExcel(JTable table, java.awt.Component parent, String sheetName) {
        if (table == null || table.getModel().getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "没有数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 弹出保存对话框
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出 Excel");
        String safeName = (sheetName != null ? sheetName : "导出数据").replaceAll("[\\\\/:*?\"<>|]", "_");
        fileChooser.setSelectedFile(new java.io.File(safeName + ".xlsx"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel 文件 (*.xlsx)", "xlsx"));

        int result = fileChooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // 用户取消
        }

        java.io.File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new java.io.File(file.getAbsolutePath() + ".xlsx");
        }

        try {
            writeExcel(table, file, safeName);
            JOptionPane.showMessageDialog(parent,
                    "导出成功！\n文件已保存到：\n" + file.getAbsolutePath(),
                    "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "导出失败：" + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * 将多张表格导出到同一个 Excel 的不同 Sheet
     */
    public static void exportMultipleToExcel(java.util.Map<String, JTable> tableMap, java.awt.Component parent) {
        if (tableMap == null || tableMap.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "没有数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出全部任务为 Excel");
        fileChooser.setSelectedFile(new java.io.File("统计结果汇总.xlsx"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel 文件 (*.xlsx)", "xlsx"));

        int result = fileChooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new java.io.File(file.getAbsolutePath() + ".xlsx");
        }

        Workbook workbook = new XSSFWorkbook();
        try {
            for (java.util.Map.Entry<String, JTable> entry : tableMap.entrySet()) {
                String name = entry.getKey().replaceAll("[\\\\/:*?\"<>|]", "_");
                if (name.length() > 31) name = name.substring(0, 31); // Excel sheet名最大31字符
                JTable table = entry.getValue();
                Sheet sheet = workbook.createSheet(name);
                writeSheet(table, sheet);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.close();

            JOptionPane.showMessageDialog(parent,
                    "全部导出成功！\n文件已保存到：\n" + file.getAbsolutePath(),
                    "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "导出失败：" + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void writeExcel(JTable table, java.io.File file, String sheetName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        String name = sheetName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.length() > 31) name = name.substring(0, 31);
        Sheet sheet = workbook.createSheet(name);
        writeSheet(table, sheet);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private static void writeSheet(JTable table, Sheet sheet) {
        // 表头样式
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 斑马纹样式
        CellStyle evenStyle = sheet.getWorkbook().createCellStyle();
        evenStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle oddStyle = sheet.getWorkbook().createCellStyle();
        oddStyle.setAlignment(HorizontalAlignment.CENTER);

        // 写表头
        int colCount = table.getColumnCount();
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < colCount; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(table.getColumnName(c));
            cell.setCellStyle(headerStyle);
        }

        // 写数据
        int rowCount = table.getRowCount();
        for (int r = 0; r < rowCount; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < colCount; c++) {
                Cell cell = row.createCell(c);
                Object val = table.getValueAt(r, c);
                if (val != null) {
                    // 尝试写数字，失败则写字符串
                    try {
                        cell.setCellValue(Double.parseDouble(val.toString()));
                    } catch (NumberFormatException e) {
                        cell.setCellValue(val.toString());
                    }
                }
                cell.setCellStyle(r % 2 == 0 ? evenStyle : oddStyle);
            }
        }

        // 自动列宽
        for (int c = 0; c < colCount; c++) {
            sheet.autoSizeColumn(c);
            int width = sheet.getColumnWidth(c);
            sheet.setColumnWidth(c, Math.min(width + 800, 12000)); // 适当加宽 + 上限
        }
    }
}
