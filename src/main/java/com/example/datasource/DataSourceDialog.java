package com.example.datasource;

import com.example.core.ConnectionManager;
import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.ui.BaseDialog;
import com.example.ui.components.CustomButton;
import com.example.utils.ThemeUtils;
import com.example.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * 数据源配置对话框
 * 浅灰 + 墨绿主题 - 按钮风格统一版
 */
public class DataSourceDialog extends BaseDialog {
    private DefaultListModel<DataSource> listModel;
    private JList<DataSource> dataSourceList;
    private JButton btnSave, btnDelete, btnTest;

    private JTextField tfName, tfHost, tfPort;
    private JTextField tfServiceName;
    private JTextField tfDatabase;
    private JTextField tfSchema;
    private JTextField tfUser;
    private JPasswordField tfPassword;
    private JComboBox<String> cbType;

    private JLabel lblService, lblDatabase, lblSchema;

    private static final int LABEL_WIDTH = 80;
    private static final int FIELD_WIDTH = 280;
    private static final int FIELD_HEIGHT = 30;

    public DataSourceDialog(JFrame owner) {
        super(owner, "数据源配置", "settings");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        setBackground(ThemeUtils.COLOR_BG);
        mainContentPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel mainPanel = new JPanel(new BorderLayout(12, 0));
        mainPanel.setBackground(ThemeUtils.COLOR_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder());

        JPanel leftPanel = createListPanel();
        JPanel rightPanel = createFormPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.28);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setBackground(ThemeUtils.COLOR_BG);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainContentPanel.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainContentPanel.add(bottomPanel, BorderLayout.SOUTH);

