package com.sunzh.datacheck;

import com.sunzh.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;

public class ExecuteBatchDialog extends BaseDialog {
    public ExecuteBatchDialog(JFrame owner) {
        super(owner, "数据质量 - 执行批次", "play");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(new ExecuteBatchPanel(), BorderLayout.CENTER);
    }
}
