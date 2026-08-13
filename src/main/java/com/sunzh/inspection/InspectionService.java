package com.sunzh.inspection;
import com.sunzh.utils.EncodingUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InspectionService {

    private static final Logger LOGGER = Logger.getLogger(InspectionService.class.getName());

    public interface ProgressListener {
        void onTaskStart(InspectionTask task, int index, int total);
        void onTaskComplete(InspectionTask task, long elapsedSeconds);
        void onLog(String message);
        void onProgress(int current, int total);
        void onFinished(int totalSuccess, int totalNoData, int totalFailed, int totalSkipped, long totalSeconds);
    }

    public List<InspectionTask> loadTasks(File configFile, File queryDir) throws IOException {
        if (!configFile.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + configFile.getAbsolutePath());
        }

        List<InspectionTask> tasks = new ArrayList<>();
        try (InputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            // 自动识别编码（UTF-8/GBK），避免 description 乱码
            List<Map<String, Object>> rawList = yaml.load(EncodingUtils.readText(input));
            if (rawList == null) return tasks;

            for (Map<String, Object> item : rawList) {
                String desc = (String) item.get("description");
                Boolean enabled = (Boolean) item.get("enabled");
                String sqlFile = (String) item.get("sqlFile");
                String sql = (String) item.get("sql");

                if (sqlFile != null && !sqlFile.trim().isEmpty()) {
                    Path filePath = queryDir.toPath().resolve(sqlFile);
                    try {
                        if (Files.exists(filePath)) {
                            // 自动识别编码（UTF-8/GBK），避免 SQL 注释/内容乱码
                            sql = EncodingUtils.readText(filePath.toFile());
                        } else {
                            // 文件不在外部 query 目录：先从 classpath (JAR 内 resources/query/) 复制到外部目录再读，
                            // 这样用户可编辑 conf/inspection/query/*.sql
                            InputStream sqlIn = getClass().getResourceAsStream("/query/" + sqlFile);
                            if (sqlIn != null) {
                                Files.createDirectories(queryDir.toPath());
                                Files.copy(sqlIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                                System.out.println("已从 JAR 复制巡检 SQL: " + filePath.toAbsolutePath());
                                sqlIn.close();
                                sql = EncodingUtils.readText(filePath.toFile());
                            } else {
                                LOGGER.log(Level.WARNING, "SQL 文件不存在（外部目录和 classpath 均未找到）: " + sqlFile + "，任务: " + desc);
                                continue;
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "读取 SQL 文件失败: " + sqlFile + "，任务: " + desc, e);
                        continue;
                    }
                }

                if (sql == null || sql.trim().isEmpty()) {
                    LOGGER.warning("任务 [" + desc + "] 无 SQL 内容，已跳过");
                    continue;
                }

                InspectionTask task = new InspectionTask();
                task.setDescription(desc);
                task.setSql(sql);
                task.setSqlFile(sqlFile);
                task.setEnabled(enabled != null && enabled);
                tasks.add(task);
            }
        }
        return tasks;
    }

    public InspectionTask.Status executeTask(Connection conn, InspectionTask task, File outputDir) {
        String desc = task.getDescription();
        int rowCount = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(task.getSql())) {

            if (!rs.next()) {
                task.setStatus(InspectionTask.Status.NO_DATA);
                task.setRowCount(0);
                return InspectionTask.Status.NO_DATA;
            }

            String fileName = sanitizeFileName(desc) + ".xlsx";
            File outFile = new File(outputDir, fileName);
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("结果");
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                Row header = sheet.createRow(0);
                for (int i = 1; i <= colCount; i++) {
                    header.createCell(i - 1).setCellValue(meta.getColumnLabel(i));
                }

                int rowNum = 1;
                do {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);
                        row.createCell(i - 1).setCellValue(value != null ? value.toString() : "");
                    }
                } while (rs.next());
                rowCount = rowNum - 1;

                for (int i = 0; i < colCount; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    workbook.write(fos);
                }
            }

            task.setStatus(InspectionTask.Status.SUCCESS);
            task.setOutputFileName(fileName);
            task.setRowCount(rowCount);
            return InspectionTask.Status.SUCCESS;

        } catch (SQLException | IOException e) {
            task.setStatus(InspectionTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            LOGGER.log(Level.SEVERE, "执行任务失败: " + desc, e);
            return InspectionTask.Status.FAILED;
        }
    }

    public void runInspection(Connection conn, List<InspectionTask> tasks, File outputDir,
                              ProgressListener listener) {
        int total = tasks.size();
        int success = 0, noData = 0, failed = 0, skipped = 0;

        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File finalOutputDir = new File(outputDir, timeStamp);
        if (!finalOutputDir.exists() && !finalOutputDir.mkdirs()) {
            listener.onLog("[失败] 创建输出目录失败: " + finalOutputDir.getAbsolutePath());
            return;
        }

        listener.onLog("结果将保存到: " + finalOutputDir.getAbsolutePath());

        long startAll = System.currentTimeMillis();
        int index = 0;
        for (InspectionTask task : tasks) {
            index++;
            if (!task.isEnabled()) {
                task.setStatus(InspectionTask.Status.SKIPPED);
                skipped++;
                listener.onTaskStart(task, index, total);
                listener.onTaskComplete(task, 0);
                listener.onProgress(index, total);
                continue;
            }

            listener.onTaskStart(task, index, total);
            long taskStart = System.currentTimeMillis();
            InspectionTask.Status status = executeTask(conn, task, finalOutputDir);
            long elapsed = (System.currentTimeMillis() - taskStart) / 1000;

            switch (status) {
                case SUCCESS: success++; break;
                case NO_DATA: noData++; break;
                case FAILED: failed++; break;
                default: break;
            }
            listener.onTaskComplete(task, elapsed);
            listener.onProgress(index, total);
        }

        long totalSec = (System.currentTimeMillis() - startAll) / 1000;
        listener.onFinished(success, noData, failed, skipped, totalSec);
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}