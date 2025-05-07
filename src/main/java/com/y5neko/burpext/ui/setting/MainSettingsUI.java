package com.y5neko.burpext.ui.setting;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.y5neko.burpext.utils.FileUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;

import static com.y5neko.burpext.core.Config.configFilePath;
import static com.y5neko.burpext.ui.listenner.ActionListenner.showEditDialog;

public class MainSettingsUI {
    private final JPanel mainPanel;

    List<JCheckBox> enabledCompoentsList = new ArrayList<>();
    List<JCheckBox> enabledDetectionMethodsList = new ArrayList<>();

    public MainSettingsUI() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.insets = new Insets(5, 5, 5, 5);  // 组件间距（上，左，下，右）
        mainGbc.fill = GridBagConstraints.HORIZONTAL; // 默认填充方式

        // ========================= 标题 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER;
        mainGbc.anchor = GridBagConstraints.CENTER;
        mainGbc.weightx = 1.0;
        JLabel titleLabel = new JLabel("SSRFxxk设置", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        mainPanel.add(titleLabel, mainGbc);

        // ========================= 选择生效组件 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 1;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER; // 跨全部列
        mainGbc.anchor = GridBagConstraints.CENTER;       // 整体居中
        JPanel burpCheckboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        burpCheckboxPanel.setBorder(BorderFactory.createTitledBorder("Burp Suite组件"));
        // 常见Burp组件列表
        String[] burpComponents = {
                "启用插件", "Proxy", "Repeater"
        };
        // 创建纯文本复选框
        for (String component : burpComponents) {
            JCheckBox checkBox = new JCheckBox(component);
            if (component.equals("启用插件")) {
                checkBox.setSelected(true);
            } else {
                checkBox.setFocusPainted(false);
            }
            burpCheckboxPanel.add(checkBox);
            enabledCompoentsList.add(checkBox);
        }
        mainPanel.add(burpCheckboxPanel, mainGbc);

        // ========================= 选择检测模式 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 2;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER; // 跨全部列
        mainGbc.anchor = GridBagConstraints.CENTER;       // 整体居中
        JPanel modeRadioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        modeRadioPanel.setBorder(BorderFactory.createTitledBorder("检测模式"));
        // 常见检测模式列表
        String[] detectionModes = {
                "参数关键字", "暴力模式（谨慎开启）"
        };
        ButtonGroup detectionModesGroup = new ButtonGroup();
        for (String opt : detectionModes) {
            JRadioButton radioButton = new JRadioButton(opt);
            if (opt.equals("参数关键字")) {
                radioButton.setSelected(true);
            } else {
                radioButton.setToolTipText("暴力模式会强行对所有参数进行检测，流量非常大，在授权状态下可能导致污染业务数据，谨慎开启！！！\n暴力模式对Dnslog平台压力也比较大，容易被Dnslog平台限流导致误报，强烈建议自行构建相关参数字典再进行检测");
                radioButton.setFocusPainted(false);
            }
            detectionModesGroup.add(radioButton);
            modeRadioPanel.add(radioButton);
        }
        mainPanel.add(modeRadioPanel, mainGbc);

        // ========================= 白名单 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 3;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER;
        mainGbc.anchor = GridBagConstraints.CENTER;
        JPanel whiteListPanel = new JPanel(new BorderLayout());
        whiteListPanel.setBorder(BorderFactory.createTitledBorder("白名单域名/地址"));
        whiteListPanel.setMinimumSize(new Dimension(300, 100));
        // 白名单文本域
        JTextArea whiteListArea = new JTextArea(5, 40);
        whiteListArea.setEditable(true);
        whiteListArea.setLineWrap(true);
        whiteListArea.setWrapStyleWord(true);
        whiteListArea.setToolTipText("每行一个域名或地址，支持通配符*\n不填默认全部处理");
        // 将文本域放入滚动面板
        JScrollPane scrollPane = new JScrollPane(whiteListArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // 确保垂直滚动条始终存在
        whiteListPanel.add(scrollPane, BorderLayout.CENTER); // 滚动面板填满白名单区域
        mainPanel.add(whiteListPanel, mainGbc);

        // ========================= 表格 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 4;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER; // 跨全部列
        mainGbc.anchor = GridBagConstraints.CENTER;       // 整体居中
        // 换乱的话记得恢复约束
        mainGbc.fill = GridBagConstraints.BOTH;
        mainGbc.weightx = 1.0;
        mainGbc.weighty = 1.0;
        // 这一行的根容器
        JPanel tableContainer = new JPanel(new GridBagLayout());
        GridBagConstraints tableGbc = new GridBagConstraints();
        tableGbc.fill = GridBagConstraints.BOTH;
        tableGbc.weightx = 1.0; // 每列水平扩展
        tableGbc.weighty = 1.0; // 垂直填满
        // =====================第一列：参数===================
        // 创建参数相关按钮
        JButton paramsAddButton = new JButton("添加");
        JButton paramsEditButton = new JButton("编辑");
        JButton paramsDeleteButton = new JButton("删除");
        JButton paramsClearButton = new JButton("清空");
        // 创建参数相关按钮面板
        JPanel paramsButtonPanel = new JPanel();
        paramsButtonPanel.setLayout(new BoxLayout(paramsButtonPanel, BoxLayout.Y_AXIS));
        paramsButtonPanel.add(Box.createVerticalGlue());
        paramsButtonPanel.add(paramsAddButton);
        paramsButtonPanel.add(Box.createVerticalStrut(15));
        paramsButtonPanel.add(paramsEditButton);
        paramsButtonPanel.add(Box.createVerticalStrut(15));
        paramsButtonPanel.add(paramsDeleteButton);
        paramsButtonPanel.add(Box.createVerticalStrut(15));
        paramsButtonPanel.add(paramsClearButton);
        paramsButtonPanel.add(Box.createVerticalGlue());
        // 创建参数相关表格
        DefaultTableModel paramsModel = new DefaultTableModel(new String[]{"关键字", "匹配规则"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable paramsTable = new JTable(paramsModel);
        JScrollPane paramsScrollPane = new JScrollPane(paramsTable);
        paramsScrollPane.setMinimumSize(new Dimension(150, 100));
//        paramsButtonPanel.setPreferredSize(new Dimension(200, 120));
        // 创建参数相关列容器
        JPanel paramsColumnPanel = new JPanel();
        paramsColumnPanel.setLayout(new BorderLayout());
        paramsColumnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        paramsColumnPanel.add(paramsButtonPanel, BorderLayout.WEST);
        paramsColumnPanel.add(Box.createHorizontalStrut(5));
        paramsColumnPanel.add(paramsScrollPane, BorderLayout.CENTER);
        paramsColumnPanel.setBorder(BorderFactory.createTitledBorder("参数关键字"));
        // 添加到表格容器
        tableGbc.gridx = 0;
        tableGbc.gridy = 0;
        tableContainer.add(paramsColumnPanel, tableGbc);
        // =====================第二列：请求头===================
        // 创建请求头相关按钮
        JButton headersAddButton = new JButton("添加");
        JButton headersEditButton = new JButton("编辑");
        JButton headersDeleteButton = new JButton("删除");
        JButton headersClearButton = new JButton("清空");
        // 创建请求头相关按钮面板
        JPanel headersButtonPanel = new JPanel();
        headersButtonPanel.setLayout(new BoxLayout(headersButtonPanel, BoxLayout.Y_AXIS));
        headersButtonPanel.add(Box.createVerticalGlue());
        headersButtonPanel.add(headersAddButton);
        headersButtonPanel.add(Box.createVerticalStrut(15));
        headersButtonPanel.add(headersEditButton);
        headersButtonPanel.add(Box.createVerticalStrut(15));
        headersButtonPanel.add(headersDeleteButton);
        headersButtonPanel.add(Box.createVerticalStrut(15));
        headersButtonPanel.add(headersClearButton);
        headersButtonPanel.add(Box.createVerticalGlue());
        // 创建请求头相关表格
        DefaultTableModel headersModel = new DefaultTableModel(new String[]{"关键字", "匹配规则"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable headersTable = new JTable(headersModel);
        JScrollPane headersScrollPane = new JScrollPane(headersTable);
        headersScrollPane.setMinimumSize(new Dimension(150, 100));
//        headersButtonPanel.setPreferredSize(new Dimension(200, 120));
        // 创建请求头相关列容器
        JPanel headersColumnPanel = new JPanel();
        headersColumnPanel.setLayout(new BorderLayout());
        headersColumnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        headersColumnPanel.add(headersButtonPanel, BorderLayout.WEST);
        headersColumnPanel.add(Box.createHorizontalStrut(5));
        headersColumnPanel.add(headersScrollPane, BorderLayout.CENTER);
        headersColumnPanel.setBorder(BorderFactory.createTitledBorder("请求头关键字"));
        // 添加到表格容器
        tableGbc.gridx = 1;
        tableGbc.gridy = 0;
        tableContainer.add(headersColumnPanel, tableGbc);
        // =====================第三列：响应头===================
        // 创建响应头相关按钮
        JButton responseAddButton = new JButton("添加");
        JButton responseEditButton = new JButton("编辑");
        JButton responseDeleteButton = new JButton("删除");
        JButton responseClearButton = new JButton("清空");
        // 创建响应头相关按钮面板
        JPanel responseButtonPanel = new JPanel();
        responseButtonPanel.setLayout(new BoxLayout(responseButtonPanel, BoxLayout.Y_AXIS));
        responseButtonPanel.add(Box.createVerticalGlue());
        responseButtonPanel.add(responseAddButton);
        responseButtonPanel.add(Box.createVerticalStrut(15));
        responseButtonPanel.add(responseEditButton);
        responseButtonPanel.add(Box.createVerticalStrut(15));
        responseButtonPanel.add(responseDeleteButton);
        responseButtonPanel.add(Box.createVerticalStrut(15));
        responseButtonPanel.add(responseClearButton);
        responseButtonPanel.add(Box.createVerticalGlue());
        // 创建响应头相关表格
        DefaultTableModel responseModel = new DefaultTableModel(new String[]{"关键字", "匹配规则"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable responseTable = new JTable(responseModel);
        JScrollPane responseScrollPane = new JScrollPane(responseTable);
        responseScrollPane.setMinimumSize(new Dimension(150, 100));
//        responseButtonPanel.setPreferredSize(new Dimension(200, 120));
        // 创建响应头相关列容器
        JPanel responseColumnPanel = new JPanel();
        responseColumnPanel.setLayout(new BorderLayout());
        responseColumnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        responseColumnPanel.add(responseButtonPanel, BorderLayout.WEST);
        responseColumnPanel.add(Box.createHorizontalStrut(5));
        responseColumnPanel.add(responseScrollPane, BorderLayout.CENTER);
        responseColumnPanel.setBorder(BorderFactory.createTitledBorder("响应关键字"));
        // 添加到表格容器
        tableGbc.gridx = 2;
        tableGbc.gridy = 0;
        tableContainer.add(responseColumnPanel, tableGbc);
        // ====================添加表格根容器=================
        mainPanel.add(tableContainer, mainGbc);

        // ========================= 检测方式 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 5;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER; // 跨全部列
        mainGbc.anchor = GridBagConstraints.CENTER;       // 整体居中
        mainGbc.weightx = 0;
        mainGbc.weighty = 0;
        JPanel detectionMethodPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        detectionMethodPanel.setBorder(BorderFactory.createTitledBorder("检测方式"));
        // 常见检测模式列表
        String[] detectionMethods = {
                "DigPm", "响应关键字"
        };
        for (String opt : detectionMethods) {
            JCheckBox checkBox = new JCheckBox(opt);
            if (opt.equals("DigPm")) {
                checkBox.setSelected(true);
            } else {
                checkBox.setFocusPainted(false);
            }
            checkBox.setToolTipText("Dnslog平台针对无回显，响应关键字针对有回显");
            detectionMethodPanel.add(checkBox);
            enabledDetectionMethodsList.add(checkBox);
        }
        mainPanel.add(detectionMethodPanel, mainGbc);

        // ========================= 底部按钮 =====================
        mainGbc.gridx = 0;
        mainGbc.gridy = 6;
        mainGbc.gridwidth = GridBagConstraints.REMAINDER; // 跨全部列
        mainGbc.anchor = GridBagConstraints.CENTER;       // 整体居中
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton saveButton = new JButton("保存");
        JButton cancleButton = new JButton("取消");
        JButton resetButton = new JButton("恢复默认");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancleButton);
        buttonPanel.add(resetButton);

        mainPanel.add(buttonPanel, mainGbc);

        // ================================================== 事件监听 ==================================================
        // ==================参数关键字相关================
        paramsAddButton.addActionListener(e -> showEditDialog(paramsModel, -1));
        paramsEditButton.addActionListener(e -> {
            int selectedRow = paramsTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditDialog(paramsModel, selectedRow);
            } else {
                JOptionPane.showMessageDialog(null, "请先选择要编辑的行", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        paramsDeleteButton.addActionListener(e -> {
            // 获取选中的行（支持多选）
            int[] selectedRows = paramsTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "请至少选择一行数据进行删除",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 确认对话框
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要删除选中的 " + selectedRows.length + " 项数据吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // 倒序删除避免索引变化
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    paramsModel.removeRow(selectedRows[i]);
                }
            }
        });
        paramsClearButton.addActionListener(e -> {
            // 空数据检查
            if (paramsModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "表格中暂无数据可清空",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 二次确认
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要清空所有数据吗？该操作不可恢复！",
                    "危险操作确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                paramsModel.setRowCount(0);
            }
        });
        paramsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    paramsEditButton.doClick();
                }
            }
        });
        // ==================请求头关键字相关================
        headersAddButton.addActionListener(e -> showEditDialog(headersModel, -1));
        headersEditButton.addActionListener(e -> {
            int selectedRow = headersTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditDialog(headersModel, selectedRow);
            } else {
                JOptionPane.showMessageDialog(null, "请先选择要编辑的行", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        headersDeleteButton.addActionListener(e -> {
            // 获取选中的行（支持多选）
            int[] selectedRows = headersTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "请至少选择一行数据进行删除",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 确认对话框
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要删除选中的 " + selectedRows.length + " 项数据吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // 倒序删除避免索引变化
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    headersModel.removeRow(selectedRows[i]);
                }
            }
        });
        headersClearButton.addActionListener(e -> {
            // 空数据检查
            if (headersModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "表格中暂无数据可清空",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 二次确认
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要清空所有数据吗？该操作不可恢复！",
                    "危险操作确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                headersModel.setRowCount(0);
            }
        });
        headersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    headersEditButton.doClick();
                }
            }
        });
        // ==================响应头关键字相关================
        responseAddButton.addActionListener(e -> showEditDialog(responseModel, -1));
        responseEditButton.addActionListener(e -> {
            int selectedRow = responseTable.getSelectedRow();
            if (selectedRow >= 0) {
                showEditDialog(responseModel, selectedRow);
            } else {
                JOptionPane.showMessageDialog(null, "请先选择要编辑的行", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        responseDeleteButton.addActionListener(e -> {
            // 获取选中的行（支持多选）
            int[] selectedRows = responseTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "请至少选择一行数据进行删除",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 确认对话框
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要删除选中的 " + selectedRows.length + " 项数据吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                // 倒序删除避免索引变化
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    responseModel.removeRow(selectedRows[i]);
                }
            }
        });
        responseClearButton.addActionListener(e -> {
            // 空数据检查
            if (responseModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                        "表格中暂无数据可清空",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // 二次确认
            int confirm = JOptionPane.showConfirmDialog(
                    mainPanel,
                    "确定要清空所有数据吗？该操作不可恢复！",
                    "危险操作确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                responseModel.setRowCount(0);
            }
        });
        responseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    responseEditButton.doClick();
                }
            }
        });
        // ==================保存按钮================
        saveButton.addActionListener(e -> {
            Map<String, Object> config = new HashMap<>();
            // 保存Burp组件状态
            List<String> enabledBurpComponents = new ArrayList<>();
            for (JCheckBox cb : enabledCompoentsList) {
                if (cb.isSelected()) {
                    enabledBurpComponents.add(cb.getText());
                }
            }
            config.put("enabledBurpComponents", enabledBurpComponents);
            // 保存检测模式
            String selectedMode = null;
            for (Enumeration<AbstractButton> buttons = detectionModesGroup.getElements(); buttons.hasMoreElements(); ) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    selectedMode = button.getText();
                    break;
                }
            }
            config.put("detectionMode", selectedMode);
            // 保存检测方式
            List<String> selectedDetectionMethods = new ArrayList<>();
            for (JCheckBox cb : enabledDetectionMethodsList) {
                if (cb.isSelected()) {
                    selectedDetectionMethods.add(cb.getText());
                }
            }
            config.put("enabledDetectionMethods", selectedDetectionMethods);
            // 保存白名单
            String whiteListText = whiteListArea.getText().trim();
            String[] whiteList = whiteListText.split("\n");
            if (whiteList.length > 0) {
                config.put("whiteList", whiteList);
            } else {
                config.put("whiteList", new String[]{});
            }
            // 保存参数关键字
            List<Map<String, String>> params = new ArrayList<>();
            Set<String> keywordSet = new HashSet<>(); // 可选：用于关键词去重
            for (int i = 0; i < paramsModel.getRowCount(); i++) {
                Object keywordObj = paramsModel.getValueAt(i, 0);
                Object ruleObj = paramsModel.getValueAt(i, 1);

                if (keywordObj != null && ruleObj != null) {
                    String keyword = keywordObj.toString().trim();
                    String rule = ruleObj.toString().trim();

                    if (!keyword.isEmpty() && keywordSet.add(keyword)) { // 去重 + 非空校验
                        Map<String, String> param = new HashMap<>();
                        param.put("keyword", keyword);
                        param.put("rule", rule);
                        params.add(param);
                    }
                }
            }
            config.put("params", params);
            // 保存请求头关键字
            List<Map<String, String>> headers = new ArrayList<>();
            Set<String> headerSet = new HashSet<>(); // 可选：用于关键词去重
            for (int i = 0; i < headersModel.getRowCount(); i++) {
                Object keywordObj = headersModel.getValueAt(i, 0);
                Object ruleObj = headersModel.getValueAt(i, 1);
                if (keywordObj!= null && ruleObj!= null) {
                    String keyword = keywordObj.toString().trim();
                    String rule = ruleObj.toString().trim();
                    if (!keyword.isEmpty() && headerSet.add(keyword)) { // 去重 + 非空校验
                        Map<String, String> param = new HashMap<>();
                        param.put("keyword", keyword);
                        param.put("rule", rule);
                        headers.add(param);
                    }
                }
            }
            config.put("headers", headers);
            // 保存响应头关键字
            List<Map<String, String>> response = new ArrayList<>();
            Set<String> responseSet = new HashSet<>(); // 可选：用于关键词去重
            for (int i = 0; i < responseModel.getRowCount(); i++) {
                Object keywordObj = responseModel.getValueAt(i, 0);
                Object ruleObj = responseModel.getValueAt(i, 1);
                if (keywordObj!= null && ruleObj!= null) {
                    String keyword = keywordObj.toString().trim();
                    String rule = ruleObj.toString().trim();
                    if (!keyword.isEmpty() && responseSet.add(keyword)) { // 去重 + 非空校验
                        Map<String, String> param = new HashMap<>();
                        param.put("keyword", keyword);
                        param.put("rule", rule);
                        response.add(param);
                    }
                }
            }
            config.put("response", response);
            // fastjson2保存配置
            try {
                File configFile = new File(configFilePath);
                if (!configFile.exists()) {
                    File parentDir = configFile.getParentFile();
                    if (!parentDir.exists()) {
                        System.out.println(parentDir.mkdirs());
                    }
                }
                // 写入文件
                try (FileWriter fileWriter = new FileWriter(configFile)) {
                    fileWriter.write(JSON.toJSONString(config, JSONWriter.Feature.PrettyFormat));
                    JOptionPane.showMessageDialog(mainPanel,
                            "配置文件写入成功",
                            "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                        "配置文件写入失败",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        // ==================取消按钮================
        cancleButton.addActionListener(e -> {
            // 读取配置文件
            try {
                JSONObject configJsonObject = JSON.parseObject(FileUtils.readFileToString(configFilePath));
                // 读取Burp组件状态
                List<String> enabledBurpComponents = configJsonObject.getJSONArray("enabledBurpComponents").toJavaList(String.class);
                for (JCheckBox cb : enabledCompoentsList) {
                    cb.setSelected(enabledBurpComponents.contains(cb.getText()));
                }
                // 读取检测模式
                String selectedMode = configJsonObject.getString("detectionMode");
                for (Enumeration<AbstractButton> buttons = detectionModesGroup.getElements(); buttons.hasMoreElements(); ) {
                    AbstractButton button = buttons.nextElement();
                    if (button.getText().equals(selectedMode)) {
                        button.setSelected(true);
                        break;
                    } else {
                        button.setSelected(false);
                    }
                }
                // 读取白名单
                String[] whiteList = configJsonObject.getObject("whiteList", String[].class);
                if (whiteList.length > 0) {
                    StringBuilder whiteListText = new StringBuilder();
                    for (String s : whiteList) {
                        whiteListText.append(s).append("\n");
                    }
                    whiteListArea.setText(whiteListText.toString());
                } else {
                    whiteListArea.setText("");
                }
                // 读取检测方式
                List<String> selectedDetectionMethods = configJsonObject.getJSONArray("enabledDetectionMethods").toJavaList(String.class);
                for (JCheckBox cb : enabledDetectionMethodsList) {
                    cb.setSelected(selectedDetectionMethods.contains(cb.getText()));
                }
                // 读取参数关键字
                JSONArray paramsArray = configJsonObject.getJSONArray("params");
                paramsModel.setRowCount(0);
                for (int i = 0; i < paramsArray.size(); i++) {
                    JSONObject paramObj = paramsArray.getJSONObject(i);
                    String keyword = paramObj.getString("keyword");
                    String rule = paramObj.getString("rule");
                    paramsModel.addRow(new Object[]{keyword, rule});
                }
                // 读取请求头关键字
                JSONArray headersArray = configJsonObject.getJSONArray("headers");
                headersModel.setRowCount(0);
                for (int i = 0; i < headersArray.size(); i++) {
                    JSONObject paramObj = headersArray.getJSONObject(i);
                    String keyword = paramObj.getString("keyword");
                    String rule = paramObj.getString("rule");
                    headersModel.addRow(new Object[]{keyword, rule});
                }
                // 读取响应头关键字
                JSONArray responseArray = configJsonObject.getJSONArray("response");
                responseModel.setRowCount(0);
                for (int i = 0; i < responseArray.size(); i++) {
                    JSONObject paramObj = responseArray.getJSONObject(i);
                    String keyword = paramObj.getString("keyword");
                    String rule = paramObj.getString("rule");
                    responseModel.addRow(new Object[]{keyword, rule});
                }
            } catch (Exception ex) {
                System.out.println("配置文件不存在或读取失败");
            }
        });
        // ==================恢复默认按钮================
        resetButton.addActionListener(e -> {
            for (JCheckBox cb : enabledCompoentsList) {
                cb.setSelected(cb.getText().equals("启用插件"));
            }
            for (Enumeration<AbstractButton> buttons = detectionModesGroup.getElements(); buttons.hasMoreElements(); ) {
                AbstractButton button = buttons.nextElement();
                if (button.getText().equals("参数关键字")) {
                    button.setSelected(true);
                    break;
                } else {
                    button.setSelected(false);
                }
            }
            whiteListArea.setText("");
            for (JCheckBox cb : enabledDetectionMethodsList) {
                cb.setSelected(cb.getText().equals("DigPm"));
            }
            paramsModel.setRowCount(0);
            headersModel.setRowCount(0);
            responseModel.setRowCount(0);
        });

        // ==================================================== 初始化 ==========================================
        // 如果存在配置文件，先读取并还原
        if (new File(configFilePath).exists()) {
            cancleButton.doClick();
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
