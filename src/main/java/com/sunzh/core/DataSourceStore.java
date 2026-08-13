package com.sunzh.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunzh.utils.ExternalConfigUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class DataSourceStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "DATASOURCE.JSON";

    /**
     * 获取配置文件路径：JAR 同级 ./conf/DATASOURCE.JSON
     */
    private static File getConfigFile() {
        return new File(ExternalConfigUtils.CONF_DIR + FILE_NAME);
    }

    private static class ConfigRoot {
        List<DataSource> datasources;
    }

    /**
     * 如果 conf/DATASOURCE.JSON 不存在：
     * 1. 优先迁移旧版本 user.dir 下的 DATASOURCE.JSON（避免用户已有数据丢失）
     * 2. 迁移不可用则从 classpath（JAR 内 resources 目录）复制默认模板出来
     * 这样打包后首次运行也能有默认配置文件，且之后读写都走外部 conf/，用户可编辑。
     */
    private static void initFromClasspathIfNeeded(File configFile) {
        if (configFile.exists()) return;

        // 兼容旧版本：迁移 user.dir 下的 DATASOURCE.JSON
        File legacy = new File(System.getProperty("user.dir"), FILE_NAME);
        if (legacy.isFile()) {
            try {
                configFile.getParentFile().mkdirs();
                Files.copy(legacy.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("已迁移旧配置到: " + configFile.getAbsolutePath());
                return;
            } catch (Exception e) {
                System.err.println("迁移旧 DATASOURCE.JSON 失败，改用 JAR 默认: " + e.getMessage());
            }
        }

        try (InputStream in = DataSourceStore.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) {
                configFile.getParentFile().mkdirs();
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("已从 JAR/resources 复制默认配置: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("从 classpath 复制配置文件失败: " + e.getMessage());
        }
    }

    public static List<DataSource> load() {
        File configFile = getConfigFile();
        initFromClasspathIfNeeded(configFile);
        if (!configFile.exists()) {
            System.err.println("[警告] 未找到数据源配置文件: " + configFile.getAbsolutePath());
            System.err.println("   请把 DATASOURCE.JSON 放到程序所在目录的 conf/ 下（即上面这个路径），");
            System.err.println("   或在“数据源管理”界面新增数据源后保存。当前数据源列表为空。");
            return new ArrayList<>();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            ConfigRoot root = GSON.fromJson(reader, ConfigRoot.class);
            if (root != null && root.datasources != null) {
                return root.datasources;
            }
        } catch (Exception e) {
            System.err.println("加载配置失败: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static void save(List<DataSource> list) {
        ConfigRoot root = new ConfigRoot();
        root.datasources = list;
        File configFile = getConfigFile();
        File parent = configFile.getParentFile();
        if (parent != null) parent.mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            System.out.println("配置文件已保存: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}