        loadData();
    }

    // ============================================================
    // 左侧：数据源列表
    // ============================================================
    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "数据源列表",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        listModel = new DefaultListModel<>();
        dataSourceList = new JList<>(listModel);
        dataSourceList.setCellRenderer(new DataSourceListRenderer());
        dataSourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataSourceList.setBackground(ThemeUtils.COLOR_BG_CARD);
        dataSourceList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        dataSourceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                DataSource selected = dataSourceList.getSelectedValue();
                if (selected != null) {
                    displayDataSource(selected);
                } else {
                    clearForm();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(dataSourceList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(listScroll, BorderLayout.CENTER);

        JButton btnRefresh = new CustomButton("刷新列表", ThemeUtils.COLOR_PRIMARY);
        btnRefresh.setIcon(SvgIconUtils.getWhite("refresh", 16));
        btnRefresh.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnRefresh.setPreferredSize(new Dimension(110, 32));
        btnRefresh.addActionListener(e -> loadData());

        JPanel btnBottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        btnBottom.setOpaque(false);
        btnBottom.add(btnRefresh);
        panel.add(btnBottom, BorderLayout.SOUTH);

        return panel;
    }

    // ---- 自定义列表渲染器 ----
    private class DataSourceListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            if (value instanceof DataSource) {
                DataSource ds = (DataSource) value;
                String icon = "ORACLE".equals(ds.getType()) ? "🔶" : "🔷";
                Color iconColor = "ORACLE".equals(ds.getType()) ?
                        new Color(200, 120, 30) : ThemeUtils.COLOR_PRIMARY;

                JLabel iconLabel = new JLabel(icon);
                iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                iconLabel.setForeground(iconColor);

                JPanel textPanel = new JPanel(new BorderLayout());
                textPanel.setOpaque(false);
                JLabel nameLabel = new JLabel(ds.getName());
                nameLabel.setFont(ThemeUtils.FONT_NORMAL);
                nameLabel.setForeground(ThemeUtils.COLOR_TEXT);

                String info = ds.getHost() + ":" + ds.getPort();
                if ("ORACLE".equals(ds.getType()) && ds.getServiceName() != null) {
                    info += " / " + ds.getServiceName();
                } else if (ds.getDatabase() != null) {
                    info += " / " + ds.getDatabase();
                }
                JLabel infoLabel = new JLabel(info);
                infoLabel.setFont(ThemeUtils.FONT_SMALL);
                infoLabel.setForeground(ThemeUtils.COLOR_TEXT_SECONDARY);

                textPanel.add(nameLabel, BorderLayout.NORTH);
                textPanel.add(infoLabel, BorderLayout.SOUTH);

                panel.add(iconLabel, BorderLayout.WEST);
                panel.add(textPanel, BorderLayout.CENTER);

                if (isSelected) {
                    panel.setBackground(ThemeUtils.COLOR_PRIMARY);
                    nameLabel.setForeground(Color.WHITE);
                    infoLabel.setForeground(new Color(220, 235, 210));
                    iconLabel.setForeground(Color.WHITE);
                } else {
                    panel.setBackground(index % 2 == 0 ? ThemeUtils.COLOR_BG_ALTERNATE : Color.WHITE);
                }
            }
            panel.setOpaque(true);
            return panel;
        }
    }

    // ============================================================
    // 右侧：编辑表单
    // ============================================================
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeUtils.COLOR_BG_CARD);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER),
                "✏️ 编辑数据源",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                ThemeUtils.COLOR_PRIMARY
        ));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // ---- 名称 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblName = new JLabel("名称 *");
        lblName.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblName.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblName.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfName = new JTextField();
        tfName.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfName.setFont(ThemeUtils.FONT_NORMAL);
        tfName.setBorder(createInputBorder());
        formPanel.add(tfName, gbc);
        row++;

        // ---- 类型 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblType = new JLabel("类型 *");
        lblType.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblType.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblType.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        cbType = new JComboBox<>(new String[]{"ORACLE", "GAUSSDB"});
        // ★★★ 修改：将宽度改为 FIELD_WIDTH，与文本框一致 ★★★
        cbType.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        cbType.setFont(ThemeUtils.FONT_NORMAL);
        cbType.setBackground(ThemeUtils.COLOR_BG_INPUT);
        cbType.addActionListener(e -> toggleTypeFields());
        formPanel.add(cbType, gbc);
        row++;

        // ---- 主机 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblHost = new JLabel("主机 *");
        lblHost.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblHost.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblHost.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblHost, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfHost = new JTextField();
        tfHost.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfHost.setFont(ThemeUtils.FONT_NORMAL);
        tfHost.setBorder(createInputBorder());
        formPanel.add(tfHost, gbc);
        row++;

        // ---- 端口 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblPort = new JLabel("端口 *");
        lblPort.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblPort.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblPort.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblPort, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfPort = new JTextField();
        // 端口可以保持稍窄，但为了对齐，也设为 FIELD_WIDTH
        tfPort.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfPort.setFont(ThemeUtils.FONT_NORMAL);
        tfPort.setBorder(createInputBorder());
        formPanel.add(tfPort, gbc);
        row++;

        // ---- 服务名 (Oracle) ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        lblService = new JLabel("服务名 *");
        lblService.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblService.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblService.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblService, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfServiceName = new JTextField();
        tfServiceName.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfServiceName.setFont(ThemeUtils.FONT_NORMAL);
        tfServiceName.setBorder(createInputBorder());
        formPanel.add(tfServiceName, gbc);
        row++;

        // ---- 数据库 (GaussDB) ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        lblDatabase = new JLabel("数据库 *");
        lblDatabase.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblDatabase.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblDatabase.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblDatabase, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfDatabase = new JTextField();
        tfDatabase.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfDatabase.setFont(ThemeUtils.FONT_NORMAL);
        tfDatabase.setBorder(createInputBorder());
        formPanel.add(tfDatabase, gbc);
        row++;

        // ---- 模式 (GaussDB) ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        lblSchema = new JLabel("模式");
        lblSchema.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblSchema.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblSchema.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblSchema, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfSchema = new JTextField();
        tfSchema.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfSchema.setFont(ThemeUtils.FONT_NORMAL);
        tfSchema.setBorder(createInputBorder());
        formPanel.add(tfSchema, gbc);
        row++;

        // ---- 用户名 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblUser = new JLabel("用户名 *");
        lblUser.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblUser.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblUser.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfUser = new JTextField();
        tfUser.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfUser.setFont(ThemeUtils.FONT_NORMAL);
        tfUser.setBorder(createInputBorder());
        formPanel.add(tfUser, gbc);
        row++;

        // ---- 密码 ----
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        JLabel lblPwd = new JLabel("密码");
        lblPwd.setPreferredSize(new Dimension(LABEL_WIDTH, FIELD_HEIGHT));
        lblPwd.setFont(ThemeUtils.FONT_SMALL_BOLD);
        lblPwd.setForeground(ThemeUtils.COLOR_TEXT);
        formPanel.add(lblPwd, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        tfPassword = new JPasswordField();
        tfPassword.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        tfPassword.setFont(ThemeUtils.FONT_NORMAL);
        tfPassword.setBorder(createInputBorder());
        formPanel.add(tfPassword, gbc);
        row++;

        // ---- 按钮（统一风格） ----
        JPanel btnPanel = createButtonPanel();
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 6, 0, 6);
        formPanel.add(btnPanel, gbc);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(ThemeUtils.COLOR_BG_CARD);

        panel.add(scrollPane, BorderLayout.CENTER);

        toggleTypeFields();
        return panel;
    }

    private CompoundBorder createInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        );
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setOpaque(false);

        Dimension btnSize = new Dimension(90, 34);

        JButton btnAdd = new CustomButton("新增", ThemeUtils.COLOR_PRIMARY);
        btnAdd.setIcon(SvgIconUtils.getWhite("plus", 16));
        btnAdd.setPreferredSize(btnSize);
        btnAdd.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnAdd.addActionListener(e -> {
            dataSourceList.clearSelection();
            clearForm();
        });

        JButton btnCopy = new CustomButton("复制", ThemeUtils.COLOR_PRIMARY);
        btnCopy.setIcon(SvgIconUtils.getWhite("copy", 16));
        btnCopy.setPreferredSize(btnSize);
        btnCopy.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnCopy.addActionListener(e -> copyDataSource());

        btnSave = new CustomButton("保存", ThemeUtils.COLOR_PRIMARY);
        btnSave.setIcon(SvgIconUtils.getWhite("save", 16));
        btnSave.setPreferredSize(btnSize);
        btnSave.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnSave.addActionListener(e -> saveDataSource());

        btnDelete = new CustomButton("删除", ThemeUtils.COLOR_DANGER);
        btnDelete.setIcon(SvgIconUtils.getWhite("trash", 16));
        btnDelete.setPreferredSize(btnSize);
        btnDelete.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnDelete.addActionListener(e -> deleteSelected());

        btnTest = new CustomButton("测试连接", ThemeUtils.COLOR_PRIMARY);
        btnTest.setIcon(SvgIconUtils.getWhite("connection", 16));
        btnTest.setPreferredSize(new Dimension(110, 34));
        btnTest.setFont(ThemeUtils.FONT_SMALL_BOLD);
        btnTest.addActionListener(e -> testConnection());

        panel.add(btnAdd);
        panel.add(btnCopy);
        panel.add(btnSave);
        panel.add(btnDelete);
        panel.add(btnTest);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(ThemeUtils.COLOR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        JButton btnClose = new CustomButton("关闭", ThemeUtils.COLOR_SECONDARY);
        btnClose.setFont(ThemeUtils.FONT_BOLD);
        btnClose.setPreferredSize(new Dimension(80, 32));
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);

        return panel;
    }

    private void toggleTypeFields() {
        String type = (String) cbType.getSelectedItem();
        boolean isOracle = "ORACLE".equals(type);

        lblService.setVisible(isOracle);
        tfServiceName.setVisible(isOracle);

        lblDatabase.setVisible(!isOracle);
        tfDatabase.setVisible(!isOracle);
        lblSchema.setVisible(!isOracle);
        tfSchema.setVisible(!isOracle);

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    // ============================================================
    // 业务逻辑（保持不变）
    // ============================================================

    public void loadData() {
        listModel.clear();
        List<DataSource> list = DataSourceStore.load();
        for (DataSource ds : list) {
            listModel.addElement(ds);
        }
        if (!listModel.isEmpty()) {
            dataSourceList.setSelectedIndex(0);
        } else {
            clearForm();
        }
    }

    private void displayDataSource(DataSource ds) {
        tfName.setText(ds.getName());
        cbType.setSelectedItem(ds.getType());
        tfHost.setText(ds.getHost());
        tfPort.setText(String.valueOf(ds.getPort()));
        tfUser.setText(ds.getUser());
        tfPassword.setText(ds.getPassword());

        if ("ORACLE".equals(ds.getType())) {
            tfServiceName.setText(ds.getServiceName());
            tfDatabase.setText("");
            tfSchema.setText("");
        } else {
            tfServiceName.setText("");
            tfDatabase.setText(ds.getDatabase());
            tfSchema.setText(ds.getSchema());
        }
        toggleTypeFields();
    }

    private void clearForm() {
        tfName.setText("");
        cbType.setSelectedIndex(0);
        tfHost.setText("");
        tfPort.setText("");
        tfServiceName.setText("");
        tfDatabase.setText("");
        tfSchema.setText("");
        tfUser.setText("");
        tfPassword.setText("");
        toggleTypeFields();
    }

    private void copyDataSource() {
        DataSource selected = dataSourceList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择要复制的数据源");
            return;
        }

        String oldName = selected.getName();
        String newName = oldName + "_copy";

        int copyNum = 1;
        String baseName = newName;
        while (true) {
            boolean exists = false;
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getName().equalsIgnoreCase(newName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) break;
            newName = baseName + "_" + (copyNum++);
        }

        DataSource copy;
        if ("ORACLE".equals(selected.getType())) {
            copy = new DataSource(newName, selected.getType(),
                    selected.getHost(), selected.getPort(),
                    selected.getServiceName(),
                    selected.getUser(), selected.getPassword());
        } else {
            copy = new DataSource(newName, selected.getType(),
                    selected.getHost(), selected.getPort(),
                    selected.getDatabase(), selected.getSchema(),
                    selected.getUser(), selected.getPassword());
        }

        dataSourceList.clearSelection();
        displayDataSource(copy);
    }

    private void saveDataSource() {
        String name = tfName.getText().trim();
        String type = (String) cbType.getSelectedItem();
        String host = tfHost.getText().trim();
        String portStr = tfPort.getText().trim();
        String user = tfUser.getText().trim();
        String pwd = new String(tfPassword.getPassword());

        if (name.isEmpty() || host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写完整信息（名称、主机、端口、用户名）");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "端口必须是数字");
            return;
        }

        DataSource selected = dataSourceList.getSelectedValue();

        if ("ORACLE".equals(type)) {
            String serviceName = tfServiceName.getText().trim();
            if (serviceName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Oracle 请填写服务名");
                return;
            }
            if (selected != null) {
                selected.setName(name);
                selected.setType(type);
                selected.setHost(host);
                selected.setPort(port);
                selected.setServiceName(serviceName);
                selected.setUser(user);
                selected.setPassword(pwd);
                listModel.setElementAt(selected, dataSourceList.getSelectedIndex());
            } else {
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getName().equalsIgnoreCase(name)) {
                        JOptionPane.showMessageDialog(this, "名称已存在，请更换");
                        return;
                    }
                }
                DataSource newDs = new DataSource(name, type, host, port, serviceName, user, pwd);
                listModel.addElement(newDs);
                dataSourceList.setSelectedIndex(listModel.size() - 1);
            }
        } else {
            String database = tfDatabase.getText().trim();
            if (database.isEmpty()) {
                JOptionPane.showMessageDialog(this, "GaussDB 请填写数据库名");
                return;
            }
            String schema = tfSchema.getText().trim();
            if (selected != null) {
                selected.setName(name);
                selected.setType(type);
                selected.setHost(host);
                selected.setPort(port);
                selected.setDatabase(database);
                selected.setSchema(schema);
                selected.setUser(user);
                selected.setPassword(pwd);
                listModel.setElementAt(selected, dataSourceList.getSelectedIndex());
            } else {
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getName().equalsIgnoreCase(name)) {
                        JOptionPane.showMessageDialog(this, "名称已存在，请更换");
                        return;
                    }
                }
                DataSource newDs = new DataSource(name, type, host, port, database, schema, user, pwd);
                listModel.addElement(newDs);
                dataSourceList.setSelectedIndex(listModel.size() - 1);
            }
        }

        List<DataSource> all = Collections.list(listModel.elements());
        DataSourceStore.save(all);

        JOptionPane.showMessageDialog(this, "✅ 保存成功！");
    }

    private void deleteSelected() {
        int idx = dataSourceList.getSelectedIndex();
        if (idx == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的数据源");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确定要删除选中的数据源吗？", "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            listModel.remove(idx);
            List<DataSource> all = Collections.list(listModel.elements());
            DataSourceStore.save(all);
            dataSourceList.clearSelection();
            clearForm();
        }
    }

    private void testConnection() {
        String name = tfName.getText().trim();
        String type = (String) cbType.getSelectedItem();
        String host = tfHost.getText().trim();
        String portStr = tfPort.getText().trim();
        String user = tfUser.getText().trim();
        String pwd = new String(tfPassword.getPassword());

        if (host.isEmpty() || portStr.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写主机、端口、用户名");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "端口必须是数字");
            return;
        }

        DataSource temp;
        if ("ORACLE".equals(type)) {
            String serviceName = tfServiceName.getText().trim();
            if (serviceName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Oracle 请填写服务名");
                return;
            }
            temp = new DataSource(name, type, host, port, serviceName, user, pwd);
        } else {
            String database = tfDatabase.getText().trim();
            if (database.isEmpty()) {
                JOptionPane.showMessageDialog(this, "GaussDB 请填写数据库名");
                return;
            }
            String schema = tfSchema.getText().trim();
            temp = new DataSource(name, type, host, port, database, schema, user, pwd);
        }

        boolean ok = ConnectionManager.testConnection(temp);
        JOptionPane.showMessageDialog(this, ok ? "✅ 连接成功！" : "❌ 连接失败，请检查参数和网络");
    }

    @Override
    public void refresh() {
        loadData();
    }
}