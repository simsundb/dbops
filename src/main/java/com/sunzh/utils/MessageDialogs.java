package com.sunzh.utils;

import javax.swing.*;
import java.awt.*;

/**
 * 消息弹窗工具 — 给 JOptionPane 弹窗挂上内置 SVG 图标。
 *
 * 之前部分弹窗直接在消息文本里用 emoji（✅ / ❌ / ⚠️）当图标，
 * 本机 macOS 有 Apple Color Emoji 字体能显示，但把 JAR 拷到没有
 * emoji 字体的机器（如部分 Windows / Linux）上会显示成空白方块。
 * 这里改为使用 JAR 内置的 SVG 图标（SvgIconUtils 从 classpath 加载，
 * 任何机器都能渲染），保证跨机器一致。
 */
public final class MessageDialogs {

    private static final int ICON_SIZE = 32;

    private MessageDialogs() {}

    /** 成功弹窗（绿色对勾图标） */
    public static void success(Component parent, String message) {
        show(parent, message, "成功", SvgIconUtils.get("check", ICON_SIZE, ThemeUtils.COLOR_SUCCESS),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** 成功弹窗，自定义标题 */
    public static void success(Component parent, String message, String title) {
        show(parent, message, title, SvgIconUtils.get("check", ICON_SIZE, ThemeUtils.COLOR_SUCCESS),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** 错误弹窗（红色警示图标） */
    public static void error(Component parent, String message) {
        show(parent, message, "错误", SvgIconUtils.get("alert-circle", ICON_SIZE, ThemeUtils.COLOR_DANGER),
                JOptionPane.ERROR_MESSAGE);
    }

    /** 错误弹窗，自定义标题 */
    public static void error(Component parent, String message, String title) {
        show(parent, message, title, SvgIconUtils.get("alert-circle", ICON_SIZE, ThemeUtils.COLOR_DANGER),
                JOptionPane.ERROR_MESSAGE);
    }

    /** 提示弹窗（主题色信息图标） */
    public static void info(Component parent, String message) {
        show(parent, message, "提示", SvgIconUtils.get("info-circle", ICON_SIZE, ThemeUtils.COLOR_PRIMARY),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** 警告弹窗（橙色警示图标） */
    public static void warning(Component parent, String message) {
        show(parent, message, "警告", SvgIconUtils.get("alert-circle", ICON_SIZE, ThemeUtils.COLOR_WARNING),
                JOptionPane.WARNING_MESSAGE);
    }

    private static void show(Component parent, String message, String title, Icon icon, int messageType) {
        JOptionPane.showMessageDialog(parent, message, title, messageType, icon);
    }
}
