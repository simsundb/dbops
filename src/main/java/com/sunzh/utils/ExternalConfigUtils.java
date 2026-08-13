package com.sunzh.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 统一外部配置定位工具。
 *
 * 设计目标：所有可编辑的配置文件 / SQL 模板都放在打包 JAR 同级的 {@code ./conf/} 目录下，
 * 按功能分子目录分门别类存储（如 {@code conf/inspection/}、{@code conf/stats/}、
 * {@code conf/DATASOURCE.JSON}）。
 *
 * conf 根目录定位规则：
 * 1. 默认读取软件包（JAR）所在目录下的 conf/（部署：jar 与 conf 放一起，自包含）
 * 2. 若无 → 使用当前目录（user.dir）的 conf/；启动时由
 *    {@link #exportBundledDefaults()} 从包内 resources 复制全部默认配置过来
 * 开发模式（非 jar 运行，如 IDE / mvn test）直接使用 user.dir/conf，保持测试行为不变。
 *
 * 读取策略：优先读外部文件（用户可自由修改、程序可保存回写）；外部不存在时，
 * 自动从 classpath（JAR 内默认模板）复制一份出来再读。这样打包后首次运行即可
 * 自动生成 conf/ 模板，之后一直读写外部文件。
 * 启动时调用 {@link #exportBundledDefaults()}，把 JAR 内全部默认配置一次性导出到 conf/。
 *
 * 注意：query 目录等包含多个文件的场景，不要在 JAR 内用 {@code new File(url.toURI())}
 * 枚举目录——打包后 classpath 是 {@code jar:file:!/query/}，不是 file URL，会失败。
 * 应改用 {@link #ensureExternalFile} 或 {@link #exportBundledDefaults()} 按已知文件名逐个复制。
 */
public final class ExternalConfigUtils {

    /** 外部配置根目录（智能定位，结尾带目录分隔符） */
    public static final String CONF_DIR = resolveConfigDir();

    private ExternalConfigUtils() {
    }

    /** 返回外部配置根目录（conf 目录本身）。 */
    public static File getConfigDir() {
        return new File(CONF_DIR);
    }

    /**
     * 启动时把 JAR 内所有默认配置导出到 conf/ 根目录下。
     * 已存在的文件不覆盖（用户自定义优先）；JAR 内不存在的资源自动跳过。
     */
    public static void exportBundledDefaults() {
        File root = getConfigDir();
        root.mkdirs();

        // 各功能模块的配置文件（对应各模块 ensureExternalFile 的落点）
        copyIfMissing("/DATASOURCE.JSON", new File(root, "DATASOURCE.JSON"));
        copyIfMissing("/config.yaml", new File(root, "inspection/config.yaml"));
        copyIfMissing("/stats_config.yaml", new File(root, "stats/stats_config.yaml"));
        copyIfMissing("/ssh_profiles.json", new File(root, "ssh/ssh_profiles.json"));

        // SQL 模板目录（query -> conf/inspection/query，sql -> conf/sql，stats -> conf/stats）
        exportResourceDir("query/", new File(root, "inspection/query"));
        exportResourceDir("sql/", new File(root, "sql"));
        exportResourceDir("stats/", new File(root, "stats"));
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
            System.out.println("已从 JAR/resources 复制默认配置: " + external.getAbsolutePath());
            return external;
        } catch (IOException e) {
            System.err.println("复制外部配置失败: " + external.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 定位 conf 根目录。见类注释的规则：
     * 软件包（JAR）同级的 conf/ 存在则读它；否则用当前目录 conf/（启动时复制默认配置过来）。
     */
    private static String resolveConfigDir() {
        File jarDir = jarDirIfAny();
        if (jarDir != null) {
            File jarConf = new File(jarDir, "conf");
            if (jarConf.isDirectory()) return withTrailingSep(jarConf);
        }
        // 软件目录无 conf：使用当前目录，启动时由 exportBundledDefaults() 从包内复制默认配置
        return withTrailingSep(new File(System.getProperty("user.dir"), "conf"));
    }

    /**
     * 当前运行环境若为打包 JAR，返回其所在目录；否则（开发模式）返回 null。
     */
    private static File jarDirIfAny() {
        try {
            CodeSource cs = ExternalConfigUtils.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) return null;
            URI uri = cs.getLocation().toURI();
            File loc = new File(uri.getPath());
            if (loc.isFile() && loc.getName().toLowerCase().endsWith(".jar")) {
                return loc.getParentFile();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 复制 JAR 内某目录（如 query/、sql/、stats/）下的所有文件到外部目录。
     * 兼容两种运行环境：JAR 内枚举（jar:file）与开发模式（文件目录）。
     * 仅处理一级子目录下的文件，已存在的文件不覆盖。
     */
    private static void exportResourceDir(String prefix, File destDir) {
        File loc;
        try {
            CodeSource cs = ExternalConfigUtils.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) return;
            loc = new File(cs.getLocation().toURI());
        } catch (Exception e) {
            return;
        }

        if (loc.isFile() && loc.getName().toLowerCase().endsWith(".jar")) {
            try (JarFile jar = new JarFile(loc)) {
                jar.stream()
                   .filter(e -> !e.isDirectory())
                   .filter(e -> e.getName().startsWith(prefix))
                   .map(JarEntry::getName)
                   .forEach(name -> {
                       String rel = name.substring(prefix.length());
                       if (rel.indexOf('/') >= 0) return; // 只复制一级文件，不递归子目录
                       copyIfMissing("/" + name, new File(destDir, rel));
                   });
            } catch (IOException e) {
                System.err.println("枚举 JAR 内资源失败: " + prefix + " - " + e.getMessage());
            }
        } else {
            // 开发模式：直接扫 classpath 源目录
            File srcDir = new File(loc, prefix);
            File[] files = srcDir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isFile()) copyIfMissing("/" + prefix + f.getName(), new File(destDir, f.getName()));
            }
        }
    }

    /**
     * 从 classpath 复制默认文件到外部路径；外部文件已存在或 classpath 资源缺失时跳过。
     */
    private static void copyIfMissing(String classpathRes, File dest) {
        if (dest.isFile()) return; // 用户已自定义/已复制，不覆盖
        try (InputStream in = ExternalConfigUtils.class.getResourceAsStream(classpathRes)) {
            if (in == null) return; // 该资源未打包（如 DATASOURCE.JSON 本地才有），跳过
            File parent = dest.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("已从 JAR 复制默认配置: " + dest.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("复制默认配置失败: " + dest.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private static String joinPath(String dir, String name) {
        if (dir == null || dir.isEmpty()) return name;
        return dir.endsWith("/") || dir.endsWith("\\") ? dir + name : dir + "/" + name;
    }

    private static String withTrailingSep(File f) {
        String p = f.getAbsolutePath();
        return p.endsWith(File.separator) ? p : p + File.separator;
    }
}
