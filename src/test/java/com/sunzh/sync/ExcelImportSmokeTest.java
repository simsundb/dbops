package com.sunzh.sync;

import com.sunzh.sync.ExcelImportEngine.Dialect;
import com.sunzh.sync.ExcelImportEngine.ErrorRecord;
import com.sunzh.sync.ExcelImportEngine.SheetResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ExcelImportEngine 纯逻辑单测（无需数据库）：
 * 1. Sheet 序号/名称 -> 表名 的生成规则
 * 2. 异常记录 Excel 的生成
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
}
