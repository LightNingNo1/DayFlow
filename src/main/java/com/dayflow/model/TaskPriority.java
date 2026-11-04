package com.dayflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 任务优先级计算。
 *
 * <p>基础分 = 重要程度×3 + 紧急程度×4 - 难度×1.5
 *           - log2(预计耗时/15)×2 + 时间约束奖励5 - 进度×0.15
 *
 * <p>时间衰减：距离截止日期越近，额外加权越大
 * <ul>
 *   <li>已过期：+8</li>
 *   <li>≤12 小时：+6</li>
 *   <li>≤24 小时：+4</li>
 *   <li>≤3 天：+2</li>
 *   <li>≤7 天：+0.5</li>
 * </ul>
 */
public final class TaskPriority {

    private TaskPriority() {
    }

    public static double score(Task t) {
        // 已完成的任务优先级为 0（不再竞争）
        if (t.isCompleted()) {
            return 0;
        }

        double base = t.getImportance() * 3.0
                + t.getUrgency() * 4.0
                - t.getDifficulty() * 1.5
                - Math.log(Math.max(1.0, t.getEstimatedMinutes() / 15.0)) * 2.0
                + (t.hasTimeConstraint() ? 5.0 : 0.0)
                - t.getProgress() * 0.15;

        base += timeDecay(t);

        return base;
    }

    /** 时间衰减：截止日期越近，奖励越大。 */
    private static double timeDecay(Task t) {
        LocalDate d = t.getDate();
        if (d == null) {
            return 0;
        }

        LocalTime lt = t.getTime() == null ? LocalTime.MAX : t.getTime();
        LocalDateTime deadline = LocalDateTime.of(d, lt);
        double hours = java.time.Duration.between(LocalDateTime.now(), deadline)
                .toSeconds() / 3600.0;

        if (hours < 0) {
            return 8.0;          // 已过期
        } else if (hours <= 12) {
            return 6.0;
        } else if (hours <= 24) {
            return 4.0;
        } else if (hours <= 72) {
            return 2.0;
        } else if (hours <= 168) {
            return 0.5;
        } else {
            return 0;
        }
    }

    /** 把优先级分数映射成 5 级标签，用于 UI 展示。 */
    public static String tier(double score) {
        if (score >= 12) return "🔥 P0";
        if (score >= 8) return "🔴 P1";
        if (score >= 4) return "🟠 P2";
        if (score >= 0) return "🟡 P3";
        return "⚪ P4";
    }
}
