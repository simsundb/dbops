package com.sunzh.comparison;

import com.sunzh.comparison.panels.*;
import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.utils.ThemeUtils;
import com.sunzh.utils.SvgIconUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class ComparisonDialog extends BaseDialog {
    /** 数据库连接超时：Oracle 毫秒，GaussDB/Postgres 秒 */
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private JComboBox<DataSource> dataSourceCombo;
    private JTabbedPane tabbedPane;
    private ExtractPanel sourceExtractPanel;
    private ExtractPanel targetExtractPanel;
    private TaskConfigPanel taskConfigPanel;
    private ComparePanel comparePanel;
    private DetailPanel detailPanel;

    public ComparisonDialog(JFrame owner) {
        super(owner, "结构对比工具", "compare");
    }

    private JLabel makeTab(String iconName, String text) {
        JLabel lbl = new JLabel(text, SvgIconUtils.get(iconName, 14), SwingConstants.LEADING);
        lbl.setIconTextGap(5);
        return lbl;
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout(10, 10));
        mainContentPanel.setBackground(ThemeUtils.COLOR_BG);

        // 顶部数据源选择
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(SvgIconUtils.label("database", "数据源:", 14, ThemeUtils.COLOR_TEXT));
        dataSourceCombo = new JComboBox<>();
        loadDataSources();
        topPanel.add(dataSourceCombo);
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        // Tab面板
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));

        sourceExtractPanel = new ExtractPanel(this, true);
        targetExtractPanel = new ExtractPanel(this, false);
        taskConfigPanel = new TaskConfigPanel(this);
        comparePanel = new ComparePanel(this);
        detailPanel = new DetailPanel(this);

        tabbedPane.addTab("  源端抽取", SvgIconUtils.get("download", 14), sourceExtractPanel);
        tabbedPane.addTab("  目标端抽取", SvgIconUtils.get("upload", 14), targetExtractPanel);
        tabbedPane.addTab("  任务与配置", SvgIconUtils.get("table", 14), taskConfigPanel);
        tabbedPane.addTab("  执行对比", SvgIconUtils.get("play", 14), comparePanel);
        tabbedPane.addTab("  对比明细", SvgIconUtils.get("list", 14), detailPanel);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            switch (idx) {
                case 0: sourceExtractPanel.refreshData(); break;
                case 1: targetExtractPanel.refreshData(); break;
                case 2: taskConfigPanel.refreshData(); break;
                case 3: comparePanel.refreshData(); break;
                case 4: detailPanel.refreshData(); break;
            }
        });

        mainContentPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadDataSources() {
        dataSourceCombo.removeAllItems();
        List<DataSource> sources = DataSourceStore.load();
        if (sources.isEmpty()) {
            dataSourceCombo.addItem(null);
            dataSourceCombo.setEnabled(false);
            return;
        }
        for (DataSource ds : sources) {
            dataSourceCombo.addItem(ds);
        }
        dataSourceCombo.setSelectedIndex(0);
        dataSourceCombo.setEnabled(true);
    }

    /**
     * 获取当前选中数据源的连接
     */
    public Connection getConnection() throws SQLException, ClassNotFoundException {
        DataSource ds = (DataSource) dataSourceCombo.getSelectedItem();
        if (ds == null) {
            throw new SQLException("未选择数据源");
        }
        // 加载驱动
        if ("ORACLE".equalsIgnoreCase(ds.getType())) {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } else if ("GAUSSDB".equalsIgnoreCase(ds.getType())) {
            Class.forName("com.huawei.gaussdb.jdbc.Driver");
        } else {
            throw new SQLException("不支持的数据库类型: " + ds.getType());
        }
        String url = ds.buildUrl();
        String user = ds.getUser();
        String password = ds.getPassword();
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        // 设置连接超时，避免数据库不可达时长时间阻塞界面
        if ("ORACLE".equalsIgnoreCase(ds.getType())) {
            props.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(CONNECT_TIMEOUT_MS));
            props.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        } else {
            props.setProperty("loginTimeout", String.valueOf(CONNECT_TIMEOUT_SECONDS));
            props.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_SECONDS));
        }
        try {
            return DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            // 包装为友好提示：包含数据源名称/主机端口，Swing 内可直接显示（不会像控制台那样中文乱码）
            throw new SQLException("无法连接数据源 [" + ds.getName() + "]（" + ds.getHost() + ":" + ds.getPort() + "）。\n请检查主机/端口/账号/密码配置及网络连通性。\n原因：" + e.getMessage(), e);
        }
    }

    /**
     * 打开对话框时调用（BaseDialog.setVisible -> refresh）。
     * 这里只重载数据源下拉框（读本地 JSON，很快），
     * 绝不连接数据库——连接只发生在用户选择数据源 / 切换页签 / 点击刷新之后。
     */
    @Override
    public void refresh() {
        loadDataSources();
    }
}