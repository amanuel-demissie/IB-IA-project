package com.javafx.demo.service;

import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.db.Database;
import com.javafx.demo.model.Product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProductDao productDao = new ProductDao();

    public Path generateTodayCsvReport() {
        return generateCsvReportForDate(LocalDate.now());
    }

    public Path generateCsvReportForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMAT);
        String home = System.getProperty("user.home");
        Path dir = Paths.get(home, "FactoryReports");
        Path file = dir.resolve("report-" + dateStr + ".csv");

        try {
            Files.createDirectories(dir);
            String csvContent = buildCsvForDate(date);
            Files.writeString(file, csvContent, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write report: " + e.getMessage(), e);
        }
    }

    public String buildCsvForDate(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        String dateStr = date.format(DATE_FORMAT);
        sb.append("Factory Inventory Daily Report,").append(dateStr).append("\n");

        try (Connection c = Database.getConnection()) {
            // Totals
            int totalCheckIns = countByActionOnDate(c, "CHECK_IN", date);
            int totalCheckOuts = countByActionOnDate(c, "CHECK_OUT", date);
            int unresolvedAlerts = countUnresolvedAlerts(c);
            int alertsToday = countAlertsOnDate(c, date);

            sb.append("Total Check-Ins,").append(totalCheckIns).append("\n");
            sb.append("Total Check-Outs,").append(totalCheckOuts).append("\n");
            sb.append("Unresolved Alerts,").append(unresolvedAlerts).append("\n");
            sb.append("Alerts Created Today,").append(alertsToday).append("\n");
            sb.append("\n");

            // Per-product aggregates for the day
            Map<Integer, Integer> insPerProduct = sumQuantityByProductAndActionOnDate(c, "CHECK_IN", date);
            Map<Integer, Integer> outsPerProduct = sumQuantityByProductAndActionOnDate(c, "CHECK_OUT", date);

            sb.append("Per Product Summary\n");
            sb.append("Product ID,Product Name,Unit,Check-Ins,Check-Outs,Net Change\n");
            for (Integer productId : unionKeys(insPerProduct, outsPerProduct).keySet()) {
                int ins = insPerProduct.getOrDefault(productId, 0);
                int outs = outsPerProduct.getOrDefault(productId, 0);
                int net = ins - outs;
                var productOpt = productDao.findById(productId);
                String productName = productOpt.map(Product::name).orElse("Product " + productId);
                String unit = productOpt.map(Product::unit).orElse("");
                sb.append(productId).append(",")
                  .append(escape(productName)).append(",")
                  .append(escape(unit)).append(",")
                  .append(ins).append(",")
                  .append(outs).append(",")
                  .append(net).append("\n");
            }
            sb.append("\n");

            // List unresolved alerts
            sb.append("Unresolved Alerts\n");
            sb.append("Alert ID,Product ID,Product Name,Alert Type,Created At,Message\n");
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT id, product_id, alert_type, created_at, message
                FROM alerts
                WHERE status = 'UNRESOLVED'
                ORDER BY created_at DESC
                """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int alertId = rs.getInt("id");
                        int pid = rs.getInt("product_id");
                        String type = rs.getString("alert_type");
                        String createdAt = rs.getTimestamp("created_at").toString();
                        String message = rs.getString("message");
                        String pname = productDao.findById(pid).map(Product::name).orElse("Product " + pid);
                        sb.append(alertId).append(",")
                          .append(pid).append(",")
                          .append(escape(pname)).append(",")
                          .append(escape(type)).append(",")
                          .append(escape(createdAt)).append(",")
                          .append(escape(message)).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build report: " + e.getMessage(), e);
        }

        return sb.toString();
    }

    public String buildHtmlForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMAT);
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='utf-8'/>");
        html.append("<style>")
            // Page and typography
            .append("@page{size:landscape;margin:18mm;}body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:12px;color:#111827;margin:0;}")
            .append("h1{font-size:18px;margin:0 0 8px 0;} .muted{color:#6b7280}")
            // Tables
            .append("table{width:100%;border-collapse:collapse;margin-top:8px;table-layout:fixed;}")
            .append("thead{display:table-header-group;} tr,td,th{page-break-inside:avoid;}")
            .append("th,td{border:1px solid #e5e7eb;padding:6px 8px;vertical-align:top;white-space:normal;word-break:break-word;overflow-wrap:anywhere;}")
            .append("th{background:#f3f4f6;text-align:left;}")
            .append("td.num{text-align:right;font-variant-numeric:tabular-nums;}")
            .append(".section{margin:16px 0 0 0;}")
            // Column widths for summary (6 columns)
            .append(".table-summary colgroup col:nth-child(1){width:10%;}")
            .append(".table-summary colgroup col:nth-child(2){width:34%;}")
            .append(".table-summary colgroup col:nth-child(3){width:10%;}")
            .append(".table-summary colgroup col:nth-child(4){width:15%;}")
            .append(".table-summary colgroup col:nth-child(5){width:15%;}")
            .append(".table-summary colgroup col:nth-child(6){width:16%;}")
            // Column widths for alerts (6 columns)
            .append(".table-alerts colgroup col:nth-child(1){width:7%;}")
            .append(".table-alerts colgroup col:nth-child(2){width:9%;}")
            .append(".table-alerts colgroup col:nth-child(3){width:18%;}")
            .append(".table-alerts colgroup col:nth-child(4){width:18%;}")
            .append(".table-alerts colgroup col:nth-child(5){width:18%;}")
            .append(".table-alerts colgroup col:nth-child(6){width:30%;}")
            .append("</style></head><body>");
        html.append("<h1>Factory Inventory Daily Report</h1>");
        html.append("<div class='muted'>").append(dateStr).append("</div>");
        try (Connection c = Database.getConnection()) {
            int totalCheckIns = countByActionOnDate(c, "CHECK_IN", date);
            int totalCheckOuts = countByActionOnDate(c, "CHECK_OUT", date);
            int unresolvedAlerts = countUnresolvedAlerts(c);
            int alertsToday = countAlertsOnDate(c, date);
            html.append("<div class='section'><table>")
                .append("<tr><th>Total Check-Ins</th><td class='num'>").append(totalCheckIns).append("</td></tr>")
                .append("<tr><th>Total Check-Outs</th><td class='num'>").append(totalCheckOuts).append("</td></tr>")
                .append("<tr><th>Unresolved Alerts</th><td class='num'>").append(unresolvedAlerts).append("</td></tr>")
                .append("<tr><th>Alerts Created Today</th><td class='num'>").append(alertsToday).append("</td></tr>")
                .append("</table></div>");

            Map<Integer, Integer> insPerProduct = sumQuantityByProductAndActionOnDate(c, "CHECK_IN", date);
            Map<Integer, Integer> outsPerProduct = sumQuantityByProductAndActionOnDate(c, "CHECK_OUT", date);
            html.append("<div class='section'><h3>Per Product Summary</h3><table class='table-summary'>")
                .append("<colgroup><col/><col/><col/><col/><col/><col/></colgroup>")
                .append("<thead><tr><th>Product ID</th><th>Product Name</th><th>Unit</th><th class='num'>Check-Ins</th><th class='num'>Check-Outs</th><th class='num'>Net Change</th></tr></thead><tbody>");
            for (Integer productId : unionKeys(insPerProduct, outsPerProduct).keySet()) {
                int ins = insPerProduct.getOrDefault(productId, 0);
                int outs = outsPerProduct.getOrDefault(productId, 0);
                int net = ins - outs;
                var pOpt = productDao.findById(productId);
                String name = pOpt.map(Product::name).orElse("Product " + productId);
                String unit = pOpt.map(Product::unit).orElse("");
                html.append("<tr>")
                    .append("<td>").append(productId).append("</td>")
                    .append("<td>").append(escapeHtml(name)).append("</td>")
                    .append("<td>").append(escapeHtml(unit)).append("</td>")
                    .append("<td class='num'>").append(ins).append("</td>")
                    .append("<td class='num'>").append(outs).append("</td>")
                    .append("<td class='num'>").append(net).append("</td>")
                    .append("</tr>");
            }
            html.append("</tbody></table></div>");

            html.append("<div class='section'><h3>Unresolved Alerts</h3><table class='table-alerts'>")
                .append("<colgroup><col/><col/><col/><col/><col/><col/></colgroup>")
                .append("<thead><tr><th>Alert ID</th><th>Product ID</th><th>Product Name</th><th>Alert Type</th><th>Created At</th><th>Message</th></tr></thead><tbody>");
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT id, product_id, alert_type, created_at, message
                FROM alerts
                WHERE status = 'UNRESOLVED'
                ORDER BY created_at DESC
                """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int aid = rs.getInt("id");
                        int pid = rs.getInt("product_id");
                        String type = rs.getString("alert_type");
                        String createdAt = rs.getTimestamp("created_at").toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        String message = rs.getString("message");
                        String pname = productDao.findById(pid).map(Product::name).orElse("Product " + pid);
                        html.append("<tr>")
                            .append("<td>").append(aid).append("</td>")
                            .append("<td>").append(pid).append("</td>")
                            .append("<td>").append(escapeHtml(pname)).append("</td>")
                            .append("<td>").append(escapeHtml(type)).append("</td>")
                            .append("<td>").append(escapeHtml(createdAt)).append("</td>")
                            .append("<td>").append(escapeHtml(message)).append("</td>")
                            .append("</tr>");
                    }
                }
            }
            html.append("</tbody></table></div>");
        } catch (Exception e) {
            throw new RuntimeException("Failed to build HTML report: " + e.getMessage(), e);
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private int countByActionOnDate(Connection c, String action, LocalDate date) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT COUNT(*) AS cnt
            FROM logs
            WHERE action_type = ?
              AND DATE(timestamp) = ?
            """)) {
            ps.setString(1, action);
            ps.setString(2, date.format(DATE_FORMAT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    private int countUnresolvedAlerts(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT COUNT(*) AS cnt
            FROM alerts
            WHERE status = 'UNRESOLVED'
            """)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    private int countAlertsOnDate(Connection c, LocalDate date) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT COUNT(*) AS cnt
            FROM alerts
            WHERE DATE(created_at) = ?
            """)) {
            ps.setString(1, date.format(DATE_FORMAT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    private Map<Integer, Integer> sumQuantityByProductAndActionOnDate(Connection c, String action, LocalDate date) throws Exception {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("""
            SELECT product_id, SUM(quantity) AS qty
            FROM logs
            WHERE action_type = ?
              AND DATE(timestamp) = ?
            GROUP BY product_id
            """)) {
            ps.setString(1, action);
            ps.setString(2, date.format(DATE_FORMAT));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("product_id"), rs.getInt("qty"));
                }
            }
        }
        return map;
    }

    private Map<Integer, Boolean> unionKeys(Map<Integer, Integer> a, Map<Integer, Integer> b) {
        Map<Integer, Boolean> keys = new HashMap<>();
        keys.putAll(a.keySet().stream().collect(HashMap::new, (m, k) -> m.put(k, true), HashMap::putAll));
        keys.putAll(b.keySet().stream().collect(HashMap::new, (m, k) -> m.put(k, true), HashMap::putAll));
        return keys;
    }

    private String escape(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}



