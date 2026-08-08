package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

/**
 * 数据质量 - 规则配置对话框
 * 统一继承 BaseDialog（Azure Pro 自定义标题栏）
 */
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
