package com.example.datacheck;

import javax.swing.*;
import java.awt.*;

public class ExecuteBatchDialog extends JDialog {
    public ExecuteBatchDialog(Frame owner) {
        super(owner, "数据质量 - 执行批次", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(owner);
        add(new ExecuteBatchPanel());
    }
}