package com.dayflow.util;

import com.dayflow.model.Task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canvas 作业 → DayFlow Task 的转换。
 *
 * <p>Canvas 字段：
 * <ul>
 *   <li>title / name      → Task.title（优先 title）
 *   <li>due_at            → Task.date + Task.time（ISO 8601）
 *   <li>course_name       → Task.category（API include[]=course_name）
 * </ul>
 * 只有 due_at 存在且日期不早于今天的作业才会被导入。
 */
public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    /**
     * 把 Canvas 返回的作业列表转成 Task 并返回过滤统计。
     */
    public static AssignmentFilterResult filterAndMap(
            List<Map<String, Object>> assignments,
            ZoneId zone) {

        LocalDate today = LocalDate.now();
        List<Task> result = new ArrayList<>();
        int skippedMalformed = 0;
        int skippedPast = 0;

        for (Map<String, Object> a : assignments) {
            String title = stringOr(a, "title", null);
            if (title == null) {
                title = stringOr(a, "name", null);
            }
            String dueAt = stringOr(a, "due_at", null);

            if (title == null || dueAt == null || dueAt.isBlank()) {
                skippedMalformed++;
                continue;
            }

            LocalDateTime when = parse(dueAt, zone);
            if (when == null) {
                skippedMalformed++;
                continue;
            }

            LocalDate date = when.toLocalDate();
            if (date.isBefore(today)) {
                skippedPast++;
                continue;
            }

            String course = stringOr(a, "course_name", null);
            if (course == null || course.isBlank()) {
                course = "Canvas";
            }

            // 动态算 urgency：离截止日期越近越紧急
            double daysLeft = java.time.Duration.between(
                    java.time.LocalDateTime.now(), when).toSeconds() / 86400.0;
            int urgency;
            if (daysLeft <= 0) urgency = 5;
            else if (daysLeft <= 1) urgency = 5;
            else if (daysLeft <= 3) urgency = 4;
            else if (daysLeft <= 7) urgency = 3;
            else if (daysLeft <= 14) urgency = 2;
            else urgency = 1;

            result.add(new Task(
                    title.trim(),
                    when.toLocalTime(),
                    course.trim(),
                    date,
                    false,
                    Task.Source.CANVAS,
                    4,          // importance: Canvas 作业默认比较重要
                    urgency,    // 动态 urgency
                    90,         // estimatedMinutes: 默认 90min
                    3,          // difficulty: 默认中等
                    true,       // timeConstraint: Canvas 作业硬截止
                    0           // progress: 默认 0
            ));
        }

        return new AssignmentFilterResult(
                result, assignments.size(), skippedMalformed, skippedPast);
    }

    /** 默认时区用系统时区，国内用户一般是 Asia/Shanghai。 */
    public static AssignmentFilterResult filterAndMap(
            List<Map<String, Object>> assignments) {
        return filterAndMap(assignments, ZoneId.systemDefault());
    }

    /** 兼容旧代码：只返回 Task 列表。 */
    public static List<Task> toTasks(List<Map<String, Object>> assignments) {
        return filterAndMap(assignments).tasks();
    }

    public static List<Task> toTasks(
            List<Map<String, Object>> assignments, ZoneId zone) {
        return filterAndMap(assignments, zone).tasks();
    }

    private static String stringOr(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static LocalDateTime parse(String iso8601, ZoneId zone) {
        try {
            Instant instant = Instant.parse(iso8601);
            return instant.atZone(zone).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(iso8601);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }
}
