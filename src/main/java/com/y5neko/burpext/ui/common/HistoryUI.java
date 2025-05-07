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
import java.awt.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HistoryUI {
    private final JPanel mainPanel;

    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;

    public HistoryUI(MontoyaApi api, Logging logging) {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // ==================== 第一行：过滤器 ====================
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // 占据整行
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        Button filterButton = new Button("Filter");
        mainPanel.add(filterButton, gbc);

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
        JTable historyTable = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(historyTable);

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

                            // 获取对应日志条目
                            LogDAO.LogEntry entry = LogDAO.getInstance(logging).getLogById(logId);
                            if (entry != null) {
                                // 设置请求
                                HttpRequest httpRequest = HttpRequest.httpRequest(MiscUtils.base64StringToStringAutoDetect(entry.getRequest()));
                                requestEditor.setRequest(httpRequest);

                                // 设置响应（可为 null）
                                if (entry.getResponse() != null) {
                                    HttpResponse httpResponse = HttpResponse.httpResponse(MiscUtils.base64StringToStringAutoDetect(entry.getResponse()));
                                    responseEditor.setResponse(httpResponse);
                                } else {
                                    responseEditor.setResponse(null); // 清空
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

    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void showRequest(HttpRequest request) {
        requestEditor.setRequest(request);
    }

    public void showResponse(HttpResponse response) {
        responseEditor.setResponse(response);
    }
}