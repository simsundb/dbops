package com.sunzh.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 统一外部配置定位工具。
 *
 * 设计目标：所有可编辑的配置文件 / SQL 模板都放在打包 JAR 同级的 {@code ./conf/} 目录下，
 * 按功能分子目录分门别类存储（如 {@code conf/inspection/}、{@code conf/stats/}、
 * {@code conf/DATASOURCE.JSON}）。
 *
 * 读取策略：优先读外部文件（用户可自由修改、程序可保存回写）；外部不存在时，
 * 自动从 classpath（JAR 内默认模板）复制一份出来再读。这样打包后首次运行即可
 * 自动生成 conf/ 模板，之后一直读写外部文件。
 *
 * 注意：query 目录等包含多个文件的场景，不要在 JAR 内用 {@code new File(url.toURI())}
 * 枚举目录——打包后 classpath 是 {@code jar:file:!/query/}，不是 file URL，会失败。
 * 应改用 {@link #ensureExternalFile} 按已知文件名逐个复制。
 */
public final class ExternalConfigUtils {

    /** 外部配置根目录（JAR 同级 ./conf/） */
    public static final String CONF_DIR = "./conf/";

    private ExternalConfigUtils() {
    }

    /**
     * 确保外部配置文件存在并返回它。
     * <p>如果外部文件已存在，直接返回（用户已自定义，不覆盖）；
     * 否则从 classpath 复制默认模板到外部目录（自动创建父目录）。</p>
     *
     * @param confSubDir   conf/ 下的子目录，如 {@code "inspection"}、{@code "stats"}；放 conf/ 根目录传空串
     * @param fileName     外部文件名，如 {@code "config.yaml"}、{@code "gsi_check.sql"}
     * @param classpathRes classpath 资源路径，如 {@code "/config.yaml"}、{@code "/query/gsi_check.sql"}
     * @return 外部文件；外部和 classpath 都没有时返回 {@code null}
     */
    public static File ensureExternalFile(String confSubDir, String fileName, String classpathRes) {
        File external = new File(CONF_DIR + joinPath(confSubDir, fileName));
        if (external.isFile()) return external;

        try (InputStream in = ExternalConfigUtils.class.getResourceAsStream(classpathRes)) {
            if (in == null) {
                System.err.println("classpath 资源不存在: " + classpathRes);
                return null;
            }
            File parent = external.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(in, external.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("📦 已从 JAR/resources 复制默认配置: " + external.getAbsolutePath());
            return external;
        } catch (IOException e) {
            System.err.println("复制外部配置失败: " + external.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    private static String joinPath(String dir, String name) {
        if (dir == null || dir.isEmpty()) return name;
        return dir.endsWith("/") || dir.endsWith("\\") ? dir + name : dir + "/" + name;
    }
}
