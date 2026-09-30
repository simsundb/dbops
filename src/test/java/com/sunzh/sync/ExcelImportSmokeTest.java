package com.sunzh.sync;

import com.sunzh.sync.ExcelImportEngine.Dialect;
import com.sunzh.sync.ExcelImportEngine.ErrorRecord;
import com.sunzh.sync.ExcelImportEngine.SheetResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ExcelImportEngine 纯逻辑单测（无需数据库）：
 * 1. Sheet 序号/名称 -> 表名 的生成规则
 * 2. 异常记录 Excel 的生成
 * 3. 单元格取值（含公式错误结果）
 * 4. 列长上限截断：Oracle 按字节、GaussDB 按字符，且不切断多字节字符
 */
public class ExcelImportSmokeTest {

    @Test
    public void tableNameFromSheet() throws Exception {
        Dialect gauss = ExcelImportEngine.GAUSSDB;
        Dialect oracle = ExcelImportEngine.ORACLE;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet cn = wb.createSheet("一月");
            Sheet punct = wb.createSheet("!!!");
            Sheet num = wb.createSheet("2023数据");

            Set<String> used = new HashSet<>();
            // 中文 Sheet 名 -> 拼音后缀
            Assert.assertEquals("xiaoshoushuju_yiyue",
                    ExcelImportEngine.buildTableName(gauss, "xiaoshoushuju", ExcelImportEngine.sheetSuffix(cn, 0), used));
            // Oracle 大写
            Assert.assertEquals("XIAOSHOUSHUJU_YIYUE",
                    ExcelImportEngine.buildTableName(oracle, "xiaoshoushuju", ExcelImportEngine.sheetSuffix(cn, 0), new HashSet<>()));
            // 无法解析的 Sheet 名 -> 回退序号 _1
            Assert.assertEquals("data_1",
                    ExcelImportEngine.buildTableName(gauss, "data", ExcelImportEngine.sheetSuffix(punct, 0), new HashSet<>()));
            // 数字开头 Sheet 名也保留
            Assert.assertEquals("data_2023shuju",
                    ExcelImportEngine.buildTableName(gauss, "data", ExcelImportEngine.sheetSuffix(num, 2), new HashSet<>()));
            // 同名 Sheet 后缀去重
            Set<String> used2 = new HashSet<>();
            Assert.assertEquals("data_yiyue",
                    ExcelImportEngine.buildTableName(gauss, "data", ExcelImportEngine.sheetSuffix(cn, 0), used2));
            Assert.assertEquals("data_yiyue_2",
                    ExcelImportEngine.buildTableName(gauss, "data", ExcelImportEngine.sheetSuffix(cn, 3), used2));
            // 超长截断：保证完整表名不超过上限
            String longBase = "abcdefghijklmnopqrstuvwxyz0123456789";
            String name = ExcelImportEngine.buildTableName(oracle, longBase, "aaaaaaaaaa", new HashSet<>());
            Assert.assertTrue("Oracle 表名超 30: " + name, name.length() <= 30);
        }
    }

    @Test
    public void errorWorkbookWritten() throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "excel_smoke_" + System.nanoTime());
        Assert.assertTrue(dir.mkdirs());
        File src = new File(dir, "销售数据.xlsx");

        List<SheetResult> results = new ArrayList<>();

        SheetResult s1 = new SheetResult();
        s1.sheetName = "一月";
        s1.tableName = "XIAOSHUSHUJU_YIYUE";
        s1.rawHeaders = Arrays.asList("姓名", "金额");
        s1.colNames = Arrays.asList("XINGMING", "JINE");
        s1.success = 99;
        s1.fail = 1;
        s1.tableCreated = true;
        s1.errors.add(new ErrorRecord("一月", 5, Arrays.asList("张三", "ABC"), "ORA-12899: 值过大"));
        results.add(s1);

        SheetResult s2 = new SheetResult();
        s2.sheetName = "二月";
        s2.tableName = "XIAOSHUSHUJU_ERYUE";
        s2.rawHeaders = Arrays.asList("姓名", "金额");
        s2.colNames = Arrays.asList("XINGMING", "JINE");
        s2.success = 100;
        s2.fail = 0;
        s2.tableCreated = true;
        results.add(s2);

        File out = ExcelImportEngine.writeErrorWorkbook(src, "销售数据", results);
        System.out.println("WROTE: " + out.getAbsolutePath());
        Assert.assertTrue("异常记录文件未生成", out.exists());
        Assert.assertTrue(out.getName().startsWith("销售数据_异常记录"));
    }

    @Test
    public void formulaCellToStringHandlesErrorResult() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("s");
            Row row = sheet.createRow(0);
            // 数值结果公式
            Cell numeric = row.createCell(0);
            numeric.setCellFormula("1+2");
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(numeric);
            Assert.assertEquals("3", ExcelImportEngine.cellToString(numeric));
            // 字符串结果公式
            Cell text = row.createCell(1);
            text.setCellFormula("\"abc\"");
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(text);
            Assert.assertEquals("abc", ExcelImportEngine.cellToString(text));
            // 错误结果公式（1/0 -> #DIV/0!）：修复前 getNumericCellValue() 抛 IllegalStateException
            Cell err = row.createCell(2);
            err.setCellFormula("1/0");
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(err);
            Assert.assertEquals("#DIV/0!", ExcelImportEngine.cellToString(err));
        }
    }

    @Test
    public void errorFormulaColumnWidenedTo4000() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("s");
            // 表头行
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("名称");
            h.createCell(1).setCellValue("数量");
            // 数据行：普通文本 + 错误公式
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("张三");
            Cell err = r1.createCell(1);
            err.setCellFormula("1/0");
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(err);
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("李四");
            r2.createCell(1).setCellValue(5.0);

            int[] maxLen = ExcelImportEngine.fixedColumnLengths(2);
            // 不再采样探测：两列都固定 4000
            Assert.assertEquals("文本列固定 4000", 4000, maxLen[0]);
            Assert.assertEquals("含错误公式的列固定 4000", 4000, maxLen[1]);
        }
    }

    @Test
    public void oracleTruncatesByBytesNotChars() {
        // Oracle 的 VARCHAR2(4000) 按字节：ASCII 1 字节 + 汉字 3 字节 = 正好 4000
        String mixed = "a" + "中".repeat(2000);
        String fitted = ExcelImportEngine.truncate(ExcelImportEngine.ORACLE, mixed);
        Assert.assertEquals("a" + "中".repeat(1333), fitted);
        Assert.assertEquals("截断后必须正好是 4000 字节", 4000, fitted.getBytes(StandardCharsets.UTF_8).length);

        // 4 字节 emoji：不能把代理对切成两半
        String emoji = "😀".repeat(1500);
        String fittedEmoji = ExcelImportEngine.truncate(ExcelImportEngine.ORACLE, emoji);
        Assert.assertEquals(4000, fittedEmoji.getBytes(StandardCharsets.UTF_8).length);
        Assert.assertEquals("代理对必须成对保留", 1000, fittedEmoji.codePointCount(0, fittedEmoji.length()));
        Assert.assertEquals("😀".repeat(1000), fittedEmoji);
    }

    @Test
    public void gaussdbTruncatesByChars() {
        // GaussDB 的 VARCHAR(4000) 按字符：4000 个汉字就是 4000 字符，不按字节削
        String cn = "中".repeat(5000);
        String fitted = ExcelImportEngine.truncate(ExcelImportEngine.GAUSSDB, cn);
        Assert.assertEquals(4000, fitted.length());
        Assert.assertEquals("GaussDB 按字符算，汉字不该被按字节提前削短", "中".repeat(4000), fitted);
    }

    @Test
    public void valuesWithinLimitAreUntouched() {
        List<String> headers = Arrays.asList("备注", "名称");

        String[] v1 = {"短文本", "中".repeat(4000)};
        Assert.assertTrue("未超长不应返回截断描述",
                ExcelImportEngine.fitColumnsToLimit(ExcelImportEngine.GAUSSDB, headers, v1).isEmpty());
        Assert.assertEquals("中".repeat(4000), v1[1]);

        // 超长：就地截断，返回可读的列名 + 长度变化
        String[] v2 = {"中".repeat(5000), "ok"};
        List<String> cut = ExcelImportEngine.fitColumnsToLimit(ExcelImportEngine.GAUSSDB, headers, v2);
        Assert.assertEquals(1, cut.size());
        Assert.assertEquals("备注(5000->4000)", cut.get(0));
        Assert.assertEquals(4000, v2[0].length());
        Assert.assertEquals("ok", v2[1]);
    }

    @Test
    public void truncationRecordedInErrorWorkbook() throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "excel_trunc_" + System.nanoTime());
        Assert.assertTrue(dir.mkdirs());
        File src = new File(dir, "销售数据.xlsx");

        SheetResult s = new SheetResult();
        s.sheetName = "一月";
        s.tableName = "XIAOSHUSHUJU_YIYUE";
        s.rawHeaders = Arrays.asList("姓名", "备注");
        s.colNames = Arrays.asList("XINGMING", "BEIZHU");
        s.success = 10;
        s.fail = 0;
        s.truncated = 1; // 已成功入库，只记告警
        s.tableCreated = true;
        s.truncations.add(new ErrorRecord("一月", 3,
                Arrays.asList("张三", "中".repeat(5000)), "值超长已截断到 4000: 备注(5000->4000)"));

        File out = ExcelImportEngine.writeErrorWorkbook(src, "销售数据", Arrays.asList(s));
        Assert.assertTrue(out.exists());

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            Sheet sum = wb.getSheet("汇总");
            Row row = sum.getRow(1);
            Assert.assertEquals("截断行不计入失败", 0, (int) row.getCell(4).getNumericCellValue());
            Assert.assertEquals(1, (int) row.getCell(5).getNumericCellValue());
            Assert.assertTrue("状态要体现出截断", row.getCell(6).getStringCellValue().contains("1行截断"));

            // 明细 Sheet：类型列区分"导入失败"与"超长截断"，且保留截断前的原值
            Sheet detail = wb.getSheet("一月");
            Assert.assertNotNull("只有截断也要出明细 Sheet", detail);
            Row d = detail.getRow(1);
            Assert.assertEquals(3, (int) d.getCell(0).getNumericCellValue());
            Assert.assertEquals("超长截断", d.getCell(1).getStringCellValue());
            Assert.assertEquals(5000, d.getCell(3).getStringCellValue().length());
            Assert.assertTrue(d.getCell(4).getStringCellValue().contains("值超长已截断到 4000"));
        }
    }

    @Test
    public void parseTokenDeletesSpecialSymbolNoise() throws Exception {
        // 中文 -> 拼音
        Assert.assertEquals("ceshi", ExcelImportEngine.parseToken("测试"));
        // 特殊符号噪音被删除（@、-、括号、emoji 等）
        Assert.assertEquals("ceshi2024zuizhongban", ExcelImportEngine.parseToken("测试@2024@最终版"));
        Assert.assertEquals("xiaoshoushuju", ExcelImportEngine.parseToken("销售-数据"));
        Assert.assertEquals("2024shuju", ExcelImportEngine.parseToken("(2024)数据"));
        Assert.assertEquals("ceshishuju", ExcelImportEngine.parseToken("测试📊数据"));
        // 下划线保留
        Assert.assertEquals("shuju_biaozhun", ExcelImportEngine.parseToken("数据_标准"));
        // 全部为特殊符号 -> null
        Assert.assertNull(ExcelImportEngine.parseToken("!!!"));
    }
}
