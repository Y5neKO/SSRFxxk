package com.y5neko.burpext.ui.listenner;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ActionListenner {
    public static void showEditDialog(DefaultTableModel model, int editRowIndex) {
        while (true) {
            // 创建输入组件
            JTextField keywordField = new JTextField();
            String[] matchOptions = {"包含", "完全匹配", "正则(包含)", "正则(完全匹配)"};
            JComboBox<String> matchTypeCombo = new JComboBox<>(matchOptions);

            if (editRowIndex >= 0) {
                keywordField.setText(model.getValueAt(editRowIndex, 0).toString());
                matchTypeCombo.setSelectedItem(model.getValueAt(editRowIndex, 1).toString());
            }

            // 构建输入面板
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            panel.add(new JLabel("关键字:"));
            panel.add(keywordField);
            panel.add(new JLabel("匹配规则:"));
            panel.add(matchTypeCombo);

            // 显示输入对话框
            int result = JOptionPane.showConfirmDialog(
                    null, panel, editRowIndex < 0 ? "添加关键字" : "编辑关键字",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                break; // 用户取消
            }

            String keyword = keywordField.getText().trim();
            String matchType = (String) matchTypeCombo.getSelectedItem();

            if (keyword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "关键字不能为空", "输入错误", JOptionPane.ERROR_MESSAGE);
                continue; // 重试
            }

            boolean isDuplicate = false;
            for (int i = 0; i < model.getRowCount(); i++) {
                if (i == editRowIndex) continue; // 跳过当前行（编辑模式）
                String existingKeyword = model.getValueAt(i, 0).toString();
                if (existingKeyword.equals(keyword)) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                JOptionPane.showMessageDialog(null, "关键字不能重复", "输入错误", JOptionPane.ERROR_MESSAGE);
                continue; // 重试
            }

            if (editRowIndex >= 0) {
                model.setValueAt(keyword, editRowIndex, 0);
                model.setValueAt(matchType, editRowIndex, 1);
            } else {
                model.addRow(new Object[]{keyword, matchType});
            }

            break; // 成功后退出循环
        }
    }

}
