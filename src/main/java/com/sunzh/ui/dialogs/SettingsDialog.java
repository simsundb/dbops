package com.sunzh.ui.dialogs;

import com.sunzh.utils.SvgIconUtils;
import com.sunzh.ui.BaseDialog;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 系统设置对话框（预留功能）
 */
public class SettingsDialog extends BaseDialog {

    public SettingsDialog(JFrame owner) {
        super(owner, "系统设置", "settings");
    }

    @Override
    protected void initUI() {
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.setBackground(Color.WHITE);
        mainContentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(SvgIconUtils.get("settings", 56, new Color(76, 110, 138)));
        JLabel textLabel = new JLabel("系统设置功能开发中...", JLabel.CENTER);
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

        mainContentPanel.add(centerPanel, BorderLayout.CENTER);
    }
}