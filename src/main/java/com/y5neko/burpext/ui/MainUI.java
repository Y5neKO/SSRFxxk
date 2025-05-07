package com.y5neko.burpext.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.y5neko.burpext.ui.common.HistoryUI;
import com.y5neko.burpext.ui.setting.MainSettingsUI;

import javax.swing.*;

public class MainUI extends JTabbedPane {
    public MainUI(MontoyaApi api, Logging logging) {
        this.addTab("主设置", new MainSettingsUI().getComponent());
        this.addTab("History", new HistoryUI(api, logging).getComponent());
    }
}
