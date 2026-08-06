package com.sunzh.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
     * 获取配置文件路径：当前工作目录（user.dir）
     */
    private static File getConfigFile() {
        String userDir = System.getProperty("user.dir");
        return new File(userDir, FILE_NAME);
    }

    private static class ConfigRoot {
        List<DataSource> datasources;
    }

    /**
     * 如果 user.dir 下不存在 DATASOURCE.JSON，从 classpath（JAR 内或 resources 目录）复制一份出来。
     * 这样打包后首次运行也能有默认配置文件，且复制到 user.dir 后可正常读写。
     */
    private static void initFromClasspathIfNeeded(File configFile) {
        if (configFile.exists()) return;
        try (InputStream in = DataSourceStore.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) {
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
        if (!configFile.exists()) return new ArrayList<>();
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
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            System.out.println("配置文件已保存: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}