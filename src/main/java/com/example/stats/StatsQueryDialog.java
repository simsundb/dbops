package com.example.stats;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;
import com.example.ui.BaseDialog;
import com.example.utils.ThemeUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.GradientBarPainter;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.yaml.snakeyaml.Yaml;

import java.text.NumberFormat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.List;

public class StatsQueryDialog extends BaseDialog {

    // ---- 外部配置目录（JAR 同级 ./conf/stats/）----
    private static final String EXTERNAL_CONFIG_DIR = "./conf/stats/";

    // ---- 主题颜色 ----
    private static final Color BG = ThemeUtils.COLOR_BG;
    private static final Color CARD = Color.WHITE;
    private static final Color PRIMARY = ThemeUtils.COLOR_PRIMARY;
    private static final Color HEADER_BG = new Color(230, 234, 240);
    private static final Color TABLE_HEADER_BG = new Color(45, 62, 80);
    private static final Color TABLE_HEADER_FG = Color.WHITE;
    private static final Color BORDER = ThemeUtils.COLOR_BORDER;
    private static final Color TEXT = ThemeUtils.COLOR_TEXT;
    private static final Color TEXT_SEC = ThemeUtils.COLOR_TEXT_SECONDARY;

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_BTN = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);

    private static final int CARD_MAX_HEIGHT = 490;
    private static final int TABLE_PREFERRED_HEIGHT = 230;

    // ---- 图表调色板（用于饼图各扇区） ----
    private static final Color[] PIE_PALETTE = {
            new Color(56, 114, 196), new Color(230, 126, 34), new Color(46, 174, 125),
            new Color(199, 84, 80), new Color(142, 105, 199), new Color(64, 168, 184),
            new Color(212, 172, 13), new Color(120, 144, 156)
    };

    private enum ChartKind { BAR, LINE, PIE, AREA }

    // ---- UI 组件 ----
    private JComboBox<String> dataSourceCombo;
    private JButton refreshButton;
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private JButton exportAllButton;
    private JPanel tasksPanel;
    private JScrollPane tasksScroll;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel configPanel;

    // ---- 数据 ----
    private List<DataSource> dataSources;
    private List<StatsConfig> configs;
    private Map<StatsConfig, JCheckBox> checkBoxMap;
    private Map<StatsConfig, DefaultTableModel> tableModels = new LinkedHashMap<>();
    private Map<StatsConfig, List<Object[]>> rawData = new LinkedHashMap<>();
    private Map<StatsConfig, String[]> columnNames = new LinkedHashMap<>();
    private Map<StatsConfig, JTable> tableMap = new LinkedHashMap<>();

    public StatsQueryDialog(JFrame owner) {
        super(owner, "📊 统计数据查询");
    }

    @Override
    protected void initUI() {
        if (configs == null) configs = new ArrayList<>();
        if (checkBoxMap == null) checkBoxMap = new LinkedHashMap<>();

        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(BG);
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        loadDataSources();
        setSize(1440, 990);
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setOpaque(false);

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTop.setOpaque(false);

        JLabel dsLabel = new JLabel("数据源:");
        dsLabel.setFont(FONT_NORMAL);
        dsLabel.setForeground(TEXT);
        leftTop.add(dsLabel);

        dataSourceCombo = new JComboBox<>();
        dataSourceCombo.setFont(FONT_NORMAL);
        dataSourceCombo.setPreferredSize(new Dimension(220, 34));
        dataSourceCombo.setBackground(CARD);
        dataSourceCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        leftTop.add(dataSourceCombo);

        refreshButton = createStyledButton("🔄 刷新数据", PRIMARY);
        refreshButton.addActionListener(e -> loadStatsData());
        leftTop.add(refreshButton);

        exportAllButton = createStyledButton("📥 全部导出Excel", new Color(32, 157, 52));
        exportAllButton.addActionListener(e -> exportAllToExcel());
        exportAllButton.setEnabled(false);
        leftTop.add(exportAllButton);

        topPanel.add(leftTop, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTop.setOpaque(false);
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_SEC);
        rightTop.add(statusLabel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(153, 23));
        progressBar.setStringPainted(true);
        progressBar.setFont(FONT_SMALL);
        progressBar.setVisible(false);
        rightTop.add(progressBar);

        topPanel.add(rightTop, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);

        configPanel = new JPanel(new BorderLayout(0, 5));
        configPanel.setBackground(CARD);
        configPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel configTitle = new JLabel("📋 任务配置（勾选后强制执行）");
        configTitle.setFont(FONT_BOLD);
        configTitle.setForeground(TEXT);
        titleBar.add(configTitle, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        selectAllButton = new JButton("全选");
        selectAllButton.setFont(FONT_SMALL);
        selectAllButton.setBackground(new Color(238, 242, 245));
        selectAllButton.setForeground(TEXT);
        selectAllButton.setFocusPainted(false);
        selectAllButton.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        selectAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectAllButton.addActionListener(e -> setAllCheckBoxes(true));
        btnBar.add(selectAllButton);

        deselectAllButton = new JButton("取消全选");
        deselectAllButton.setFont(FONT_SMALL);
        deselectAllButton.setBackground(new Color(238, 242, 245));
        deselectAllButton.setForeground(TEXT);
        deselectAllButton.setFocusPainted(false);
        deselectAllButton.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        deselectAllButton.addActionListener(e -> setAllCheckBoxes(false));
        btnBar.add(deselectAllButton);

        titleBar.add(btnBar, BorderLayout.EAST);
        configPanel.add(titleBar, BorderLayout.NORTH);

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setOpaque(false);
        JScrollPane configScroll = new JScrollPane(checkboxPanel);
        configScroll.setBorder(BorderFactory.createEmptyBorder());
        configScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        configScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        configScroll.getViewport().setBackground(CARD);
        configPanel.add(configScroll, BorderLayout.CENTER);

        centerPanel.add(configPanel, BorderLayout.NORTH);

        tasksPanel = new JPanel();
        tasksPanel.setLayout(new BoxLayout(tasksPanel, BoxLayout.Y_AXIS));
        tasksPanel.setBackground(BG);

        tasksScroll = new JScrollPane(tasksPanel);
        tasksScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                "📊 统计结果",
                TitledBorder.LEFT, TitledBorder.TOP,
                ThemeUtils.FONT_SUBTITLE,
                TEXT
        ));
        tasksScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tasksScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tasksScroll.getViewport().setBackground(BG);

        centerPanel.add(tasksScroll, BorderLayout.CENTER);
        return centerPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton closeBtn = new JButton("关闭");
        closeBtn.setFont(FONT_NORMAL);
        closeBtn.setBackground(new Color(161, 173, 187));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setPreferredSize(new Dimension(100, 39));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        return bottomPanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(168, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
        return btn;
    }

    private void loadDataSources() {
        dataSources = DataSourceStore.load();
        dataSourceCombo.removeAllItems();
        if (dataSources.isEmpty()) {
            dataSourceCombo.addItem("请先配置数据源");
            dataSourceCombo.setEnabled(false);
        } else {
            for (DataSource ds : dataSources) dataSourceCombo.addItem(ds.getName());
            dataSourceCombo.setSelectedIndex(0);
            dataSourceCombo.setEnabled(true);
        }
    }

    private void loadConfig(JPanel checkboxPanel) {
        if (checkBoxMap == null) checkBoxMap = new LinkedHashMap<>();
        else checkBoxMap.clear();
        if (configs == null) configs = new ArrayList<>();
        else configs.clear();
        checkboxPanel.removeAll();

        InputStream is = null;
        try {
            File external = new File(EXTERNAL_CONFIG_DIR + "stats_config.yaml");
            if (external.exists() && external.isFile()) {
                is = new FileInputStream(external);
                statusLabel.setText("使用外部配置: " + external.getAbsolutePath());
            } else {
                is = getClass().getClassLoader().getResourceAsStream("stats_config.yaml");
                if (is != null) statusLabel.setText("使用内置配置 (classpath:/stats_config.yaml)");
            }

            if (is == null) {
                JLabel err = new JLabel("未找到 stats_config.yaml（外部和内置均无）");
                err.setForeground(Color.RED);
                checkboxPanel.add(err);
                return;
            }

            Yaml yaml = new Yaml();
            List<Map<String, Object>> rawList = yaml.load(is);
            if (rawList == null || rawList.isEmpty()) {
                checkboxPanel.add(new JLabel("配置为空"));
                return;
            }

            int total = rawList.size();
            final int COLS = 4;
            int rows = (int) Math.ceil((double) total / COLS);
            checkboxPanel.setLayout(new GridLayout(rows, COLS, 18, 12));

            for (Map<String, Object> item : rawList) {
                String desc = (String) item.get("description");
                Boolean enabled = item.get("enabled") != null && (Boolean) item.get("enabled");
                String sqlFile = (String) item.get("sqlFile");
                if (desc == null || sqlFile == null) continue;

                StatsConfig cfg = new StatsConfig();
                cfg.setDescription(desc);
                cfg.setEnabled(enabled);
                cfg.setSqlFile(sqlFile);
                configs.add(cfg);

                JCheckBox cb = new JCheckBox(desc);
                cb.setFont(FONT_NORMAL);
                cb.setSelected(enabled);
                cb.setToolTipText(sqlFile);
                cb.setHorizontalAlignment(SwingConstants.LEFT);
                checkBoxMap.put(cfg, cb);
                checkboxPanel.add(cb);
            }
            statusLabel.setText("加载配置成功，共 " + configs.size() + " 个任务");
        } catch (Exception e) {
            statusLabel.setText("加载配置失败: " + e.getMessage());
            JLabel err = new JLabel("加载配置失败: " + e.getMessage());
            err.setForeground(Color.RED);
            checkboxPanel.add(err);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
        checkboxPanel.revalidate();
        checkboxPanel.repaint();
    }

    private void setAllCheckBoxes(boolean selected) {
        if (checkBoxMap == null) return;
        for (JCheckBox cb : checkBoxMap.values()) cb.setSelected(selected);
    }

    private void loadStatsData() {
        String selectedName = (String) dataSourceCombo.getSelectedItem();
        if (selectedName == null || dataSources.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择数据源", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DataSource ds = findDataSource(selectedName);
        if (ds == null) {
            JOptionPane.showMessageDialog(this, "数据源不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<StatsConfig> selectedTasks = new ArrayList<>();
        for (Map.Entry<StatsConfig, JCheckBox> entry : checkBoxMap.entrySet()) {
            if (entry.getValue().isSelected()) selectedTasks.add(entry.getKey());
        }
        if (selectedTasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一个任务", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tasksPanel.removeAll();
        tableModels.clear();
        rawData.clear();
        columnNames.clear();
        tableMap.clear();

        setUIEnabled(false);
        refreshButton.setText("执行中...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("正在执行查询...");
        exportAllButton.setEnabled(false);

        final DataSource finalDs = ds;
        final List<StatsConfig> finalTasks = selectedTasks;

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                Connection conn = null;
                try {
                    if ("ORACLE".equalsIgnoreCase(finalDs.getType())) {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } else if ("GAUSSDB".equalsIgnoreCase(finalDs.getType())) {
                        try { Class.forName("com.huawei.gaussdb.jdbc.Driver"); }
                        catch (ClassNotFoundException e1) {
                            try { Class.forName("com.huawei.gauss.jdbc.Driver"); }
                            catch (ClassNotFoundException e2) { Class.forName("org.postgresql.Driver"); }
                        }
                    } else {
                        throw new SQLException("不支持的数据源类型");
                    }

                    conn = DriverManager.getConnection(finalDs.buildUrl(), finalDs.getUser(), finalDs.getPassword());

                    int idx = 0;
                    for (StatsConfig cfg : finalTasks) {
                        idx++;
                        publish("正在执行: " + cfg.getDescription() + " (" + idx + "/" + finalTasks.size() + ")");

                        String sql = loadSqlFile(cfg.getSqlFile());
                        if (sql == null) {
                            publish("  ⚠️ 无法加载 SQL 文件: " + cfg.getSqlFile());
                            continue;
                        }

                        try (Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery(sql)) {
                            ResultSetMetaData meta = rs.getMetaData();
                            int colCount = meta.getColumnCount();
                            String[] cols = new String[colCount];
                            for (int i = 0; i < colCount; i++) cols[i] = meta.getColumnName(i + 1);

                            List<Object[]> rows = new ArrayList<>();
                            while (rs.next()) {
                                Object[] row = new Object[colCount];
                                for (int i = 0; i < colCount; i++) row[i] = rs.getObject(i + 1);
                                rows.add(row);
                            }

                            columnNames.put(cfg, cols);
                            rawData.put(cfg, rows);

                            DefaultTableModel model = new DefaultTableModel();
                            model.setColumnIdentifiers(cols);
                            for (Object[] r : rows) model.addRow(r);
                            tableModels.put(cfg, model);

                            publish("  ✅ 完成: " + cfg.getDescription() + " (行数: " + rows.size() + ")");
                        } catch (SQLException e) {
                            publish("  ❌ 失败: " + cfg.getDescription() + " - " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    publish("❌ 连接失败: " + e.getMessage());
                } finally {
                    if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String m : chunks) statusLabel.setText(m);
            }

            @Override
            protected void done() {
                setUIEnabled(true);
                refreshButton.setText("🔄 刷新数据");
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                statusLabel.setText("执行完成");
                displayTables();
            }
        }.execute();
    }

    private String loadSqlFile(String fileName) {
        File external = new File(EXTERNAL_CONFIG_DIR + fileName);
        if (external.exists() && external.isFile()) {
            try (InputStream is = new FileInputStream(external)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                statusLabel.setText("读取外部 SQL 失败: " + fileName + " - " + e.getMessage());
            }
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("stats/" + fileName)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void displayTables() {
        tasksPanel.removeAll();
        tableMap.clear();

        if (tableModels.isEmpty()) {
            tasksPanel.add(new JLabel("没有数据"));
            tasksPanel.revalidate();
            tasksPanel.repaint();
            return;
        }

        int cardIndex = 0;
        for (Map.Entry<StatsConfig, DefaultTableModel> entry : tableModels.entrySet()) {
            StatsConfig cfg = entry.getKey();
            DefaultTableModel model = entry.getValue();

            JPanel card = new JPanel(new BorderLayout(0, 4));
            card.setBackground(CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_MAX_HEIGHT));
            card.setPreferredSize(new Dimension(0, CARD_MAX_HEIGHT));

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setOpaque(false);
            JLabel titleLabel = new JLabel((cardIndex + 1) + ". " + cfg.getDescription());
            titleLabel.setFont(FONT_BOLD);
            titleLabel.setForeground(TEXT);
            titlePanel.add(titleLabel, BorderLayout.WEST);

            JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rightInfo.setOpaque(false);
            JLabel rowLabel = new JLabel(model.getRowCount() + " 行");
            rowLabel.setFont(FONT_SMALL);
            rowLabel.setForeground(TEXT_SEC);
            rightInfo.add(rowLabel);

            // ---- 导出按钮 ----
            JButton expBtn = new JButton("⬇ 导出");
            expBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            expBtn.setBackground(new Color(239, 248, 237));
            expBtn.setForeground(new Color(29, 143, 40));
            expBtn.setFocusPainted(false);
            expBtn.setBorder(BorderFactory.createLineBorder(new Color(174, 215, 176), 1));
            expBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            expBtn.setPreferredSize(new Dimension(73, 27));
            rightInfo.add(expBtn);

            // ---- 图表按钮 ----
            JButton chartBtn = new JButton("📊 图表");
            chartBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            chartBtn.setBackground(new Color(233, 242, 252));
            chartBtn.setForeground(new Color(33, 102, 206));
            chartBtn.setFocusPainted(false);
            chartBtn.setBorder(BorderFactory.createLineBorder(new Color(176, 206, 239), 1));
            chartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            chartBtn.setPreferredSize(new Dimension(76, 27));
            rightInfo.add(chartBtn);

            titlePanel.add(rightInfo, BorderLayout.EAST);
            card.add(titlePanel, BorderLayout.NORTH);

            // ---- 表格 ----
            JTable table = new JTable(model);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setRowHeight(29);
            table.setFont(FONT_NORMAL);
            table.setGridColor(new Color(228, 234, 242));
            table.setShowVerticalLines(true);
            table.setShowHorizontalLines(true);

            JTableHeader header = table.getTableHeader();
            header.setFont(FONT_SMALL);
            header.setBackground(TABLE_HEADER_BG);
            header.setForeground(TABLE_HEADER_FG);
            header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? new Color(250, 252, 254) : Color.WHITE);
                    } else {
                        c.setBackground(new Color(206, 220, 239));
                        c.setForeground(TEXT);
                    }
                    setHorizontalAlignment(JLabel.CENTER);
                    return c;
                }
            });

            // ---- 滚动面板 ----
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            tableScroll.setPreferredSize(new Dimension(0, TABLE_PREFERRED_HEIGHT));
            tableScroll.setMinimumSize(new Dimension(0, 123));
            tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            card.add(tableScroll, BorderLayout.CENTER);

            tableMap.put(cfg, table);

            // 延迟调整列宽
            SwingUtilities.invokeLater(() -> autoResizeColumns(table));

            // 窗口大小改变时重新调整列宽
            tableScroll.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    autoResizeColumns(table);
                }
            });

            // ---- 右键菜单 ----
            JPopupMenu popup = new JPopupMenu();
            JMenuItem exportItem = new JMenuItem("📥 导出为 Excel");
            exportItem.setFont(FONT_NORMAL);
            exportItem.addActionListener(e -> TableExportUtil.exportToExcel(table, this, cfg.getDescription()));
            popup.add(exportItem);
            table.setComponentPopupMenu(popup);
            table.getTableHeader().setComponentPopupMenu(popup);

            // 导出按钮事件
            expBtn.addActionListener(e -> TableExportUtil.exportToExcel(table, this, cfg.getDescription()));

            // 图表按钮事件（弹出图表类型选择）
            final DefaultTableModel finalModel = model;
            chartBtn.addActionListener(e -> promptChartTypeAndShow(cfg.getDescription(), finalModel));

            tasksPanel.add(card);
            tasksPanel.add(Box.createVerticalStrut(15));
            cardIndex++;
        }

        exportAllButton.setEnabled(true);
        tasksPanel.revalidate();
        tasksPanel.repaint();
    }

    /**
     * 弹出图表类型 + 分类列/数值列选择框
     */
    private void promptChartTypeAndShow(String title, DefaultTableModel model) {
        int colCount = model.getColumnCount();
        if (colCount < 2) {
            JOptionPane.showMessageDialog(this, "数据不足以生成图表（需要至少两列）", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] colNames = new String[colCount];
        for (int i = 0; i < colCount; i++) colNames[i] = model.getColumnName(i);

        // ---- 图表类型 ----
        String[] chartOptions = {"柱状图", "折线图", "饼图", "面积图"};
        JComboBox<String> chartCombo = new JComboBox<>(chartOptions);
        chartCombo.setFont(FONT_NORMAL);

        // ---- 分类列（X轴 / 饼图标签）----
        JComboBox<String> catCombo = new JComboBox<>(colNames);
        catCombo.setFont(FONT_NORMAL);
        catCombo.setSelectedIndex(0);

        // ---- 数值列（Y轴 / 饼图数值），可多选（柱状图/折线图支持多系列）----
        DefaultListModel<String> valListModel = new DefaultListModel<>();
        for (String c : colNames) valListModel.addElement(c);
        JList<String> valList = new JList<>(valListModel);
        valList.setFont(FONT_NORMAL);
        valList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        valList.setVisibleRowCount(Math.min(colCount, 6));
        // 默认预选第一个可以解析为数字的列
        int defaultVal = -1;
        int rowCount = model.getRowCount();
        outerDefault:
        for (int c = 0; c < colCount; c++) {
            for (int r = 0; r < Math.min(rowCount, 20); r++) {
                Object v = model.getValueAt(r, c);
                if (v == null) continue;
                try {
                    Double.parseDouble(v.toString());
                    defaultVal = c;
                    break outerDefault;
                } catch (NumberFormatException ignored) {}
            }
        }
        if (defaultVal >= 0) valList.setSelectedIndex(defaultVal);
        JScrollPane valScroll = new JScrollPane(valList);
        valScroll.setPreferredSize(new Dimension(220, 110));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new BorderLayout(8, 4));
        JLabel chartLabel = new JLabel("图表类型：");
        chartLabel.setFont(FONT_NORMAL);
        row1.add(chartLabel, BorderLayout.WEST);
        row1.add(chartCombo, BorderLayout.CENTER);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JPanel row2 = new JPanel(new BorderLayout(8, 4));
        JLabel catLabel = new JLabel("分类列（X轴 / 饼图标签）：");
        catLabel.setFont(FONT_NORMAL);
        row2.add(catLabel, BorderLayout.WEST);
        row2.add(catCombo, BorderLayout.CENTER);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JPanel row3 = new JPanel(new BorderLayout(8, 4));
        JLabel valLabel = new JLabel("数值列（Y轴，可多选，饼图只取第一个）：");
        valLabel.setFont(FONT_NORMAL);
        row3.add(valLabel, BorderLayout.NORTH);
        row3.add(valScroll, BorderLayout.CENTER);
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(row1);
        panel.add(Box.createVerticalStrut(10));
        panel.add(row2);
        panel.add(Box.createVerticalStrut(10));
        panel.add(row3);
        panel.setPreferredSize(new Dimension(320, 240));

        int result = JOptionPane.showConfirmDialog(
                this, panel, "生成图表 - " + title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        ChartKind kind;
        switch (chartCombo.getSelectedIndex()) {
            case 1: kind = ChartKind.LINE; break;
            case 2: kind = ChartKind.PIE; break;
            case 3: kind = ChartKind.AREA; break;
            default: kind = ChartKind.BAR;
        }

        int catCol = catCombo.getSelectedIndex();
        List<Integer> valCols = new ArrayList<>();
        for (int idx : valList.getSelectedIndices()) valCols.add(idx);

        if (valCols.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少选择一个数值列", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (valCols.contains(catCol) && valCols.size() == 1) {
            JOptionPane.showMessageDialog(this, "数值列不能与分类列相同", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        showChart(title, model, kind, catCol, valCols);
    }

    /**
     * 根据表格数据、所选图表类型、分类列、数值列（可多个）生成图表并弹出窗口
     */
    private void showChart(String title, DefaultTableModel model, ChartKind kind,
                            int catCol, List<Integer> valCols) {
        // 确保在 EDT 上执行（双重保险）
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showChart(title, model, kind, catCol, valCols));
            return;
        }

        try {
            int rowCount = model.getRowCount();
            if (rowCount == 0 || valCols.isEmpty()) {
                JOptionPane.showMessageDialog(this, "数据不足以生成图表", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String categoryCol = model.getColumnName(catCol);
            JFreeChart chart;
            int categoryCount = 0;   // 用于动态调整窗口大小
            int maxLabelLen = 0;     // 用于决定 X 轴标签是否需要旋转

            if (kind == ChartKind.PIE) {
                int valCol = valCols.get(0);
                String valueCol = model.getColumnName(valCol);
                DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
                for (int i = 0; i < rowCount; i++) {
                    Object catObj = model.getValueAt(i, catCol);
                    Object valObj = model.getValueAt(i, valCol);
                    if (catObj == null || valObj == null) continue;
                    try {
                        pieDataset.setValue(catObj.toString(), Double.parseDouble(valObj.toString()));
                    } catch (NumberFormatException ignored) {}
                }
                if (pieDataset.getItemCount() == 0) {
                    JOptionPane.showMessageDialog(this, "所选数值列（" + valueCol + "）没有有效的数值数据", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                categoryCount = pieDataset.getItemCount();

                chart = ChartFactory.createPieChart(title, pieDataset, true, true, false);
                PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
                plot.setBackgroundPaint(Color.WHITE);
                plot.setOutlineVisible(false);
                plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
                // 切片较多时标签容易挤在一起，只保留名称+百分比，完整数值放图例里看
                plot.setLabelGenerator(categoryCount > 8
                        ? new StandardPieSectionLabelGenerator("{0}: {2}")
                        : new StandardPieSectionLabelGenerator("{0}: {1} ({2})"));
                plot.setLabelBackgroundPaint(new Color(255, 255, 255, 220));
                plot.setLabelOutlinePaint(null);
                plot.setLabelShadowPaint(null);
                plot.setSectionOutlinesVisible(false);
                plot.setShadowPaint(null);
                plot.setInteriorGap(0.04);
                plot.setLabelLinkPaint(new Color(180, 188, 199));
                plot.setLabelLinkStroke(new BasicStroke(1f));
                int colorIdx = 0;
                for (Object key : pieDataset.getKeys()) {
                    plot.setSectionPaint((String) key, PIE_PALETTE[colorIdx % PIE_PALETTE.length]);
                    colorIdx++;
                }
            } else {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (int valCol : valCols) {
                    String valueCol = model.getColumnName(valCol);
                    for (int i = 0; i < rowCount; i++) {
                        Object catObj = model.getValueAt(i, catCol);
                        Object valObj = model.getValueAt(i, valCol);
                        if (catObj == null || valObj == null) continue;
                        try {
                            dataset.addValue(Double.parseDouble(valObj.toString()), valueCol, catObj.toString());
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (dataset.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "所选数值列没有有效的数值数据", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                categoryCount = dataset.getColumnCount();
                for (Object catKey : dataset.getColumnKeys()) {
                    maxLabelLen = Math.max(maxLabelLen, String.valueOf(catKey).length());
                }

                String axisLabel = valCols.size() == 1 ? model.getColumnName(valCols.get(0)) : "数值";

                switch (kind) {
                    case LINE:
                        chart = ChartFactory.createLineChart(title, categoryCol, axisLabel, dataset,
                                PlotOrientation.VERTICAL, valCols.size() > 1, true, false);
                        break;
                    case AREA:
                        chart = ChartFactory.createAreaChart(title, categoryCol, axisLabel, dataset,
                                PlotOrientation.VERTICAL, valCols.size() > 1, true, false);
                        break;
                    default:
                        chart = ChartFactory.createBarChart(title, categoryCol, axisLabel, dataset,
                                PlotOrientation.VERTICAL, valCols.size() > 1, true, false);
                }

                CategoryPlot plot = chart.getCategoryPlot();
                plot.setBackgroundPaint(new Color(247, 249, 252));
                plot.setRangeGridlinePaint(new Color(222, 227, 235));
                plot.setDomainGridlinePaint(new Color(222, 227, 235));
                plot.setOutlineVisible(false);
                plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

                // ---- X 轴（分类轴）：文字太长/太多就自动倾斜，避免被截断看不见 ----
                CategoryAxis domainAxis = plot.getDomainAxis();
                domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
                domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
                domainAxis.setCategoryMargin(valCols.size() > 1 ? 0.25 : 0.15);
                domainAxis.setMaximumCategoryLabelWidthRatio(100f); // 不强制截断标签
                if (categoryCount > 12 || maxLabelLen > 10) {
                    // 类目很多或文字很长：倾斜 60°，尽量不重叠
                    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.toRadians(60)));
                } else if (categoryCount > 5 || maxLabelLen > 4) {
                    // 类目中等或有一定长度：倾斜 30°
                    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.toRadians(30)));
                } else {
                    // 类目少且短：保持水平，最清晰
                    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
                }

                // ---- Y 轴（数值轴）：千分位分隔，数字更好读 ----
                if (plot.getRangeAxis() instanceof NumberAxis) {
                    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                    rangeAxis.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
                    rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
                    rangeAxis.setNumberFormatOverride(NumberFormat.getNumberInstance());
                    rangeAxis.setAutoRangeIncludesZero(true);
                }

                if (kind == ChartKind.LINE) {
                    LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
                    for (int i = 0; i < valCols.size(); i++) {
                        Color c = PIE_PALETTE[i % PIE_PALETTE.length];
                        renderer.setSeriesPaint(i, c);
                        renderer.setSeriesStroke(i, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        renderer.setSeriesShapesVisible(i, true);
                        renderer.setSeriesShapesFilled(i, true);
                        renderer.setSeriesOutlinePaint(i, Color.WHITE);
                    }
                    renderer.setDefaultItemLabelsVisible(valCols.size() == 1 && categoryCount <= 15);
                    renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                    renderer.setDefaultItemLabelPaint(new Color(78, 88, 102));
                } else if (kind == ChartKind.AREA) {
                    AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
                    for (int i = 0; i < valCols.size(); i++) {
                        Color c = PIE_PALETTE[i % PIE_PALETTE.length];
                        renderer.setSeriesPaint(i, new Color(c.getRed(), c.getGreen(), c.getBlue(), 165));
                        renderer.setSeriesOutlinePaint(i, c);
                        renderer.setSeriesOutlineStroke(i, new BasicStroke(2f));
                    }
                } else {
                    BarRenderer renderer = (BarRenderer) plot.getRenderer();
                    // 用渐变柱身替代 JFreeChart 新版默认的“扁平单色”柱子，视觉上更精致
                    renderer.setBarPainter(new GradientBarPainter(0.12, 0.15, 0.55));
                    renderer.setShadowVisible(false);
                    renderer.setDrawBarOutline(false);
                    renderer.setItemMargin(valCols.size() > 1 ? 0.12 : 0.0);
                    renderer.setMaximumBarWidth(categoryCount <= 4 ? 0.12 : 0.08);
                    renderer.setMinimumBarLength(0.01);
                    for (int i = 0; i < valCols.size(); i++) {
                        renderer.setSeriesPaint(i, PIE_PALETTE[i % PIE_PALETTE.length]);
                    }
                    renderer.setDefaultItemLabelsVisible(valCols.size() == 1 && categoryCount <= 20);
                    renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));
                    renderer.setDefaultItemLabelPaint(new Color(78, 88, 102));
                }
            }

            // ---------- 通用美化 ----------
            chart.setBackgroundPaint(Color.WHITE);
            chart.setBorderVisible(false);
            chart.setPadding(new RectangleInsets(8, 8, 8, 8));
            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 17));
            chart.getTitle().setPaint(new Color(53, 63, 79));
            if (chart.getLegend() != null) {
                chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));
                chart.getLegend().setBackgroundPaint(Color.WHITE);
                chart.getLegend().setBorder(0, 0, 0, 0);
            }

            // ---------- 根据数据量动态计算窗口尺寸 ----------
            // 分类越多、标签越长、系列越多，图表就需要越宽/越高，否则内容挤在一起看不清
            int seriesCount = valCols.size();
            boolean rotated = kind != ChartKind.PIE && (categoryCount > 5 || maxLabelLen > 4);

            int width = 760;
            width += Math.max(0, categoryCount - 6) * (kind == ChartKind.PIE ? 12 : 34);
            width += (seriesCount > 1 ? (seriesCount - 1) * 40 : 0);
            width = Math.max(720, Math.min(1600, width));

            int height = 560;
            if (rotated) height += 60;              // 倾斜标签需要额外底部空间
            if (kind == ChartKind.PIE && categoryCount > 8) height += 40; // 图例项多时加高
            if (seriesCount > 4) height += (seriesCount - 4) * 18;        // 图例换行预留
            height = Math.max(540, Math.min(980, height));

            // ---------- 弹出窗口（ChartPanel 自带缩放/另存为图片/打印） ----------
            // 注意：当前对话框本身是模态的（BaseDialog），如果用独立的 JFrame 展示图表，
            // 会被模态对话框挡住/抢不到焦点，出现"点确认后图表窗口不见了"的现象。
            // 这里改用归属于当前对话框的非模态子 JDialog，保证正常显示在最前面。
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setMouseWheelEnabled(true);
            chartPanel.setPreferredSize(new Dimension(width, height));

            JDialog chartDialog = new JDialog(this, title, false);
            chartDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            chartDialog.setContentPane(chartPanel);
            chartDialog.pack();
            chartDialog.setMinimumSize(new Dimension(600, 460));
            chartDialog.setLocationRelativeTo(this);
            chartDialog.setAlwaysOnTop(true);
            chartDialog.setVisible(true);
            chartDialog.toFront();

        } catch (Throwable ex) {
            // 用 Throwable 而不是 Exception：避免 NoSuchMethodError / NoClassDefFoundError
            // 等因 jfreechart/jcommon 版本冲突产生的 Error 被静默吞掉，导致"点了没反应"
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "生成图表失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAllToExcel() {
        if (tableMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Map<String, JTable> exportMap = new LinkedHashMap<>();
        for (Map.Entry<StatsConfig, JTable> e : tableMap.entrySet()) {
            exportMap.put(e.getKey().getDescription(), e.getValue());
        }
        TableExportUtil.exportMultipleToExcel(exportMap, this);
    }

    private void autoResizeColumns(JTable table) {
        if (table.getColumnCount() == 0) return;

        Container parent = table.getParent();
        if (parent == null) return;

        int viewWidth = parent.getWidth();
        if (viewWidth <= 0) viewWidth = 1280;

        int colCount = table.getColumnCount();
        int[] minWidths = new int[colCount];
        int totalMinWidth = 0;

        for (int col = 0; col < colCount; col++) {
            int maxWidth = 111;
            TableColumn column = table.getColumnModel().getColumn(col);

            Object hv = column.getHeaderValue();
            if (hv != null) {
                FontMetrics fm = table.getFontMetrics(table.getTableHeader().getFont());
                maxWidth = Math.max(maxWidth, fm.stringWidth(hv.toString()) + 50);
            }

            int rc = Math.min(table.getRowCount(), 100);
            for (int row = 0; row < rc; row++) {
                Object v = table.getValueAt(row, col);
                if (v != null) {
                    FontMetrics fm = table.getFontMetrics(table.getFont());
                    int w = fm.stringWidth(v.toString()) + 50;
                    if (w > maxWidth) maxWidth = w;
                }
            }

            minWidths[col] = Math.min(Math.max(maxWidth, 111), 650);
            totalMinWidth += minWidths[col];
        }

        if (totalMinWidth < viewWidth && colCount > 0) {
            int remaining = viewWidth - totalMinWidth;
            int extraPerCol = remaining / colCount;
            for (int col = 0; col < colCount; col++) {
                TableColumn column = table.getColumnModel().getColumn(col);
                int finalWidth = minWidths[col] + extraPerCol;
                column.setPreferredWidth(finalWidth);
                column.setMinWidth(finalWidth);
            }
        } else {
            for (int col = 0; col < colCount; col++) {
                TableColumn column = table.getColumnModel().getColumn(col);
                column.setPreferredWidth(minWidths[col]);
                column.setMinWidth(minWidths[col]);
            }
        }

        table.getTableHeader().resizeAndRepaint();
    }

    private DataSource findDataSource(String name) {
        for (DataSource ds : dataSources) if (ds.getName().equals(name)) return ds;
        return null;
    }

    private void setUIEnabled(boolean enabled) {
        dataSourceCombo.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        selectAllButton.setEnabled(enabled);
        deselectAllButton.setEnabled(enabled);
        for (JCheckBox cb : checkBoxMap.values()) cb.setEnabled(enabled);
    }

    @Override
    public void refresh() {
        loadDataSources();
        if (configPanel != null) {
            JScrollPane scroll = (JScrollPane) configPanel.getComponent(1);
            JPanel checkboxPanel = (JPanel) scroll.getViewport().getView();
            loadConfig(checkboxPanel);
            configPanel.revalidate();
            configPanel.repaint();
        }
    }
}