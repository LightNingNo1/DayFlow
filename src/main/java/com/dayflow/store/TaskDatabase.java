package com.dayflow.store;

import com.dayflow.model.Task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 任务持久化：单表 tasks，应用启动时建库建表。
 *
 * <p>数据库文件默认在 {@code 用户目录/.dayflow/dayflow.db}。
 */
public class TaskDatabase implements AutoCloseable {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS tasks (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                title           TEXT    NOT NULL,
                time            TEXT,
                category        TEXT,
                date            TEXT,
                completed       INTEGER NOT NULL DEFAULT 0,
                source          TEXT    NOT NULL DEFAULT 'MANUAL',
                importance      INTEGER NOT NULL DEFAULT 3,
                urgency         INTEGER NOT NULL DEFAULT 3,
                estimated_min   INTEGER NOT NULL DEFAULT 60,
                difficulty      INTEGER NOT NULL DEFAULT 3,
                time_constraint INTEGER NOT NULL DEFAULT 0,
                progress        INTEGER NOT NULL DEFAULT 0
            )
            """;

    private final Connection connection;

    private TaskDatabase(Connection connection) {
        this.connection = connection;
    }

    /** 打开默认位置的数据库（不存在则创建）。 */
    public static TaskDatabase openDefault() {
        Path dir = Path.of(System.getProperty("user.home"), ".dayflow");
        return open(dir.resolve("dayflow.db"));
    }

    /** 打开指定文件的数据库。 */
    public static TaskDatabase open(Path file) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + file);
            try (Statement st = conn.createStatement()) {
                st.execute(SCHEMA);
            }
            return new TaskDatabase(conn);
        } catch (Exception e) {
            throw new IllegalStateException("无法打开任务数据库: " + file, e);
        }
    }

    /** 读取全部任务（按 id 顺序，即创建顺序）。 */
    public List<Task> loadAll() {
        String sql = "SELECT id, title, time, category, date, completed, source,"
                + " importance, urgency, estimated_min, difficulty, time_constraint, progress"
                + " FROM tasks ORDER BY id";
        List<Task> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Task task = new Task(
                        rs.getString("title"),
                        rs.getString("time") == null ? null : LocalTime.parse(rs.getString("time")),
                        rs.getString("category"),
                        rs.getString("date") == null ? null : LocalDate.parse(rs.getString("date")),
                        rs.getInt("completed") != 0,
                        Task.Source.valueOf(rs.getString("source")),
                        rs.getInt("importance"),
                        rs.getInt("urgency"),
                        rs.getInt("estimated_min"),
                        rs.getInt("difficulty"),
                        rs.getInt("time_constraint") != 0,
                        rs.getInt("progress")
                );
                task.setId(rs.getInt("id"));
                result.add(task);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("读取任务失败", e);
        }
        return result;
    }

    /** 插入一条任务，返回生成的主键。 */
    public int insert(Task task) {
        String sql = "INSERT INTO tasks (title, time, category, date, completed, source,"
                + " importance, urgency, estimated_min, difficulty, time_constraint, progress)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, task);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("保存任务失败: " + task.getTitle(), e);
        }
    }

    /** 按主键更新一条任务。 */
    public void update(Task task) {
        String sql = "UPDATE tasks SET title=?, time=?, category=?, date=?, completed=?, source=?,"
                + " importance=?, urgency=?, estimated_min=?, difficulty=?, time_constraint=?, progress=?"
                + " WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, task);
            ps.setInt(13, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("更新任务失败: " + task.getTitle(), e);
        }
    }

    /** 按主键删除。 */
    public void delete(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("删除任务失败: " + id, e);
        }
    }

    private static void bind(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getTime() == null ? null : task.getTime().toString());
        ps.setString(3, task.getCategory());
        ps.setString(4, task.getDate() == null ? null : task.getDate().toString());
        ps.setInt(5, task.isCompleted() ? 1 : 0);
        ps.setString(6, task.getSource().name());
        ps.setInt(7, task.getImportance());
        ps.setInt(8, task.getUrgency());
        ps.setInt(9, task.getEstimatedMinutes());
        ps.setInt(10, task.getDifficulty());
        ps.setInt(11, task.hasTimeConstraint() ? 1 : 0);
        ps.setInt(12, task.getProgress());
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
