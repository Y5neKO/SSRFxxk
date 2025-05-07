package com.y5neko.burpext;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import com.y5neko.burpext.dao.LogDAO;
import com.y5neko.burpext.detector.DigPm;
import com.y5neko.burpext.handlers.YHttpHandler;
import com.y5neko.burpext.ui.MainUI;

import static com.y5neko.burpext.core.Config.EXTENSION_NAME;
import static com.y5neko.burpext.core.Config.LOGO;

public class SSRFxxk implements BurpExtension
{
    Logging logging;

    @Override
    public void initialize(MontoyaApi api)
    {
        // set extension name
        api.extension().setName(EXTENSION_NAME);

        logging = api.logging();

        // write a message to our output stream
        logging.logToOutput("====================");
        logging.logToOutput(LOGO);
        logging.logToOutput("");
        logging.logToOutput("[+]Author: Y5neKO");
        logging.logToOutput("[+]GitHub: https://github.com/Y5neKO");
        logging.logToOutput("====================");

        // 注册组件
        api.http().registerHttpHandler(new YHttpHandler(logging));
        api.userInterface().registerSuiteTab("SSRFxxk", new MainUI(api, logging));

        // 检查dnslog平台状态
        logging.logToOutput("正在检查dnslog平台状态...");
        try {
            if (DigPm.checkDnslogStatus()){
                logging.logToOutput("[+]DigPm状态正常");
            }
        } catch (Exception e) {
            logging.logToError("[-]检查dnslog平台状态失败，可能是网络问题或dnslog平台已关闭");
        }
        logging.logToOutput("====================");
    }
}