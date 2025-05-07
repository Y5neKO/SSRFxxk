package com.y5neko.burpext.ui.common;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import com.y5neko.burpext.dao.LogDAO;
import com.y5neko.burpext.utils.CharsetUtils;
import com.y5neko.burpext.utils.MiscUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HistoryUI {
    // 主面板
    private final JPanel mainPanel;

    // 用于编辑请求和响应的编辑器
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;

    // 过滤器默认条件
    private String filterUrl = "";
    private String filterHasVul = "全部";
    private String filterDesc = "";
    private String filterTime = "";

    // 用于存储当前选中的行的ID
    // 初始化为null，表示没有选中任何行
    private Integer currentSelectedId = null;


    public HistoryUI(MontoyaApi api, Logging logging) {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // ==================== 第一行：过滤器 ====================
        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.gridx = 0;
        gbcTop.gridy = 0;
        gbcTop.gridwidth = 2;
        gbcTop.weightx = 1.0;
        gbcTop.weighty = 0.0;
        gbcTop.fill = GridBagConstraints.HORIZONTAL;
        gbcTop.anchor = GridBagConstraints.NORTHWEST;
        gbcTop.insets = new Insets(5, 5, 5, 5);
        Button filterButton = new Button("Filter");
        mainPanel.add(filterButton, gbcTop);

        // ==================== 第二行：历史记录表格/请求和响应窗口 ====================
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // 占据整行
        gbc.weightx = 1.0;
        gbc.weighty = 0.5; // 分配剩余空间
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);

        // ========表格========
        // 获取数据库日志列表
        List<LogDAO.LogEntry> logs = LogDAO.getInstance(logging).getAllLogs();
        // 转换成 Object[][] 格式
        Object[][] data = new Object[logs.size()][6];
        for (int i = 0; i < logs.size(); i++) {
            LogDAO.LogEntry entry = logs.get(i);
            data[i][0] = entry.getId();
            data[i][1] = entry.getUrl();
            data[i][2] = entry.isHasVul() ? "是" : "否";
            data[i][3] = String.join(",", entry.getVulParams());
            data[i][4] = entry.getTime();
            data[i][5] = entry.getDescription();
        }
        // 表头
        String[] columnNames = {"ID", "URL", "是否存在漏洞", "漏洞参数", "时间", "描述"};
        // 创建不可编辑的表格
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable historyTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(historyTable);

        // ======== 添加右键菜单 ========
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem deleteItem = new JMenuItem("删除该条日志");
        JMenuItem clearItem = new JMenuItem("清空所有日志");
        JMenuItem detailItem = new JMenuItem("查看漏洞详情");

        popupMenu.add(deleteItem);
        popupMenu.add(clearItem);
        popupMenu.add(detailItem);

        historyTable.setComponentPopupMenu(popupMenu);
        historyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 表格刷新
        // 自动刷新表格每 3 秒
        Timer refreshTimer = new Timer(3000, e -> refreshTable(model, historyTable, logging));
        refreshTimer.start();
        refreshTable(model, historyTable, logging); // 初始加载一次数据

        // =========请求和响应编辑器========
        requestEditor = api.userInterface().createHttpRequestEditor();
        responseEditor = api.userInterface().createHttpResponseEditor();
        //
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("请求"));
        requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
        //
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("响应"));
        responsePanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);
        // ========左右分割面板========
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestPanel, responsePanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        // ========上下分割面板========
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, splitPane);
        verticalSplit.setContinuousLayout(true);

        mainPanel.add(verticalSplit, gbc);


        // ============================================事件监听===============================================
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            // 确保是最终选中状态变化时触发
            if (!e.getValueIsAdjusting()) {
                int selectedRow = historyTable.getSelectedRow();
                if (selectedRow != -1) {
                    Object idObj = historyTable.getValueAt(selectedRow, 0);
                    if (idObj != null) {
                        try {
                            int logId = Integer.parseInt(idObj.toString());

                            // 如果选中的 ID 与当前一样，则不更新（避免刷新触发滚动重置）
                            if (currentSelectedId != null && logId == currentSelectedId) {
                                return;
                            }
                            currentSelectedId = logId;

                            // 设置请求
                            LogDAO.LogEntry entry = LogDAO.getInstance(logging).getLogById(logId);
                            if (entry != null) {
                                HttpRequest httpRequest = HttpRequest.httpRequest(MiscUtils.base64StringToStringAutoDetect(entry.getRequest()));
                                requestEditor.setRequest(httpRequest);

                                if (entry.getResponse() != null) {
                                    String responseStr = MiscUtils.base64StringToStringAutoDetect(entry.getResponse());
                                    ByteArray respByteArray = ByteArray.byteArray(responseStr.getBytes());
                                    HttpResponse httpResponse = HttpResponse.httpResponse(respByteArray);
                                    responseEditor.setResponse(httpResponse);
                                } else {
                                    responseEditor.setResponse(null); // 清空响应
                                }
                            }

                        } catch (Exception ex) {
                            logging.logToError("加载请求/响应失败: " + ex.getMessage());
                            requestEditor.setRequest(HttpRequest.httpRequest(CharsetUtils.bytesToString("Parsing failed, please check the error log.".getBytes(StandardCharsets.UTF_8))));
                            responseEditor.setResponse(HttpResponse.httpResponse(CharsetUtils.bytesToString("Parsing failed, please check the error log.".getBytes(StandardCharsets.UTF_8))));
                        }
                    }
                }

            }
        });
        // 表格右键菜单相关
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            private void showMenu(MouseEvent e) {
                int row = historyTable.rowAtPoint(e.getPoint());
                if (row != -1) {
                    historyTable.setRowSelectionInterval(row, row);
                }
            }
        });
        deleteItem.addActionListener(e -> {
            int[] selectedRows = historyTable.getSelectedRows();
            if (selectedRows.length > 0) {
                int confirm = JOptionPane.showConfirmDialog(mainPanel,
                        "确定删除选中的 " + selectedRows.length + " 条日志？", "确认删除", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        int modelRow = historyTable.convertRowIndexToModel(selectedRows[i]); // 支持排序时的正确索引
                        int id = (int) model.getValueAt(modelRow, 0);
                        LogDAO.getInstance(logging).deleteLogById(id);
                        model.removeRow(modelRow);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel, "未选中任何日志！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        clearItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainPanel, "确定清空所有日志？", "确认", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                LogDAO.getInstance(logging).clearLogs();
                model.setRowCount(0); // 清空表格
                requestEditor.setRequest(null); // 清空请求
                responseEditor.setResponse(null); // 清空响应
            }
        });
        detailItem.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow >= 0) {
                String params = (String) model.getValueAt(selectedRow, 3);
                String desc = (String) model.getValueAt(selectedRow, 5);
                if (params.isEmpty()) params = "无";
                JOptionPane.showMessageDialog(mainPanel,
                        "漏洞参数: " + params + "\n描述: " + desc,
                        "漏洞详情",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
//        filterButton.addActionListener(e -> {
//            JTextField urlField = new JTextField(filterUrl, 20);
//            JComboBox<String> hasVulCombo = new JComboBox<>(new String[]{"全部", "是", "否"});
//            hasVulCombo.setSelectedItem(filterHasVul);
//            JTextField descField = new JTextField(filterDesc, 20);
//            JTextField timeField = new JTextField(filterTime, 20);
//
//            JPanel panel = new JPanel(new GridLayout(0, 2));
//            panel.add(new JLabel("URL 包含:"));
//            panel.add(urlField);
//            panel.add(new JLabel("是否存在漏洞:"));
//            panel.add(hasVulCombo);
//            panel.add(new JLabel("描述 包含:"));
//            panel.add(descField);
//            panel.add(new JLabel("时间 包含:"));
//            panel.add(timeField);
//
//            int result = JOptionPane.showConfirmDialog(mainPanel, panel, "设置过滤条件",
//                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//
//            if (result == JOptionPane.OK_OPTION) {
//                filterUrl = urlField.getText().trim();
//                filterHasVul = Objects.requireNonNull(hasVulCombo.getSelectedItem()).toString();
//                filterDesc = descField.getText().trim();
//                filterTime = timeField.getText().trim();
//                refreshTable((DefaultTableModel) historyTable.getModel(), historyTable, logging);
//            }
//        });
        filterButton.addActionListener(e -> {
            JTextField urlField = new JTextField(filterUrl, 20);
            JComboBox<String> hasVulCombo = new JComboBox<>(new String[]{"全部", "是", "否"});
            hasVulCombo.setSelectedItem(filterHasVul);
            JTextField descField = new JTextField(filterDesc, 20);
            JTextField timeField = new JTextField(filterTime, 20);

            JPanel panel = new JPanel(new BorderLayout());

            JPanel inputPanel = new JPanel(new GridLayout(0, 2));
            inputPanel.add(new JLabel("URL 包含:"));
            inputPanel.add(urlField);
            inputPanel.add(new JLabel("是否存在漏洞:"));
            inputPanel.add(hasVulCombo);
            inputPanel.add(new JLabel("描述 包含:"));
            inputPanel.add(descField);
            inputPanel.add(new JLabel("时间 包含:"));
            inputPanel.add(timeField);

            panel.add(inputPanel, BorderLayout.CENTER);

            // 按钮区
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton resetButton = new JButton("重置");
            buttonPanel.add(resetButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            // 重置按钮动作：清空字段并刷新表格
            resetButton.addActionListener(ae -> {
                filterUrl = "";
                filterHasVul = "全部";
                filterDesc = "";
                filterTime = "";

                refreshTable((DefaultTableModel) historyTable.getModel(), historyTable, logging);
                urlField.setText("");
                hasVulCombo.setSelectedItem("全部");
                descField.setText("");
                timeField.setText("");
            });

            int result = JOptionPane.showConfirmDialog(mainPanel, panel, "设置过滤条件",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                filterUrl = urlField.getText().trim();
                filterHasVul = Objects.requireNonNull(hasVulCombo.getSelectedItem()).toString();
                filterDesc = descField.getText().trim();
                filterTime = timeField.getText().trim();
                refreshTable((DefaultTableModel) historyTable.getModel(), historyTable, logging);
            }
        });

    }

    /**
     * 刷新表格
     * @param model 表格模型
     * @param historyTable 历史记录表格
     * @param logging 日志记录器
     */
    private void refreshTable(DefaultTableModel model, JTable historyTable, Logging logging) {
        int[] selectedRows = historyTable.getSelectedRows();
        List<Integer> selectedIds = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = historyTable.convertRowIndexToModel(selectedRow);
            Object idObj = model.getValueAt(modelRow, 0);
            if (idObj instanceof Integer) {
                selectedIds.add((Integer) idObj);
            }
        }

        List<LogDAO.LogEntry> logs = LogDAO.getInstance(logging).getAllLogs();
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            for (LogDAO.LogEntry entry : logs) {
                // 过滤条件判断
                if (!filterUrl.isEmpty() && !entry.getUrl().contains(filterUrl)) continue;
                if (!filterDesc.isEmpty() && !entry.getDescription().contains(filterDesc)) continue;
                if (!filterTime.isEmpty() && !entry.getTime().contains(filterTime)) continue;
                if (!filterHasVul.equals("全部")) {
                    boolean expected = filterHasVul.equals("是");
                    if (entry.isHasVul() != expected) continue;
                }

                model.addRow(new Object[]{
                        entry.getId(),
                        entry.getUrl(),
                        entry.isHasVul() ? "是" : "否",
                        String.join(",", entry.getVulParams()),
                        entry.getTime(),
                        entry.getDescription()
                });
            }

            ListSelectionModel selectionModel = historyTable.getSelectionModel();
            selectionModel.clearSelection();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object idObj = model.getValueAt(i, 0);
                if (idObj instanceof Integer && selectedIds.contains((Integer) idObj)) {
                    int viewRow = historyTable.convertRowIndexToView(i);
                    historyTable.addRowSelectionInterval(viewRow, viewRow);
                }
            }
        });
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}