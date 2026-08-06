package com.sunzh.ssh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sunzh.utils.CryptoUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SshProfileStore {
    private static final String CONFIG_FILE = "./conf/ssh/ssh_profiles.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<SshProfile> load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            SshProfile defaultProfile = new SshProfile();
            defaultProfile.setName("默认配置");
            defaultProfile.setSshHost("");
            defaultProfile.setSshPort(22);
            defaultProfile.setSshUser("root");
            defaultProfile.setSshPassword("");
            defaultProfile.setExecUser("Ruby");
            defaultProfile.setDbHost("");
            defaultProfile.setDbPort(8000);
            defaultProfile.setDbName("");
            defaultProfile.setDbUser("");
            defaultProfile.setDbPassword("");
            defaultProfile.setBackupDir("/data/dump");
            List<SshProfile> list = new ArrayList<>();
            list.add(defaultProfile);
            save(list);
            return list;
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
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(copy, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}