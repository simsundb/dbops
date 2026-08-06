package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

public class GenerateScriptDialog extends BaseDialog {
    public GenerateScriptDialog(JFrame owner) {
        super(owner, "数据质量 - 生成检查脚本", "code");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(new GenerateScriptPanel(), BorderLayout.CENTER);
    }
}
