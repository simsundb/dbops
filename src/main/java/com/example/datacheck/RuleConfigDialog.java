package com.example.datacheck;

import javax.swing.*;
import java.awt.*;

public class RuleConfigDialog extends JDialog {
    public RuleConfigDialog(Frame owner) {
        super(owner, "数据质量 - 规则配置", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(owner);
        add(new RuleConfigPanel());
    }
}