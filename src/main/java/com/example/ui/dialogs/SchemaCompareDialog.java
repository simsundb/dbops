package com.example.ui.dialogs;

import com.example.utils.SvgIconUtils;
import com.example.ui.BaseDialog;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 结构对比对话框（预留功能）
 */
public class SchemaCompareDialog extends BaseDialog {

    public SchemaCompareDialog(JFrame owner) {
        super(owner, "结构对比", "compare");
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(SvgIconUtils.get("compare", 56, new Color(76, 110, 138)));
        centerPanel.add(iconLabel, new GridBagConstraints());

        JLabel textLabel = new JLabel("结构对比功能开发中...", JLabel.CENTER);
        textLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 22));
        textLabel.setForeground(new Color(150, 150, 150));

        JLabel hintLabel = new JLabel("敬请期待", JLabel.CENTER);
        hintLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        hintLabel.setForeground(new Color(180, 180, 180));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        centerPanel.add(iconLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        centerPanel.add(textLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(hintLabel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // 底部关闭按钮
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton btnClose = new JButton("关闭");
        btnClose.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        btnClose.setBackground(new Color(108, 117, 125));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);

        return panel;
    }
}