package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

public class QualitySummaryDialog extends BaseDialog {
    public QualitySummaryDialog(JFrame owner) {
        super(owner, "数据质量规则引擎", "tool");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        tabs.addTab("1.规则配置", new RuleConfigPanel());
        tabs.addTab("2.生成脚本", new GenerateScriptPanel());
        tabs.addTab("3.执行批次", new ExecuteBatchPanel());
        mainContentPanel.add(tabs, BorderLayout.CENTER);
    }
}
