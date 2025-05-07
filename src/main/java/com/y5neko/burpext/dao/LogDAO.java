package com.y5neko.burpext.dao;

import burp.api.montoya.logging.Logging;
import com.alibaba.fastjson2.JSON;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.y5neko.burpext.core.Config.DB_URL;

public class LogDAO {
    // 单例实例
    private static volatile LogDAO instance;

    // 日志记录器
    private final Logging logging;

    // 私有构造方法
    private LogDAO(Logging logging) {
        this.logging = logging;
        initDatabase();
    }

    // 获取单例实例，双重检查锁
    public static LogDAO getInstance(Logging logging) {
        if (instance == null) {
            synchronized (LogDAO.class) {
                if (instance == null) {
                    instance = new LogDAO(logging);
                }
            }
        }
        return instance;
    }

    /**
     * 初始化数据库
     */
    private void initDatabase() {
        // 注册数据库驱动
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "url TEXT," +
                    "hasVul BOOLEAN," +
                    "vulParams TEXT," +
                    "request TEXT," +
                    "response TEXT," +
                    "time TEXT," +
                    "description TEXT" +
                    ");";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logging.logToError("数据库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 插入日志
     * @param log 日志对象
     */
    public void insertLog(LogEntry log) {
        String sql = "INSERT INTO log (url, hasVul, vulParams, request, response, time, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getUrl());
            pstmt.setBoolean(2, log.isHasVul());
            pstmt.setString(3, JSON.toJSONString(log.getVulParams()));
            pstmt.setString(4, log.getRequest());
            pstmt.setString(5, log.getResponse());
            pstmt.setString(6, log.getTime());
            pstmt.setString(7, log.getDescription());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logging.logToError("日志插入失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取日志
     * @param id 日志ID
     * @return 日志对象
     */
    public LogEntry getLogById(int id) {
        String sql = "SELECT * FROM log WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new LogEntry(
                        rs.getInt("id"),
                        rs.getString("url"),
                        rs.getBoolean("hasVul"),
                        JSON.parseArray(rs.getString("vulParams"), String.class),
                        rs.getString("request"),
                        rs.getString("response"),
                        rs.getString("time"),
                        rs.getString("description")
                );
            }
        } catch (SQLException e) {
            logging.logToError("日志查询失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 根据ID删除日志
     * @param id 日志ID
     */
    public void deleteLogById(int id) {
        String sql = "DELETE FROM log WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logging.logToError("日志删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空日志和主键自增序列
     */
    public void clearLogs() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM log");
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='log'");
        } catch (SQLException e) {
            logging.logToError("日志清空失败: " + e.getMessage());
        }
    }

    /**
     * 正序获取所有日志
     * @return 所有日志列表
     */
    public List<LogEntry> getAllLogs() {
        List<LogEntry> logs = new ArrayList<>();
        String sql = "SELECT * FROM log ORDER BY id";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(new LogEntry(
                        rs.getInt("id"),
                        rs.getString("url"),
                        rs.getBoolean("hasVul"),
                        JSON.parseArray(rs.getString("vulParams"), String.class),
                        rs.getString("request"),
                        rs.getString("response"),
                        rs.getString("time"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            logging.logToError("日志加载失败: " + e.getMessage());
        }
        return logs;
    }


    /**
     * 日志实体类
     */
    public static class LogEntry {
        private final int id;
        private final String url;
        private final boolean hasVul;
        private final List<String> vulParams;
        private final String request;
        private final String response;
        private final String time;
        private final String description;

        public LogEntry(int id, String url, boolean hasVul, List<String> vulParams, String request, String response, String time, String description) {
            this.id = id;
            this.url = url;
            this.hasVul = hasVul;
            this.vulParams = vulParams;
            this.request = request;
            this.response = response;
            this.time = time;
            this.description = description;
        }

        public LogEntry(String url, boolean hasVul, List<String> vulParams, String request, String response, String description) {
            this(0, url, hasVul, vulParams, request, response, getCurrentTime(), description);
        }

        /**
         * 获取当前时间
         * @return 格式化后的当前时间字符串
         */
        private static String getCurrentTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date());
        }

        public int getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public boolean isHasVul() {
            return hasVul;
        }

        public List<String> getVulParams() {
            return vulParams;
        }

        public String getRequest() {
            return request;
        }

        public String getResponse() {
            return response;
        }

        public String getTime() {
            return time;
        }

        public String getDescription() {
            return description;
        }
    }
}
