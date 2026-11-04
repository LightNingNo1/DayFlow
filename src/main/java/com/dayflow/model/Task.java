package com.dayflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 一条任务的数据结构。
 *
 * <p>六个优先级维度：重要程度 / 紧急程度 / 预计耗时 / 任务难度 / 时间约束 / 任务进度。
 * 最终优先级分数由 {@link TaskPriority#score(Task)} 统一计算。
 */
public class Task {

    public enum Source {
        MANUAL, CANVAS
    }

    private final String title;
    private final LocalTime time;
    private final String category;
    private final LocalDate date;
    private boolean completed;
    private final Source source;

    // —— 六个优先级维度 ——

    /** 重要程度，范围 1-5，5 最重要。 */
    private int importance;

    /** 紧急程度，范围 1-5，5 最紧急。 */
    private int urgency;

    /** 预计耗时，分钟数，建议 15-480。 */
    private int estimatedMinutes;

    /** 任务难度，范围 1-5，5 最难。 */
    private int difficulty;

    /** 是否硬截止时间约束（Canvas 作业默认 true）。 */
    private boolean timeConstraint;

    /** 完成进度，范围 0-100。 */
    private int progress;

    public Task(String title, LocalTime time, String category,
                LocalDate date, boolean completed, Source source) {
        this(title, time, category, date, completed, source,
                3, 3, 60, 3, source == Source.CANVAS, 0);
    }

    public Task(String title, LocalTime time, String category,
                LocalDate date, boolean completed, Source source,
                int importance, int urgency, int estimatedMinutes,
                int difficulty, boolean timeConstraint, int progress) {
        this.title = title;
        this.time = time;
        this.category = category;
        this.date = date;
        this.completed = completed;
        this.source = source;

        this.importance = clamp5(importance);
        this.urgency = clamp5(urgency);
        this.estimatedMinutes = Math.max(5, estimatedMinutes);
        this.difficulty = clamp5(difficulty);
        this.timeConstraint = timeConstraint;
        this.progress = Math.max(0, Math.min(100, progress));
    }

    private static int clamp5(int v) {
        return Math.max(1, Math.min(5, v));
    }

    // —— 基础字段 ——

    public String getTitle() {
        return title;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Source getSource() {
        return source;
    }

    public boolean isCanvasHomework() {
        return source == Source.CANVAS;
    }

    // —— 六维度 getter/setter ——

    public int getImportance() {
        return importance;
    }

    public void setImportance(int v) {
        this.importance = clamp5(v);
    }

    public int getUrgency() {
        return urgency;
    }

    public void setUrgency(int v) {
        this.urgency = clamp5(v);
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int v) {
        this.estimatedMinutes = Math.max(5, v);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int v) {
        this.difficulty = clamp5(v);
    }

    public boolean hasTimeConstraint() {
        return timeConstraint;
    }

    public void setTimeConstraint(boolean v) {
        this.timeConstraint = v;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int v) {
        this.progress = Math.max(0, Math.min(100, v));
    }

    // —— 计算 ——

    /** 动态优先级分数，越大越优先。每次调用重新计算（时间衰减随现在变化）。 */
    public double priority() {
        return TaskPriority.score(this);
    }

    public boolean isOverDue() {
        if (date == null) {
            return false;
        }
        return date.atTime(time == null ? LocalTime.MAX : time)
                .isBefore(java.time.LocalDateTime.now());
    }
}
