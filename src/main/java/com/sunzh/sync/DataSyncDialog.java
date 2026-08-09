package com.sunzh.sync;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;
import com.sunzh.ui.BaseDialog;
import com.sunzh.utils.SvgIconUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class DataSyncDialog extends BaseDialog {

    // -------------------- 颜色和字体 --------------------
    private static final Color BG           = new Color(240, 242, 245);
    private static final Color CARD         = Color.WHITE;
    private static final Color PRIMARY      = new Color(70, 110, 150);
    private static final Color PRIMARY_H    = new Color(55, 90, 130);
    private static final Color DANGER       = new Color(221, 68, 68);
    private static final Color DANGER_H     = new Color(187, 34, 34);
    private static final Color WARN         = new Color(230, 138, 0);
    private static final Color TEXT         = new Color(34, 51, 68);
    private static final Color TEXT_SEC     = new Color(136, 153, 170);
    // private static final Color BORDER       = new Color(221, 224, 230);
        private static final Color BORDER       = new Color(210, 215, 222);
    private static final Color INPUT_BG     = new Color(250, 251, 252);
    private static final Color LOG_BG       = new Color(26, 26, 30);
    private static final Color LOG_FG       = new Color(204, 221, 238);
    private static final Color LOG_INFO     = new Color(102, 187, 255);
    private static final Color LOG_OK       = new Color(85, 204, 119);
    private static final Color LOG_ERR      = new Color(255, 102, 102);

    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static final Font FONT_BTN   = new Font("SansSerif", Font.BOLD, 14);
    private static final Font FONT_LOG   = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font FONT_FIELD = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);

    // -------------------- UI 组件 --------------------
    private JTextArea logArea;
    private JButton btnStop;
    private SwingWorker<?, ?> currentWorker;
    private Process currentProcess;

    // Tab1: Oracle → GaussDB
    private JComboBox<String> cmbSrcOra, cmbTgtGauss;
    private JTextArea taTableMaps;
    private JRadioButton rbOverwrite, rbAppend;

    // Tab2: GaussDB → Oracle
    private JComboBox<String> cmbSrcGauss, cmbTgtOra;
    private JTextArea taTableMapsG2O;
    private JRadioButton rbOverwriteG2O, rbAppendG2O;

    // Tab3: Excel → Oracle
    private JComboBox<String> cmbETO_Ora;
    private JTextField tfExcel1;

    // Tab4: Excel → GaussDB
    private JComboBox<String> cmbETG_Gauss;
    private JTextField tfExcel2;

    public DataSyncDialog(JFrame owner) {
        super(owner, "数据同步工具", "transfer");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout(0, 0));
        mainContentPanel.setBackground(BG);

        mainContentPanel.add(createHeader(), BorderLayout.NORTH);
        mainContentPanel.add(createTabPane(), BorderLayout.CENTER);
        mainContentPanel.add(createLogPanel(), BorderLayout.SOUTH);

        // setSize(1280, 900);
        refreshAllCombos();
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JLabel title = new JLabel("数据同步工具");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        p.add(title, BorderLayout.WEST);

        JButton clearBtn = new JButton("清空日志");
        clearBtn.setIcon(SvgIconUtils.get("clear", 14, PRIMARY));
        clearBtn.setFont(FONT_SMALL);
        clearBtn.setText("清空日志");
        clearBtn.setForeground(PRIMARY);
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> logArea.setText(""));
        p.add(clearBtn, BorderLayout.EAST);
        return p;
    }

    private JTabbedPane createTabPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_BOLD);
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        tabs.addTab("  Oracle → GaussDB", SvgIconUtils.get("transfer", 16), buildO2GPanel());
        tabs.addTab("  GaussDB → Oracle", SvgIconUtils.get("transfer", 16), buildG2OPanel());
        tabs.addTab("  Excel → Oracle", SvgIconUtils.get("file-spreadsheet", 16), buildExcelOraclePanel());
        tabs.addTab("  Excel → GaussDB", SvgIconUtils.get("file-spreadsheet", 16), buildExcelGaussPanel());

        return tabs;
    }

    // ---------- Tab1: Oracle → GaussDB ----------
    private JPanel buildO2GPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        // ---- 行1: 数据源选择（两栏） ----
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel dsRow = new JPanel(new GridBagLayout());
        dsRow.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 0.5;
        g.weighty = 0;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 0, 10);

        cmbSrcOra = new JComboBox<>();
        cmbTgtGauss = new JComboBox<>();
        styleCombo(cmbSrcOra);
        styleCombo(cmbTgtGauss);

        g.gridx = 0;
        g.insets = new Insets(0, 0, 0, 12);
        dsRow.add(buildSelectorCard("源端 Oracle 数据源", cmbSrcOra, "ORACLE"), g);
        g.gridx = 1;
        g.insets = new Insets(0, 12, 0, 0);
        dsRow.add(buildSelectorCard("目标端 GaussDB 数据源", cmbTgtGauss, "GAUSSDB"), g);

        panel.add(dsRow, gbc);

        // ---- 行2: 表映射 ----
        gbc.gridy = 1;
        gbc.weighty = 0.7;  // 分配较多垂直空间
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel mapCard = new JPanel(new BorderLayout(0, 6));
        mapCard.setBackground(CARD);
        mapCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));

        JPanel mapTopPanel = new JPanel();
        mapTopPanel.setOpaque(false);
        mapTopPanel.setLayout(new BoxLayout(mapTopPanel, BoxLayout.Y_AXIS));
        mapTopPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel mapTitle = SvgIconUtils.labelWithFont("table", "表映射", FONT_BOLD, TEXT);
        mapTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapTopPanel.add(mapTitle);
        mapTopPanel.add(Box.createVerticalStrut(4));

        JLabel hint = label("格式: TABLE1,TABLE2  或  OLD:NEW (重命名), 逗号分隔", FONT_SMALL, TEXT_SEC);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapTopPanel.add(hint);

        mapCard.add(mapTopPanel, BorderLayout.NORTH);

        taTableMaps = new JTextArea(8, 80);
        taTableMaps.setFont(FONT_FIELD);
        taTableMaps.setLineWrap(true);
        taTableMaps.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        JScrollPane sp = new JScrollPane(taTableMaps);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // sp.setPreferredSize(new Dimension(900, 260)); // 改为由布局动态分配高度
        mapCard.add(sp, BorderLayout.CENTER);

        panel.add(mapCard, gbc);

        // ---- 行3: 覆盖/追加选项 ----
        gbc.gridy = 2;
        gbc.weighty = 0.2;  // 占用少量垂直空间
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel optCard = new JPanel(new BorderLayout(0, 8));
        optCard.setBackground(CARD);
        optCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));

        JLabel optTitle = label("目标表已存在时的处理方法", FONT_BOLD, TEXT);
        optCard.add(optTitle, BorderLayout.NORTH);

        JPanel optRadioPanel = new JPanel();
        optRadioPanel.setOpaque(false);
        optRadioPanel.setLayout(new BoxLayout(optRadioPanel, BoxLayout.Y_AXIS));
        optRadioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbOverwrite = new JRadioButton("覆盖 — 删除表后重新创建并插入数据（默认）", true);
        rbAppend = new JRadioButton("追加 — 保留原有数据，仅插入新数据");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbOverwrite);
        bg.add(rbAppend);
        for (JRadioButton rb : new JRadioButton[]{rbOverwrite, rbAppend}) {
            rb.setFont(FONT_FIELD);
            rb.setBackground(CARD);
            rb.setFocusPainted(false);
            rb.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);
            optRadioPanel.add(rb);
        }

        optCard.add(optRadioPanel, BorderLayout.CENTER);
        panel.add(optCard, gbc);

        // ---- 行4: 执行按钮 ----
        gbc.gridy = 3;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        JButton btn = btnSolid("执行同步", PRIMARY, PRIMARY_H, true);
        btn.setIcon(SvgIconUtils.get("play", 16, Color.WHITE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.addActionListener(e -> runO2G());
        btnPanel.add(btn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // ---------- Tab2: GaussDB → Oracle ----------
    private JPanel buildG2OPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel dsRow = new JPanel(new GridBagLayout());
        dsRow.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 0.5;
        g.weighty = 0;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 0, 10);

        cmbSrcGauss = new JComboBox<>();
        cmbTgtOra = new JComboBox<>();
        styleCombo(cmbSrcGauss);
        styleCombo(cmbTgtOra);

        g.gridx = 0;
        g.insets = new Insets(0, 0, 0, 12);
        dsRow.add(buildSelectorCard("源端 GaussDB 数据源", cmbSrcGauss, "GAUSSDB"), g);
        g.gridx = 1;
        g.insets = new Insets(0, 12, 0, 0);
        dsRow.add(buildSelectorCard("目标端 Oracle 数据源", cmbTgtOra, "ORACLE"), g);

        panel.add(dsRow, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.7;
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel mapCard = new JPanel(new BorderLayout(0, 6));
        mapCard.setBackground(CARD);
        mapCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));

        JPanel mapTopPanel = new JPanel();
        mapTopPanel.setOpaque(false);
        mapTopPanel.setLayout(new BoxLayout(mapTopPanel, BoxLayout.Y_AXIS));
        mapTopPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel mapTitle = SvgIconUtils.labelWithFont("table", "表映射", FONT_BOLD, TEXT);
        mapTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapTopPanel.add(mapTitle);
        mapTopPanel.add(Box.createVerticalStrut(4));

        JLabel hint = label("格式: TABLE1,TABLE2  或  OLD:NEW (重命名), 逗号分隔", FONT_SMALL, TEXT_SEC);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        mapTopPanel.add(hint);

        mapCard.add(mapTopPanel, BorderLayout.NORTH);

        taTableMapsG2O = new JTextArea(8, 80);
        taTableMapsG2O.setFont(FONT_FIELD);
        taTableMapsG2O.setLineWrap(true);
        taTableMapsG2O.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));
        JScrollPane sp = new JScrollPane(taTableMapsG2O);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // sp.setPreferredSize(new Dimension(900, 260)); // 改为由布局动态分配高度
        mapCard.add(sp, BorderLayout.CENTER);

        panel.add(mapCard, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.2;
        gbc.insets = new Insets(0, 0, 8, 0);

        JPanel optCard = new JPanel(new BorderLayout(0, 8));
        optCard.setBackground(CARD);
        optCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));

        JLabel optTitle = label("目标表已存在时的处理方法", FONT_BOLD, TEXT);
        optCard.add(optTitle, BorderLayout.NORTH);

        JPanel optRadioPanel = new JPanel();
        optRadioPanel.setOpaque(false);
        optRadioPanel.setLayout(new BoxLayout(optRadioPanel, BoxLayout.Y_AXIS));
        optRadioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbOverwriteG2O = new JRadioButton("覆盖 — 删除表后重新创建并插入数据（默认）", true);
        rbAppendG2O = new JRadioButton("追加 — 保留原有数据，仅插入新数据");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbOverwriteG2O);
        bg.add(rbAppendG2O);
        for (JRadioButton rb : new JRadioButton[]{rbOverwriteG2O, rbAppendG2O}) {
            rb.setFont(FONT_FIELD);
            rb.setBackground(CARD);
            rb.setFocusPainted(false);
            rb.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);
            optRadioPanel.add(rb);
        }

        optCard.add(optRadioPanel, BorderLayout.CENTER);
        panel.add(optCard, gbc);

        gbc.gridy = 3;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        JButton btn = btnSolid("执行同步", PRIMARY, PRIMARY_H, true);
        btn.setIcon(SvgIconUtils.get("play", 16, Color.WHITE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.addActionListener(e -> runG2O());
        btnPanel.add(btn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // ---------- Tab3: Excel → Oracle ----------
    private JPanel buildExcelOraclePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        // 边距对齐 Tab1/Tab2（16,20,20,20），保持四页视觉一致
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        cmbETO_Ora = new JComboBox<>();
        styleCombo(cmbETO_Ora);
        JPanel dsCard = buildSelectorCard("目标 Oracle 数据源", cmbETO_Ora, "ORACLE");
        panel.add(dsCard, gbc);

        gbc.gridy = 1;
        // 中间行吸收剩余垂直空间（对齐 Tab1/Tab2：可伸缩内容行在上，按钮行固定在底部）
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);

        tfExcel1 = new JTextField();
        tfExcel1.setEditable(false);
        tfExcel1.setBackground(INPUT_BG);
        tfExcel1.setFont(FONT_FIELD);
        tfExcel1.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JButton btnBrowse1 = btnOutline("浏览...");
        btnBrowse1.setIcon(SvgIconUtils.get("folder-open", 14, PRIMARY));
        btnBrowse1.addActionListener(e -> browseExcel(tfExcel1));

        JPanel fileRow = new JPanel(new BorderLayout(10, 0));
        fileRow.setOpaque(false);
        fileRow.add(tfExcel1, BorderLayout.CENTER);
        fileRow.add(btnBrowse1, BorderLayout.EAST);

        JPanel fileCard = new JPanel(new BorderLayout(0, 8));
        fileCard.setBackground(CARD);
        fileCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));
        fileCard.add(label("选择 Excel 文件", FONT_BOLD, TEXT), BorderLayout.NORTH);
        fileCard.add(fileRow, BorderLayout.CENTER);

        panel.add(fileCard, gbc);

        gbc.gridy = 2;
        // 按钮行 weighty=0 固定在底部（完全对齐 Tab1/Tab2 的按钮行写法），
        // 上方 fileCard(weighty=1.0) 先吸收压缩，按钮不会被底部裁切
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        JButton btn = btnSolid("执行导入", PRIMARY, PRIMARY_H, true);
        btn.setIcon(SvgIconUtils.get("play", 16, Color.WHITE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.addActionListener(e -> runE2O());
        btnPanel.add(btn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // ---------- Tab4: Excel → GaussDB ----------
    private JPanel buildExcelGaussPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        // 边距对齐 Tab1/Tab2（16,20,20,20），保持四页视觉一致
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 8, 0);

        cmbETG_Gauss = new JComboBox<>();
        styleCombo(cmbETG_Gauss);
        JPanel dsCard = buildSelectorCard("目标 GaussDB 数据源", cmbETG_Gauss, "GAUSSDB");
        panel.add(dsCard, gbc);

        gbc.gridy = 1;
        // 中间行吸收剩余垂直空间（对齐 Tab1/Tab2：可伸缩内容行在上，按钮行固定在底部）
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);

        tfExcel2 = new JTextField();
        tfExcel2.setEditable(false);
        tfExcel2.setBackground(INPUT_BG);
        tfExcel2.setFont(FONT_FIELD);
        tfExcel2.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JButton btnBrowse2 = btnOutline("浏览...");
        btnBrowse2.setIcon(SvgIconUtils.get("folder-open", 14, PRIMARY));
        btnBrowse2.addActionListener(e -> browseExcel(tfExcel2));

        JPanel fileRow = new JPanel(new BorderLayout(10, 0));
        fileRow.setOpaque(false);
        fileRow.add(tfExcel2, BorderLayout.CENTER);
        fileRow.add(btnBrowse2, BorderLayout.EAST);

        JPanel fileCard = new JPanel(new BorderLayout(0, 8));
        fileCard.setBackground(CARD);
        fileCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));
        fileCard.add(label("选择 Excel 文件", FONT_BOLD, TEXT), BorderLayout.NORTH);
        fileCard.add(fileRow, BorderLayout.CENTER);

        panel.add(fileCard, gbc);

        gbc.gridy = 2;
        // 按钮行 weighty=0 固定在底部（完全对齐 Tab1/Tab2 的按钮行写法），
        // 上方 fileCard(weighty=1.0) 先吸收压缩，按钮不会被底部裁切
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        JButton btn = btnSolid("执行导入", PRIMARY, PRIMARY_H, true);
        btn.setIcon(SvgIconUtils.get("play", 16, Color.WHITE));
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.addActionListener(e -> runE2G());
        btnPanel.add(btn);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // -------------------- 日志面板 --------------------
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
        // 140：给上方 TAB 内容区留更多纵向空间。小屏(如1280x800@150%)下窗口仅~680高，
        // 180 的日志面板会挤掉 TAB 底部按钮（尤其最后两个 Excel 导入页），导致按钮被裁切。
        p.setPreferredSize(new Dimension(0, 140));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(SvgIconUtils.labelWithFont("log", "执行日志", FONT_BOLD, TEXT), BorderLayout.WEST);

        btnStop = new JButton("终止任务");
        btnStop.setIcon(SvgIconUtils.get("stop", 14, Color.WHITE));
        btnStop.setText("终止任务");
        btnStop.setFont(FONT_BTN);
        btnStop.setBackground(DANGER);
        btnStop.setForeground(Color.WHITE);
        btnStop.setOpaque(true);
        btnStop.setBorderPainted(false);
        btnStop.setFocusPainted(false);
        btnStop.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopCurrentTask());
        top.add(btnStop, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setFont(FONT_LOG);
        logArea.setBackground(LOG_BG);
        logArea.setForeground(LOG_FG);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // -------------------- 辅助组件工厂 --------------------
    private JPanel buildSelectorCard(String title, JComboBox<String> cmb, String type) {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(16, 18, 18, 18)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        card.add(label(title, FONT_BOLD, TEXT));
        card.add(Box.createVerticalStrut(4));
        card.add(label("选择已保存的 " + type + " 数据源", FONT_SMALL, TEXT_SEC));
        card.add(Box.createVerticalStrut(6));

        cmb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cmb.setPreferredSize(new Dimension(200, 36));
        card.add(cmb);
        card.add(Box.createVerticalStrut(6));

        JLabel summary = new JLabel(" ");
        summary.setFont(FONT_SMALL);
        summary.setForeground(TEXT_SEC);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(summary);

        cmb.addActionListener(e -> {
            String sel = (String) cmb.getSelectedItem();
            if (sel == null || sel.isEmpty()) {
                summary.setText(" ");
                return;
            }
            DataSource ds = findDataSource(sel);
            if (ds != null) {
                String url = ds.buildUrl();
                if (url.length() > 60) url = url.substring(0, 60) + "...";
                summary.setText("<html><font color=#8899AA>" + url + "  /  " + ds.getUser() + "</font></html>");
            } else {
                summary.setText(" ");
            }
        });
        return card;
    }

    private JButton btnSolid(String text, Color bg, Color hover, boolean wide) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int padV = 12, padH = wide ? 36 : 24;
        b.setBorder(BorderFactory.createEmptyBorder(padV, padH, padV, padH));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private JButton btnOutline(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setForeground(PRIMARY);
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(
            new LineBorder(PRIMARY, 1, true),
            new EmptyBorder(8, 18, 8, 18)
        ));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(238, 242, 255)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(Color.WHITE); }
        });
        return b;
    }

    private JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private void styleCombo(JComboBox<String> cmb) {
        cmb.setFont(FONT_FIELD);
        cmb.setBackground(Color.WHITE);
        cmb.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
    }

    // -------------------- 数据源加载与查找 --------------------
    private void refreshAllCombos() {
        refreshCombo(cmbSrcOra,   "ORACLE");
        refreshCombo(cmbTgtGauss, "GAUSSDB");
        refreshCombo(cmbETO_Ora,  "ORACLE");
        refreshCombo(cmbETG_Gauss,"GAUSSDB");
        refreshCombo(cmbSrcGauss, "GAUSSDB");
        refreshCombo(cmbTgtOra,   "ORACLE");
    }

    private void refreshCombo(JComboBox<String> cmb, String type) {
        if (cmb == null) return;
        cmb.removeAllItems();
        cmb.addItem("");
        List<DataSource> sources = DataSourceStore.load();
        for (DataSource ds : sources) {
            if (ds.getType().equalsIgnoreCase(type)) {
                cmb.addItem(ds.getName());
            }
        }
        if (cmb.getItemCount() > 1) {
            cmb.setSelectedIndex(1);
        }
    }

    private DataSource findDataSource(String name) {
        if (name == null || name.isEmpty()) return null;
        for (DataSource ds : DataSourceStore.load()) {
            if (ds.getName().equals(name)) return ds;
        }
        return null;
    }

    // -------------------- Excel 文件选择 --------------------
    private void browseExcel(JTextField target) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().matches(".*\\.(xlsx|xls|csv)");
            }
            public String getDescription() { return "Excel (*.xlsx, *.xls, *.csv)"; }
        });
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            target.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    // -------------------- 执行逻辑 --------------------
    private void runO2G() {
        DataSource src = getSelected(cmbSrcOra);
        DataSource tgt = getSelected(cmbTgtGauss);
        if (src == null || tgt == null) {
            warn("请选择有效的源端和目标端数据源");
            return;
        }
        String tableSpec = taTableMaps.getText().trim();
        if (tableSpec.isEmpty()) {
            warn("请输入表映射（如: TABLE1,TABLE2 或 OLD:NEW）");
            return;
        }
        List<String> cmd = buildCommand(
            "com.sunzh.sync.OracleToGaussDB",
            src.buildUrl(), src.getUser(), src.getPassword(),
            tgt.buildUrl(), tgt.getUser(), tgt.getPassword(),
            tableSpec
        );
        if (rbAppend.isSelected()) cmd.add("--append");
        launchProcess(cmd);
    }

    private void runG2O() {
        DataSource src = getSelected(cmbSrcGauss);
        DataSource tgt = getSelected(cmbTgtOra);
        if (src == null || tgt == null) {
            warn("请选择有效的源端和目标端数据源");
            return;
        }
        String tableSpec = taTableMapsG2O.getText().trim();
        if (tableSpec.isEmpty()) {
            warn("请输入表映射（如: TABLE1,TABLE2 或 OLD:NEW）");
            return;
        }
        List<String> cmd = buildCommand(
            "com.sunzh.sync.GaussDBToOracle",
            src.buildUrl(), src.getUser(), src.getPassword(),
            tgt.buildUrl(), tgt.getUser(), tgt.getPassword(),
            tableSpec
        );
        if (rbAppendG2O.isSelected()) cmd.add("--append");
        launchProcess(cmd);
    }

    private void runE2O() {
        DataSource tgt = getSelected(cmbETO_Ora);
        if (tgt == null) {
            warn("请选择目标 Oracle 数据源");
            return;
        }
        String excel = tfExcel1.getText().trim();
        if (excel.isEmpty()) {
            warn("请选择 Excel 文件");
            return;
        }
        if (!new File(excel).exists()) {
            warn("文件不存在: " + excel);
            return;
        }
        List<String> cmd = buildCommand(
            "com.sunzh.sync.ExcelToOracle",
            tgt.buildUrl(), tgt.getUser(), tgt.getPassword(),
            excel
        );
        launchProcess(cmd);
    }

    private void runE2G() {
        DataSource tgt = getSelected(cmbETG_Gauss);
        if (tgt == null) {
            warn("请选择目标 GaussDB 数据源");
            return;
        }
        String excel = tfExcel2.getText().trim();
        if (excel.isEmpty()) {
            warn("请选择 Excel 文件");
            return;
        }
        if (!new File(excel).exists()) {
            warn("文件不存在: " + excel);
            return;
        }
        List<String> cmd = buildCommand(
            "com.sunzh.sync.ExcelToGaussDB",
            tgt.buildUrl(), tgt.getUser(), tgt.getPassword(),
            excel
        );
        launchProcess(cmd);
    }

    private DataSource getSelected(JComboBox<String> cmb) {
        String name = (String) cmb.getSelectedItem();
        return findDataSource(name);
    }

    private List<String> buildCommand(String mainClass, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(mainClass);
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    // -------------------- 启动子进程 --------------------
    private void launchProcess(List<String> cmd) {
        if (currentWorker != null && !currentWorker.isDone()) {
            appendLog("[WARN] 已有任务在运行，请等待完成或终止后再启动", LOG_INFO);
            return;
        }

        appendLog("══════════════════════════════════════════════════", LOG_INFO);
        appendLog("  >>> " + String.join(" ", cmd), LOG_INFO);
        appendLog("  >>> " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), LOG_INFO);
        appendLog("══════════════════════════════════════════════════", LOG_INFO);

        btnStop.setEnabled(true);

        currentWorker = new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                currentProcess = pb.start();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(currentProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (isCancelled()) break;
                        publish(line);
                    }
                }
                return currentProcess.waitFor();
            }
            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    Color c = LOG_FG;
                    if (line.contains("ERROR") || line.contains("error"))       c = LOG_ERR;
                    else if (line.contains("WARN") || line.contains("WARNING")) c = WARN;
                    else if (line.contains("成功") || line.contains("完成"))     c = LOG_OK;
                    else if (line.contains(">>>"))                              c = LOG_INFO;
                    appendLog(line, c);
                }
            }
            @Override
            protected void done() {
                btnStop.setEnabled(false);
                currentWorker = null;
                currentProcess = null;
                try {
                    int ec = get();
                    Color c = ec == 0 ? LOG_OK : LOG_ERR;
                    appendLog("──────────────────────────────────────────────────", LOG_INFO);
                    appendLog("  " + (ec == 0 ? "✅ 完成 (exit=0)" : "❌ 退出 (exit=" + ec + ")"), c);
                    appendLog("──────────────────────────────────────────────────", LOG_INFO);
                } catch (CancellationException | InterruptedException e) {
                    appendLog("[INFO] 任务已终止", LOG_INFO);
                } catch (ExecutionException e) {
                    appendLog("[ERROR] " + e.getCause().getMessage(), LOG_ERR);
                }
            }
        };
        currentWorker.execute();
    }

    private void stopCurrentTask() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
        }
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        appendLog("[INFO] 任务已终止", LOG_INFO);
    }

    // -------------------- 日志 --------------------
    private void appendLog(String msg, Color c) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            if (logArea.getLineCount() > 5000) {
                try {
                    int end = logArea.getLineEndOffset(logArea.getLineCount() - 5000);
                    logArea.replaceRange("", 0, end);
                } catch (Exception ignored) {}
            }
        });
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    @Override
    public void refresh() {
        refreshAllCombos();
    }
}