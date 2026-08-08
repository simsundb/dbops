package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

/**
 * 数据质量 - 执行检查和数据清洗对话框
 * 统一继承 BaseDialog（Azure Pro 自定义标题栏）
 */
public class ExecuteBatchDialog extends BaseDialog {

    public ExecuteBatchDialog(JFrame owner) {
        super(owner, "数据质量 - 执行检查和数据清洗", "play");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(new ExecuteBatchPanel(), BorderLayout.CENTER);
    }
}
