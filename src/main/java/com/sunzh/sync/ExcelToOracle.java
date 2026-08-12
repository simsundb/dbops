package com.sunzh.sync;

/**
 * Excel -> Oracle 导入工具（多Sheet版）。
 * 用法: java -cp ".:lib/*" ExcelToOracle <jdbc-url> <user> <password> <excel-file>
 *
 * JDBC URL 示例:
 *   jdbc:oracle:thin:@//host:1521/service_name
 *   jdbc:oracle:thin:@host:1521:SID
 *
 * 每个 Excel Sheet 单独建表；有异常行时生成 <文件名>_异常记录.xlsx。
 * 实际逻辑见 {@link ExcelImportEngine}。
 */
public class ExcelToOracle {

    public static void main(String[] args) {
        ConsoleEncoding.configureUtf8();
        if (args.length < 4) {
            System.out.println("用法: java -cp \".:lib/*\" ExcelToOracle <jdbc-url> <user> <password> <excel-file>");
            System.out.println("示例: java -cp \".:lib/*\" ExcelToOracle jdbc:oracle:thin:@//127.0.0.1:1521/ORCL scott tiger data.xlsx");
            System.exit(1);
        }
        int code = ExcelImportEngine.run(ExcelImportEngine.ORACLE, args[0], args[1], args[2], args[3]);
        System.exit(code);
    }
}
