package com.sunzh.sync;

/**
 * Excel -> GaussDB 导入工具（多Sheet版）。
 * 用法: java -cp ".:lib/*" ExcelToGaussDB <jdbc-url> <user> <password> <excel-file>
 *
 * JDBC URL 示例:
 *   jdbc:gaussdb://host:8000/database
 *
 * 每个 Excel Sheet 单独建表；有异常行时生成 <文件名>_异常记录.xlsx。
 * 实际逻辑见 {@link ExcelImportEngine}。
 */
public class ExcelToGaussDB {

    public static void main(String[] args) {
        ConsoleEncoding.configureUtf8();
        if (args.length < 4) {
            System.out.println("用法: java -cp \".:lib/*\" ExcelToGaussDB <jdbc-url> <user> <password> <excel-file>");
            System.out.println("示例: java -cp \".:lib/*\" ExcelToGaussDB jdbc:gaussdb://127.0.0.1:8000/mydb root pass123 data.xlsx");
            System.exit(1);
        }
        int code = ExcelImportEngine.run(ExcelImportEngine.GAUSSDB, args[0], args[1], args[2], args[3]);
        System.exit(code);
    }
}
