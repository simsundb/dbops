package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

/**
 * 数据质量 - 生成检查与清洗脚本对话框
 * 统一继承 BaseDialog（Azure Pro 自定义标题栏）
 */
public class GenerateScriptDialog extends BaseDialog {

    public GenerateScriptDialog(JFrame owner) {
        super(owner, "数据质量 - 生成检查和数据清洗脚本", "file-code");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(new GenerateScriptPanel(), BorderLayout.CENTER);
    }
}
