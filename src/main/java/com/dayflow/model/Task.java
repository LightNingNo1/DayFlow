package com.dayflow.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 一条任务的数据结构。
 */
public class Task {

    private final String title;
    private final LocalTime time;
    private final String category;
    private final LocalDate date;
    private boolean completed;

    public Task(String title, LocalTime time, String category,
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

    public boolean isOverDue(){
        if(this.date == null || this.time == null){
            return false;
        }

        LocalDateTime deadline = LocalDateTime.of(this.date, this.time);

        return deadline.isBefore(LocalDateTime.now());
    }
}
