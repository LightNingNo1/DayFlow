package com.dayflow.model;

import java.time.LocalDate;

/**
 * 一条任务的数据结构。
 */
public class Task {

    private final String title;
    private final String time;
    private final String category;
    private final LocalDate date;
    private boolean completed;

    public Task(String title, String time, String category,
                LocalDate date, boolean completed) {
        this.title = title;
        this.time = time;
        this.category = category;
        this.date = date;
        this.completed = completed;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
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
}
