package com.sunzh.comparison;

import com.sunzh.comparison.panels.*;
import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.ui.components.WidgetFactory;
import com.sunzh.utils.ThemeUtils;
import com.sunzh.utils.SvgIconUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class ComparisonDialog extends BaseDialog {
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
        topPanel.add(WidgetFactory.iconLabelWithText("database", "数据源:", 14, ThemeUtils.COLOR_TEXT));
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

        setLocationRelativeTo(owner);
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
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void refresh() {
        loadDataSources();
        // 刷新所有面板（可选）
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof ExtractPanel) ((ExtractPanel) comp).refreshData();
            else if (comp instanceof TaskConfigPanel) ((TaskConfigPanel) comp).refreshData();
            else if (comp instanceof ComparePanel) ((ComparePanel) comp).refreshData();
            else if (comp instanceof DetailPanel) ((DetailPanel) comp).refreshData();
        }
    }
}