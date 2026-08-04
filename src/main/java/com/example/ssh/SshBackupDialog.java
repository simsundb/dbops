package com.example.ssh;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.ui.BaseDialog;
import com.example.utils.ThemeUtils;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class SshBackupDialog extends BaseDialog {

    private DefaultListModel<String> profileListModel;
    private JList<String> profileList;
    private List<SshProfile> profiles;
    private SshProfile currentProfile;

    private JTextField nameField, sshHostField, sshPortField, sshUserField;
    private JPasswordField sshPasswordField;
    private JTextField execUserField;
    private JTextField dbHostField, dbPortField, dbNameField, dbUserField;
    private JPasswordField dbPasswordField;
    private JTextField backupDirField;

    private JRadioButton dbRadio, schemaRadio, tableRadio;
    private JRadioButton fullRadio, structRadio, dataRadio;
    private JTextField schemaField;
    private JTextArea tableListArea;
    private JCheckBox useTableListCheck;
    private JComboBox<String> formatCombo;

    private JLabel dataSourceLabel;
    private JComboBox<DataSource> dataSourceCombo;

    private JButton saveProfileBtn, deleteProfileBtn, newProfileBtn;
    private JButton executeBtn, stopBtn, closeBtn, clearLogBtn;
    private JTextArea logArea;
    private JProgressBar progressBar;

    private SwingWorker<Void, String> currentWorker;
    private ClientSession session;

    public SshBackupDialog(JFrame owner) {
        super(owner, "SSH 数据库备份", "backup");
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(ThemeUtils.COLOR_BG);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        profiles = SshProfileStore.load();

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setResizeWeight(0.2);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());

        JPanel leftPanel = createProfileListPanel();
        mainSplit.setLeftComponent(leftPanel);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(ThemeUtils.COLOR_BG);

        JPanel topRight = createFormPanel();
        rightPanel.add(topRight, BorderLayout.NORTH);

        JPanel centerRight = createBackupParamsPanel();
        rightPanel.add(centerRight, BorderLayout.CENTER);

        JPanel bottomRight = createBottomPanel();
        rightPanel.add(bottomRight, BorderLayout.SOUTH);

        mainSplit.setRightComponent(rightPanel);
        add(mainSplit, BorderLayout.CENTER);

        if (!profileListModel.isEmpty()) {
            profileList.setSelectedIndex(0);
            loadProfile(profiles.get(0));
        }

        setSize(1150, 880);
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private JPanel createProfileListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "SSH 配置列表",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        profileListModel = new DefaultListModel<>();
        for (SshProfile p : profiles) {
            profileListModel.addElement(p.getName());
        }
        profileList = new JList<>(profileListModel);
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = profileList.getSelectedIndex();
                if (idx >= 0 && idx < profiles.size()) {
                    loadProfile(profiles.get(idx));
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(profileList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(listScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        btnPanel.setOpaque(false);
        newProfileBtn = new JButton("➕ 新建");
        newProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        newProfileBtn.addActionListener(e -> createNewProfile());
        btnPanel.add(newProfileBtn);

        deleteProfileBtn = new JButton("🗑 删除");
        deleteProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        deleteProfileBtn.addActionListener(e -> deleteProfile());
        btnPanel.add(deleteProfileBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "配置详情",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("配置名称:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        nameField = new JTextField(15);
        panel.add(nameField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("SSH 主机:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        sshHostField = new JTextField(15);
        panel.add(sshHostField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("端口:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.1;
        sshPortField = new JTextField(5);
        panel.add(sshPortField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("SSH 用户:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        sshUserField = new JTextField(15);
        panel.add(sshUserField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        sshPasswordField = new JPasswordField(15);
        panel.add(sshPasswordField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("执行用户:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        execUserField = new JTextField(15);
        panel.add(execUserField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("DB 主机:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        dbHostField = new JTextField(15);
        panel.add(dbHostField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("端口:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.1;
        dbPortField = new JTextField(5);
        panel.add(dbPortField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("数据库名:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        dbNameField = new JTextField(15);
        panel.add(dbNameField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("DB 用户:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        dbUserField = new JTextField(15);
        panel.add(dbUserField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("DB 密码:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        dbPasswordField = new JPasswordField(15);
        panel.add(dbPasswordField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("备份目录:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        backupDirField = new JTextField(20);
        panel.add(backupDirField, gbc);
        gbc.gridx = 3; gbc.weightx = 0;
        saveProfileBtn = new JButton("💾 保存");
        saveProfileBtn.setBackground(ThemeUtils.COLOR_PRIMARY);
        saveProfileBtn.setForeground(Color.WHITE);
        saveProfileBtn.setFocusPainted(false);
        saveProfileBtn.setBorderPainted(false);
        saveProfileBtn.setPreferredSize(new Dimension(80, 28));
        saveProfileBtn.addActionListener(e -> saveCurrentProfile());
        panel.add(saveProfileBtn, gbc);

        return panel;
    }

    private JPanel createBackupParamsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "备份参数",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));
        // 设置最小高度，防止压缩
        panel.setMinimumSize(new Dimension(400, 320));

        // 统一的布局约束
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        // 统一标签宽度
        Dimension labelDim = new Dimension(80, 25);
        // 统一输入组件首选宽度
        Dimension fieldDim = new Dimension(220, 25);

        int row = 0;
        // 粒度
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel grainLabel = new JLabel("粒度:");
        grainLabel.setPreferredSize(labelDim);
        grainLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(grainLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        JPanel grainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        grainPanel.setOpaque(false);
        dbRadio = new JRadioButton("数据库级", true);
        schemaRadio = new JRadioButton("模式级");
        tableRadio = new JRadioButton("表级");
        ButtonGroup grainGroup = new ButtonGroup();
        grainGroup.add(dbRadio);
        grainGroup.add(schemaRadio);
        grainGroup.add(tableRadio);
        grainPanel.add(dbRadio);
        grainPanel.add(schemaRadio);
        grainPanel.add(tableRadio);
        panel.add(grainPanel, gbc);
        row++;

        // 内容
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        JLabel contentLabel = new JLabel("内容:");
        contentLabel.setPreferredSize(labelDim);
        contentLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(contentLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        contentPanel.setOpaque(false);
        fullRadio = new JRadioButton("全量", true);
        structRadio = new JRadioButton("仅结构");
        dataRadio = new JRadioButton("仅数据");
        ButtonGroup contentGroup = new ButtonGroup();
        contentGroup.add(fullRadio);
        contentGroup.add(structRadio);
        contentGroup.add(dataRadio);
        contentPanel.add(fullRadio);
        contentPanel.add(structRadio);
        contentPanel.add(dataRadio);
        panel.add(contentPanel, gbc);
        row++;

        // 格式
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        JLabel formatLabel = new JLabel("格式:");
        formatLabel.setPreferredSize(labelDim);
        formatLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(formatLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3; gbc.gridwidth = 1;
        formatCombo = new JComboBox<>(new String[]{"自定义 (-F c)", "纯文本 (-F p)"});
        formatCombo.setPreferredSize(fieldDim);
        panel.add(formatCombo, gbc);
        row++;

        // 模式名
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        JLabel schemaLabel = new JLabel("模式名:");
        schemaLabel.setPreferredSize(labelDim);
        schemaLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(schemaLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        schemaField = new JTextField();
        schemaField.setPreferredSize(new Dimension(220, 25));
        schemaField.setEnabled(false);
        panel.add(schemaField, gbc);
        row++;

        // 表名列表（带提示）
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        JLabel tableLabel = new JLabel("表名列表:");
        tableLabel.setPreferredSize(labelDim);
        tableLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(tableLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        JPanel tablePanel = new JPanel(new BorderLayout(0, 3));
        tablePanel.setOpaque(false);
        JLabel tableHint = new JLabel("多个表名用逗号分隔");
        tableHint.setFont(ThemeUtils.FONT_SMALL_BOLD);
        tableHint.setForeground(ThemeUtils.COLOR_TEXT_HINT);
        tablePanel.add(tableHint, BorderLayout.NORTH);

        tableListArea = new JTextArea(3, 20);
        tableListArea.setLineWrap(true);
        tableListArea.setEnabled(false);
        JScrollPane tableScroll = new JScrollPane(tableListArea);
        tableScroll.setPreferredSize(new Dimension(0, 90));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        panel.add(tablePanel, gbc);
        row++;

        // 表列表来源
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        JLabel sourceLabel = new JLabel("表列表来源:");
        sourceLabel.setPreferredSize(labelDim);
        sourceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(sourceLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5; gbc.gridwidth = 2;
        useTableListCheck = new JCheckBox("从 gk_sjdb.gk_gsdump_tablelist 读取");
        useTableListCheck.setEnabled(false);
        panel.add(useTableListCheck, gbc);
        row++;

        // GaussDB 数据源（动态显示）
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        dataSourceLabel = new JLabel("GaussDB 数据源:");
        dataSourceLabel.setPreferredSize(labelDim);
        dataSourceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dataSourceLabel.setVisible(false);
        panel.add(dataSourceLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.5; gbc.gridwidth = 2;
        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setPreferredSize(new Dimension(220, 25));
        dataSourceCombo.setVisible(false);
        refreshDataSourceCombo();
        panel.add(dataSourceCombo, gbc);

        // 监听粒度变化，控制启用状态
        ActionListener grainListener = e -> {
            boolean table = tableRadio.isSelected();
            boolean useList = table && useTableListCheck.isSelected();
            schemaField.setEnabled(schemaRadio.isSelected());
            useTableListCheck.setEnabled(table);
            tableListArea.setEnabled(table && !useList);
            dataSourceLabel.setVisible(table && useList);
            dataSourceCombo.setVisible(table && useList);
            if (useList) tableListArea.setText("");
        };
        dbRadio.addActionListener(grainListener);
        schemaRadio.addActionListener(grainListener);
        tableRadio.addActionListener(grainListener);
        useTableListCheck.addActionListener(e -> {
            boolean selected = useTableListCheck.isSelected();
            boolean table = tableRadio.isSelected();
            tableListArea.setEnabled(table && !selected);
            dataSourceLabel.setVisible(table && selected);
            dataSourceCombo.setVisible(table && selected);
            if (selected) tableListArea.setText("");
        });
        grainListener.actionPerformed(null);

        return panel;
    }

    private void refreshDataSourceCombo() {
        dataSourceCombo.removeAllItems();
        List<DataSource> gaussSources = DataSourceStore.load().stream()
                .filter(ds -> "GAUSSDB".equalsIgnoreCase(ds.getType()))
                .toList();
        if (gaussSources.isEmpty()) {
            dataSourceCombo.addItem(null);
            dataSourceCombo.setEnabled(false);
        } else {
            for (DataSource ds : gaussSources) dataSourceCombo.addItem(ds);
            dataSourceCombo.setSelectedIndex(0);
            dataSourceCombo.setEnabled(true);
        }
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ThemeUtils.COLOR_BG);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        btnPanel.setOpaque(false);
        executeBtn = new JButton("▶ 执行备份");
        executeBtn.setBackground(ThemeUtils.COLOR_PRIMARY);
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFocusPainted(false);
        executeBtn.setBorderPainted(false);
        executeBtn.setPreferredSize(new Dimension(120, 34));
        executeBtn.addActionListener(e -> executeBackup());
        btnPanel.add(executeBtn);

        stopBtn = new JButton("⏹ 终止");
        stopBtn.setBackground(ThemeUtils.COLOR_DANGER);
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setFocusPainted(false);
        stopBtn.setBorderPainted(false);
        stopBtn.setPreferredSize(new Dimension(100, 34));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopBackup());
        btnPanel.add(stopBtn);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(150, 20));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        btnPanel.add(progressBar);

        clearLogBtn = new JButton("🧹 清空日志");
        clearLogBtn.setBackground(new Color(180, 190, 200));
        clearLogBtn.setForeground(Color.WHITE);
        clearLogBtn.setFocusPainted(false);
        clearLogBtn.setBorderPainted(false);
        clearLogBtn.setPreferredSize(new Dimension(100, 34));
        clearLogBtn.addActionListener(e -> logArea.setText(""));
        btnPanel.add(clearLogBtn);

        closeBtn = new JButton("✕ 关闭");
        closeBtn.setBackground(new Color(160, 170, 185));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setPreferredSize(new Dimension(90, 34));
        closeBtn.addActionListener(e -> dispose());
        btnPanel.add(closeBtn);

        logArea = new JTextArea(10, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(26, 26, 30));
        logArea.setForeground(new Color(204, 221, 238));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "执行日志",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        panel.add(logScroll, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.NORTH);
        return panel;
    }

    private void createNewProfile() {
        SshProfile newProf = new SshProfile();
        newProf.setName("新配置");
        profiles.add(newProf);
        profileListModel.addElement(newProf.getName());
        int idx = profiles.size() - 1;
        profileList.setSelectedIndex(idx);
        loadProfile(newProf);
    }

    private void deleteProfile() {
        int idx = profileList.getSelectedIndex();
        if (idx < 0) return;
        if (JOptionPane.showConfirmDialog(this, "确定删除配置 \"" + profiles.get(idx).getName() + "\" ?",
                "确认删除", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        profiles.remove(idx);
        profileListModel.remove(idx);
        if (profileListModel.isEmpty()) {
            clearForm();
        } else {
            int newIdx = Math.min(idx, profileListModel.size() - 1);
            profileList.setSelectedIndex(newIdx);
            loadProfile(profiles.get(newIdx));
        }
        saveAllProfiles();
    }

    private void saveCurrentProfile() {
        int idx = profileList.getSelectedIndex();
        if (idx < 0) return;
        SshProfile prof = profiles.get(idx);
        updateProfileFromForm(prof);
        String oldName = profileListModel.get(idx);
        if (!oldName.equals(prof.getName())) {
            profileListModel.set(idx, prof.getName());
        }
        saveAllProfiles();
        JOptionPane.showMessageDialog(this, "配置已保存！", "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveAllProfiles() {
        SshProfileStore.save(profiles);
    }

    private void loadProfile(SshProfile prof) {
        currentProfile = prof;
        nameField.setText(prof.getName());
        sshHostField.setText(prof.getSshHost());
        sshPortField.setText(String.valueOf(prof.getSshPort()));
        sshUserField.setText(prof.getSshUser());
        sshPasswordField.setText(prof.getSshPassword());
        execUserField.setText(prof.getExecUser());
        dbHostField.setText(prof.getDbHost());
        dbPortField.setText(String.valueOf(prof.getDbPort()));
        dbNameField.setText(prof.getDbName());
        dbUserField.setText(prof.getDbUser());
        dbPasswordField.setText(prof.getDbPassword());
        backupDirField.setText(prof.getBackupDir());
    }

    private void updateProfileFromForm(SshProfile prof) {
        prof.setName(nameField.getText().trim());
        prof.setSshHost(sshHostField.getText().trim());
        try { prof.setSshPort(Integer.parseInt(sshPortField.getText().trim())); } catch (NumberFormatException e) {}
        prof.setSshUser(sshUserField.getText().trim());
        prof.setSshPassword(new String(sshPasswordField.getPassword()).trim());
        prof.setExecUser(execUserField.getText().trim());
        prof.setDbHost(dbHostField.getText().trim());
        try { prof.setDbPort(Integer.parseInt(dbPortField.getText().trim())); } catch (NumberFormatException e) {}
        prof.setDbName(dbNameField.getText().trim());
        prof.setDbUser(dbUserField.getText().trim());
        prof.setDbPassword(new String(dbPasswordField.getPassword()).trim());
        prof.setBackupDir(backupDirField.getText().trim());
    }

    private void clearForm() {
        nameField.setText("");
        sshHostField.setText("");
        sshPortField.setText("");
        sshUserField.setText("");
        sshPasswordField.setText("");
        execUserField.setText("");
        dbHostField.setText("");
        dbPortField.setText("");
        dbNameField.setText("");
        dbUserField.setText("");
        dbPasswordField.setText("");
        backupDirField.setText("");
        currentProfile = null;
    }

    private String escapeForShell(String raw) {
        if (raw == null) return "''";
        return "'" + raw.replace("'", "'\\''") + "'";
    }

    private void executeBackup() {
        int idx = profileList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "请先选择或新建一个配置", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateProfileFromForm(currentProfile);

        String sshHost = currentProfile.getSshHost();
        String sshUser = currentProfile.getSshUser();
        String sshPassword = currentProfile.getSshPassword();
        String dbName = currentProfile.getDbName();
        String backupDir = currentProfile.getBackupDir();

        if (sshHost.isEmpty() || sshUser.isEmpty() || sshPassword.isEmpty() || dbName.isEmpty() || backupDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写完整的配置（SSH主机、用户、密码、数据库名、备份目录）", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String grain = dbRadio.isSelected() ? "db" : schemaRadio.isSelected() ? "schema" : "table";
        String content = fullRadio.isSelected() ? "full" : structRadio.isSelected() ? "struct" : "data";
        String format = formatCombo.getSelectedIndex() == 0 ? "-F c" : "-F p";

        if ("schema".equals(grain) && schemaField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入模式名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("table".equals(grain) && !useTableListCheck.isSelected() && tableListArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入至少一个表名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("table".equals(grain) && useTableListCheck.isSelected() && dataSourceCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "请选择 GaussDB 数据源", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String dbHost = currentProfile.getDbHost().isEmpty() ? currentProfile.getSshHost() : currentProfile.getDbHost();
        int dbPort = currentProfile.getDbPort() > 0 ? currentProfile.getDbPort() : 8000;
        String dbUser = currentProfile.getDbUser().isEmpty() ? currentProfile.getDbName() : currentProfile.getDbUser();
        String dbPassword = currentProfile.getDbPassword();

        String normalizedBackupDir = backupDir.replaceAll("/+$", "");
        if (normalizedBackupDir.isEmpty()) normalizedBackupDir = "/data/dump";

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String namePart = "schema".equals(grain) ? schemaField.getText().trim() : dbName;
        String ext = format.contains("c") ? "dmp" : "sql";
        final String dumpFile = normalizedBackupDir + "/backup_" + namePart + "_" + timestamp + "." + ext;
        final String logFile  = normalizedBackupDir + "/backup_" + namePart + "_" + timestamp + ".log";

        final String escapedDbPassword = escapeForShell(dbPassword);

        setUIEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        stopBtn.setEnabled(true);

        final String finalGrain = grain;
        final String finalContent = content;
        final String finalFormat = format;
        final String finalDbHost = dbHost;
        final int finalDbPort = dbPort;
        final String finalDbUser = dbUser;
        final String finalDbName = dbName;
        final DataSource selectedDs = useTableListCheck.isSelected() ? (DataSource) dataSourceCombo.getSelectedItem() : null;

        currentWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    SshClient client = SshClient.setUpDefaultClient();
                    client.start();
                    ConnectFuture cf = client.connect(sshUser, sshHost, currentProfile.getSshPort());
                    ClientSession sess = cf.verify(30000).getSession();
                    sess.addPasswordIdentity(sshPassword);
                    sess.auth().verify(30000);
                    session = sess;

                    String execCmd;
                    String passArg = "-W " + escapedDbPassword;
                    // 使用 tee 同时输出到屏幕（捕获）和远程日志文件
                    String teeLog = " 2>&1 | tee -a " + logFile;

                    if ("table".equals(finalGrain) && useTableListCheck.isSelected()) {
                        List<String> tables = queryTableList(selectedDs);
                        if (tables.isEmpty()) {
                            publish("⚠️ 表列表为空，无法执行备份");
                            return null;
                        }
                        File localFile = File.createTempFile("tablelist_", ".txt");
                        try (PrintWriter writer = new PrintWriter(localFile, "UTF-8")) {
                            for (String t : tables) writer.println(t);
                        }
                        String remotePath = "/tmp/tablelist_" + System.currentTimeMillis() + ".txt";
                        uploadFile(localFile, remotePath);

                        execCmd = "gs_dump -h " + finalDbHost + " -p " + finalDbPort +
                                " -U " + finalDbUser + " " + passArg + " " + finalDbName + " " +
                                (finalContent.equals("struct") ? "-s " : finalContent.equals("data") ? "-a " : "") +
                                finalFormat + " " +
                                "--include-table-file=" + remotePath + " " +
                                "-f " + dumpFile + teeLog;
                    } else if ("table".equals(finalGrain)) {
                        StringBuilder cmd = new StringBuilder();
                        cmd.append("gs_dump -h ").append(finalDbHost).append(" -p ").append(finalDbPort)
                                .append(" -U ").append(finalDbUser).append(" ").append(passArg).append(" ")
                                .append(finalDbName).append(" ");
                        if ("struct".equals(finalContent)) cmd.append("-s ");
                        else if ("data".equals(finalContent)) cmd.append("-a ");
                        cmd.append(finalFormat).append(" ");
                        String[] tables = tableListArea.getText().trim().split("[\\s,;\\n]+");
                        for (String t : tables) {
                            if (!t.trim().isEmpty()) cmd.append("-t ").append(t.trim()).append(" ");
                        }
                        cmd.append("-f ").append(dumpFile).append(teeLog);
                        execCmd = cmd.toString();
                    } else {
                        StringBuilder cmd = new StringBuilder();
                        cmd.append("gs_dump -h ").append(finalDbHost).append(" -p ").append(finalDbPort)
                                .append(" -U ").append(finalDbUser).append(" ").append(passArg).append(" ")
                                .append(finalDbName).append(" ");
                        if ("schema".equals(finalGrain)) {
                            cmd.append("-n ").append(schemaField.getText().trim()).append(" ");
                        }
                        if ("struct".equals(finalContent)) cmd.append("-s ");
                        else if ("data".equals(finalContent)) cmd.append("-a ");
                        cmd.append(finalFormat).append(" ");
                        cmd.append("-f ").append(dumpFile).append(teeLog);
                        execCmd = cmd.toString();
                    }

                    String fullSuCommand = "su - " + currentProfile.getExecUser() + " -c \"" + execCmd.replace("\"", "\\\"") + "\"";
                    publish("--------------------------------------------------");
                    publish("🔧 实际通过 SSH 执行的完整命令: " + fullSuCommand);
                    publish("--------------------------------------------------");
                    executeCommand(session, execCmd);

                } catch (Exception e) {
                    publish("❌ 错误: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (session != null && !session.isClosed()) {
                        try { session.close(); } catch (IOException e) { publish("⚠️ 关闭 SSH 会话时出错: " + e.getMessage()); }
                    }
                }
                return null;
            }

            private List<String> queryTableList(DataSource ds) throws SQLException, ClassNotFoundException {
                List<String> tables = new ArrayList<>();
                if ("ORACLE".equalsIgnoreCase(ds.getType())) {
                    throw new SQLException("仅支持 GaussDB 数据源");
                }
                try { Class.forName("com.huawei.gaussdb.jdbc.Driver"); }
                catch (ClassNotFoundException e1) {
                    try { Class.forName("org.postgresql.Driver"); }
                    catch (ClassNotFoundException e2) { throw new ClassNotFoundException("GaussDB JDBC 驱动未找到"); }
                }
                String url = ds.buildUrl();
                String user = ds.getUser();
                String password = ds.getPassword();
                try (Connection conn = DriverManager.getConnection(url, user, password);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT schema_name||'.'||table_name FROM gk_sjdb.gk_gsdump_tablelist")) {
                    while (rs.next()) tables.add(rs.getString(1));
                }
                return tables;
            }

            private void uploadFile(File localFile, String remotePath) {
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session);
                     FileInputStream fis = new FileInputStream(localFile)) {
                    sftp.put(fis, remotePath);
                } catch (Exception e) {
                    publish("⚠️ 上传表列表文件失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void executeCommand(ClientSession sess, String command) throws Exception {
                String fullCmd = "su - " + currentProfile.getExecUser() + " -c \"" + command.replace("\"", "\\\"") + "\"";
                try (ChannelExec execChannel = sess.createExecChannel(fullCmd)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ByteArrayOutputStream err = new ByteArrayOutputStream();
                    execChannel.setOut(out);
                    execChannel.setErr(err);
                    execChannel.open().verify(10000);
                    execChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));
                    String output = out.toString(StandardCharsets.UTF_8);
                    String error = err.toString(StandardCharsets.UTF_8);
                    if (!output.isEmpty()) publish(output);
                    if (!error.isEmpty()) publish("[ERR] " + error);
                    int exit = execChannel.getExitStatus();
                    if (exit != 0) publish("⚠️ 命令退出码: " + exit);
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) log(msg);
            }

            @Override
            protected void done() {
                setUIEnabled(true);
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                stopBtn.setEnabled(false);
                currentWorker = null;
                if (session != null && !session.isClosed()) {
                    try { session.close(); } catch (IOException e) { log("⚠️ 关闭 SSH 会话时出错: " + e.getMessage()); }
                }
            }
        };
        currentWorker.execute();
    }

    private void stopBackup() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            if (session != null && !session.isClosed()) {
                try { session.close(); } catch (IOException e) { log("⚠️ 关闭 SSH 会话时出错: " + e.getMessage()); }
            }
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setUIEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            executeBtn.setEnabled(enabled);
            stopBtn.setEnabled(!enabled);
            saveProfileBtn.setEnabled(enabled);
            newProfileBtn.setEnabled(enabled);
            deleteProfileBtn.setEnabled(enabled);
            Component[] comps = {nameField, sshHostField, sshPortField, sshUserField, sshPasswordField,
                    execUserField, dbHostField, dbPortField, dbNameField, dbUserField, dbPasswordField, backupDirField,
                    dbRadio, schemaRadio, tableRadio, fullRadio, structRadio, dataRadio, formatCombo,
                    schemaField, tableListArea, useTableListCheck, dataSourceCombo};
            for (Component c : comps) c.setEnabled(enabled);
        });
    }

    @Override
    public void refresh() {
        profiles = SshProfileStore.load();
        profileListModel.clear();
        for (SshProfile p : profiles) profileListModel.addElement(p.getName());
        if (!profiles.isEmpty()) {
            profileList.setSelectedIndex(0);
            loadProfile(profiles.get(0));
        }
        refreshDataSourceCombo();
    }
}