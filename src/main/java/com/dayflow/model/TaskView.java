package com.dayflow.model;

/**
 * 任务列表的筛选视图，对应左侧导航项。
 */
public enum TaskView {

    TODAY("今天"),
    ALL("所有任务"),
    COMPLETED("已完成");

    private final String label;

    TaskView(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
