package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

public class RuleConfigDialog extends BaseDialog {
    public RuleConfigDialog(JFrame owner) {
        super(owner, "数据质量 - 规则配置", "settings");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(new RuleConfigPanel(), BorderLayout.CENTER);
    }
}
