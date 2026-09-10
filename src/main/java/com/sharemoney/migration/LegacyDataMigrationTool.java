package com.sharemoney.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class LegacyDataMigrationTool {

    private static final String TEMP_PASSWORD = "123456";
    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);
    private static final DateTimeFormatter JSON_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter JSON_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Connection connection;
    private final Workbook workbook;
    private final Map<String, Long> userIdByUsername = new HashMap<>();

    private LegacyDataMigrationTool(Connection connection, Workbook workbook) {
        this.connection = connection;
        this.workbook = workbook;
    }

    public static LegacyDataMigrationTool of(Connection connection, Workbook workbook) {
        return new LegacyDataMigrationTool(connection, workbook);
    }

    public void run() throws Exception {
        try {
            migrateUsers();
            migrateInstallmentChoices();
            migrateDebts();
            migrateLoginLogs();
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: LegacyDataMigrationTool <xlsx-path> [dbUrl] [dbUser] [dbPassword]");
            System.exit(1);
        }

        String xlsxPath = args[0];
        String dbUrl = args.length > 1 ? args[1] : envOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/share_money?currentSchema=shmy");
        String dbUser = args.length > 2 ? args[2] : envOrDefault("DB_USERNAME", "postgres");
        String dbPassword = args.length > 3 ? args[3] : envOrDefault("DB_PASSWORD", "");

        try (FileInputStream fis = new FileInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(fis);
             Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            connection.setAutoCommit(false);
            try {
                of(connection, workbook).run();
                System.out.println("Migration committed successfully.");
                System.out.println("All migrated users share temporary password: " + TEMP_PASSWORD);
                System.out.println("Ask each user to change it via POST /api/auth/change-password.");
            } catch (Exception e) {
                System.err.println("Migration failed, rolled back. Cause: " + e.getMessage());
                throw e;
            }
        }
    }

    private void migrateUsers() throws Exception {
        Sheet sheet = workbook.getSheet("Users");
        migrateUsersPass(sheet, false);
        migrateUsersPass(sheet, true);
    }

    private void migrateUsersPass(Sheet sheet, boolean debtorsOnly) throws Exception {
        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }
            String username = str(row, 0);
            if (username == null || username.isBlank()) {
                continue;
            }
            String role = str(row, 3).trim().toUpperCase();
            boolean isDebtor = "DEBTOR".equals(role);
            if (isDebtor != debtorsOnly) {
                continue;
            }

            Long existingId = findUserId(username);
            if (existingId != null) {
                userIdByUsername.put(username, existingId);
                System.out.println("Skip user (already exists): " + username);
                continue;
            }

            String fullName = str(row, 4);
            String creditorUsername = str(row, 5);
            boolean active = "TRUE".equalsIgnoreCase(str(row, 6));
            LocalDateTime createdAt = dateTime(row, 7);

            Long creditorId = isDebtor ? userIdByUsername.get(creditorUsername) : null;

            String sql = """
                    INSERT INTO users (username, password_hash, full_name, role, creditor_id, active, preferred_language, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'th', ?, ?)
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, PASSWORD_ENCODER.encode(TEMP_PASSWORD));
                ps.setString(3, fullName);
                ps.setString(4, role);
                if (creditorId != null) {
                    ps.setLong(5, creditorId);
                } else {
                    ps.setNull(5, java.sql.Types.BIGINT);
                }
                ps.setBoolean(6, active);
                ps.setTimestamp(7, timestampOrNow(createdAt));
                ps.setTimestamp(8, timestampOrNow(createdAt));
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    userIdByUsername.put(username, keys.getLong(1));
                }
            }
            System.out.println("Inserted user: " + username + " (" + role + ")");
        }
    }

    private void migrateInstallmentChoices() throws Exception {
        Sheet sheet = workbook.getSheet("Settings");
        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }
            String key = str(row, 0);
            if (!"installmentChoices".equals(key)) {
                continue;
            }
            String value = str(row, 1);
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE settings SET value = ? WHERE key = 'installment_choices'")) {
                ps.setString(1, value);
                ps.executeUpdate();
            }
            System.out.println("Updated installment_choices = " + value);
        }
    }

    private void migrateDebts() throws Exception {
        Sheet sheet = workbook.getSheet("Debts");
        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }
            String legacyId = str(row, 0);
            if (legacyId == null || legacyId.isBlank()) {
                continue;
            }

            String creditorUsername = str(row, 1);
            String debtorUsername = str(row, 2);
            Long creditorId = userIdByUsername.get(creditorUsername);
            Long debtorId = userIdByUsername.get(debtorUsername);
            if (creditorId == null || debtorId == null) {
                System.out.println("Skip debt " + legacyId + ": creditor/debtor not migrated (" + creditorUsername + "/" + debtorUsername + ")");
                continue;
            }

            String title = str(row, 3);
            String description = str(row, 4);
            java.math.BigDecimal amount = decimal(row, 5);
            String method = str(row, 6).trim().toUpperCase();
            Integer installmentCount = intOrNull(row, 7);
            java.math.BigDecimal installmentAmount = decimal(row, 8);
            String installmentsJson = str(row, 9);
            java.math.BigDecimal paid = decimal(row, 10);
            String status = str(row, 11).trim().toUpperCase();
            LocalDateTime createdAt = dateTime(row, 12);
            LocalDateTime updatedAt = dateTime(row, 13);
            Integer sortOrder = intOrNull(row, 14);

            JsonNode installmentsArray = parseJsonArray(installmentsJson);
            LocalDate startDate = deriveStartDate(method, installmentsArray, createdAt);

            long debtId = insertDebt(creditorId, debtorId, title, description, method, amount,
                    "OPEN".equals(method) ? null : installmentCount,
                    installmentAmount, startDate, status, paid, sortOrder, createdAt, updatedAt);

            if ("INSTALLMENT".equals(method)) {
                insertInstallments(debtId, installmentsArray, startDate);
            } else if ("OPEN".equals(method)) {
                insertOpenLoanRecords(debtId, installmentsArray);
            }

            System.out.println("Inserted debt " + legacyId + " -> id=" + debtId);
        }
    }

    private long insertDebt(long creditorId, long debtorId, String title, String description, String method,
                             java.math.BigDecimal amount, Integer installmentCount, java.math.BigDecimal installmentAmount,
                             LocalDate startDate, String status, java.math.BigDecimal paid, Integer sortOrder,
                             LocalDateTime createdAt, LocalDateTime updatedAt) throws Exception {
        String sql = """
                INSERT INTO debts (creditor_id, debtor_id, title, description, method, principal_amount,
                                    installment_count, installment_amount, start_date, status, paid_amount,
                                    sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, creditorId);
            ps.setLong(2, debtorId);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setString(5, method);
            ps.setBigDecimal(6, amount);
            if (installmentCount != null) {
                ps.setInt(7, installmentCount);
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setBigDecimal(8, installmentAmount);
            ps.setDate(9, java.sql.Date.valueOf(startDate));
            ps.setString(10, status);
            ps.setBigDecimal(11, paid);
            if (sortOrder != null) {
                ps.setInt(12, sortOrder);
            } else {
                ps.setNull(12, java.sql.Types.INTEGER);
            }
            ps.setTimestamp(13, timestampOrNow(createdAt));
            ps.setTimestamp(14, timestampOrNow(updatedAt != null ? updatedAt : createdAt));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertInstallments(long debtId, JsonNode installments, LocalDate debtStartDate) throws Exception {
        String sql = """
                INSERT INTO installments (debt_id, no, amount, kind, status, due_date, pay_date, paid_at)
                VALUES (?, ?, ?, 'PRINCIPAL', ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (JsonNode item : installments) {
                int no = item.get("no").asInt();
                String payDateText = textOrEmpty(item, "payDate");
                String dueDateText = textOrEmpty(item, "dueDate");

                LocalDate dueDate;
                if (!dueDateText.isBlank()) {
                    dueDate = LocalDate.parse(dueDateText, JSON_DATE);
                } else if (!payDateText.isBlank()) {
                    dueDate = LocalDate.parse(payDateText, JSON_DATE);
                } else {
                    dueDate = debtStartDate.plusMonths(no - 1L);
                }

                ps.setLong(1, debtId);
                ps.setInt(2, no);
                ps.setBigDecimal(3, new java.math.BigDecimal(item.get("amount").asText()));
                ps.setString(4, textOrEmpty(item, "status").toUpperCase());
                ps.setDate(5, java.sql.Date.valueOf(dueDate));
                ps.setDate(6, sqlDate(payDateText));
                ps.setTimestamp(7, sqlTimestamp(textOrEmpty(item, "paidAt")));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertOpenLoanRecords(long debtId, JsonNode records) throws Exception {
        String sql = """
                INSERT INTO open_loan_records (debt_id, no, pay_date, total_paid, interest, remaining_principal, status, paid_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (JsonNode item : records) {
                ps.setLong(1, debtId);
                ps.setInt(2, item.get("no").asInt());
                ps.setDate(3, sqlDate(textOrEmpty(item, "payDate")));
                ps.setBigDecimal(4, new java.math.BigDecimal(item.get("total").asText()));
                ps.setBigDecimal(5, new java.math.BigDecimal(item.get("interest").asText()));
                ps.setBigDecimal(6, new java.math.BigDecimal(item.get("remain").asText()));
                ps.setString(7, textOrEmpty(item, "status").toUpperCase());
                ps.setTimestamp(8, sqlTimestamp(textOrEmpty(item, "paidAt")));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void migrateLoginLogs() throws Exception {
        Sheet sheet = workbook.getSheet("Logins");
        String sql = "INSERT INTO login_logs (username, role, action, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int count = 0;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                LocalDateTime timestamp = dateTime(row, 0);
                String username = str(row, 1);
                String role = str(row, 2);
                String action = str(row, 3);
                if (username == null || username.isBlank()) {
                    continue;
                }
                ps.setString(1, username);
                ps.setString(2, role == null ? "" : role.toUpperCase());
                ps.setString(3, action == null ? "" : action.toUpperCase());
                ps.setTimestamp(4, timestampOrNow(timestamp));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            System.out.println("Inserted " + count + " login log rows.");
        }
    }

    private LocalDate deriveStartDate(String method, JsonNode installments, LocalDateTime createdAt) {
        if (installments != null && installments.size() > 0) {
            JsonNode first = installments.get(0);
            String primaryField = "OPEN".equals(method) ? "payDate" : "dueDate";
            String primaryValue = textOrEmpty(first, primaryField);
            if (!primaryValue.isBlank()) {
                return LocalDate.parse(primaryValue, JSON_DATE);
            }
            String payDate = textOrEmpty(first, "payDate");
            if (!payDate.isBlank()) {
                return LocalDate.parse(payDate, JSON_DATE);
            }
        }
        return createdAt != null ? createdAt.toLocalDate() : LocalDate.now();
    }

    private Long findUserId(String username) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM users WHERE lower(username) = lower(?)")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private JsonNode parseJsonArray(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return JSON.createArrayNode();
        }
        return JSON.readTree(json);
    }

    private String str(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private java.math.BigDecimal decimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return java.math.BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String text = FORMATTER.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private Integer intOrNull(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String text = FORMATTER.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime dateTime(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }
        return cell.getLocalDateTimeCellValue();
    }

    private String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private java.sql.Date sqlDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.sql.Date.valueOf(LocalDate.parse(value, JSON_DATE));
    }

    private Timestamp sqlTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.parse(value, JSON_DATE_TIME));
    }

    private Timestamp timestampOrNow(LocalDateTime value) {
        return Timestamp.valueOf(value != null ? value : LocalDateTime.now());
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value != null && !value.isBlank() ? value : fallback;
    }
}
