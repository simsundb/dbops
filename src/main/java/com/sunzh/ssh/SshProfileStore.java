package com.sunzh.ssh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunzh.utils.CryptoUtils;
import com.sunzh.utils.ExternalConfigUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SshProfileStore {
    private static final String CONF_SUB_DIR = "ssh";
    private static final String FILE_NAME = "ssh_profiles.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 获取配置文件：JAR 同级 ./conf/ssh/ssh_profiles.json。
     * 不存在时从 classpath（JAR 内默认模板）复制出来，之后用户可编辑外部文件。
     */
    private static File getConfigFile() {
        return ExternalConfigUtils.ensureExternalFile(CONF_SUB_DIR, FILE_NAME, "/" + FILE_NAME);
    }

    public static List<SshProfile> load() {
        File file = getConfigFile();
        if (file == null || !file.exists()) {
            System.err.println("SSH 配置文件缺失且无默认模板: " + FILE_NAME);
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

    public static void save(List<SshProfile> profiles) {
        File configFile = getConfigFile();
        if (configFile == null) {
            System.err.println("SSH 配置文件目录不可用，无法保存: " + FILE_NAME);
            return;
        }
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