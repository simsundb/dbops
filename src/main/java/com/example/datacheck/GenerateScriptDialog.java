package com.example.datacheck;

import javax.swing.*;
import java.awt.*;

public class GenerateScriptDialog extends JDialog {
    public GenerateScriptDialog(Frame owner) {
        super(owner, "数据质量 - 生成检查脚本", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(owner);
        add(new GenerateScriptPanel());
    }
}