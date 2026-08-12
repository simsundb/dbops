package com.sunzh.ssh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunzh.utils.CryptoUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class SshProfileStore {
    private static final String CONFIG_FILE = "./conf/ssh/ssh_profiles.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<SshProfile> load() {
        File file = ensureConfigFile();
        if (file == null || !file.exists()) {
            System.err.println("SSH 配置文件缺失且无默认模板: " + CONFIG_FILE);
            return new ArrayList<>();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            SshProfile[] arr = GSON.fromJson(reader, SshProfile[].class);
            List<SshProfile> list = new ArrayList<>();
            if (arr != null) {
                for (SshProfile p : arr) {
                    if (p.getSshPassword() != null && !p.getSshPassword().isEmpty()) {
                        p.setSshPassword(CryptoUtils.decrypt(p.getSshPassword()));
                    }
                    if (p.getDbPassword() != null && !p.getDbPassword().isEmpty()) {
                        p.setDbPassword(CryptoUtils.decrypt(p.getDbPassword()));
                    }
                    list.add(p);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 确保外部配置文件存在：外部缺失时从 JAR/resources 复制默认模板（自动创建父目录）。
     * 已存在的文件不覆盖（用户自定义优先）。
     */
    private static File ensureConfigFile() {
        File file = new File(CONFIG_FILE);
        if (file.isFile()) return file;
        try (InputStream in = SshProfileStore.class.getResourceAsStream("/ssh_profiles.json")) {
            if (in == null) return null;
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("📦 已从 JAR 复制默认配置: " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            System.err.println("复制 SSH 配置失败: " + file.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    public static void save(List<SshProfile> profiles) {
        File configFile = new File(CONFIG_FILE);
        File parent = configFile.getParentFile();
        if (parent != null) parent.mkdirs();
        List<SshProfile> copy = new ArrayList<>();
        for (SshProfile p : profiles) {
            SshProfile encrypted = new SshProfile();
            encrypted.setName(p.getName());
            encrypted.setSshHost(p.getSshHost());
            encrypted.setSshPort(p.getSshPort());
            encrypted.setSshUser(p.getSshUser());
            encrypted.setSshPassword(CryptoUtils.encrypt(p.getSshPassword()));
            encrypted.setExecUser(p.getExecUser());
            encrypted.setDbHost(p.getDbHost());
            encrypted.setDbPort(p.getDbPort());
            encrypted.setDbName(p.getDbName());
            encrypted.setDbUser(p.getDbUser());
            encrypted.setDbPassword(CryptoUtils.encrypt(p.getDbPassword()));
            encrypted.setBackupDir(p.getBackupDir());
            copy.add(encrypted);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(copy, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}