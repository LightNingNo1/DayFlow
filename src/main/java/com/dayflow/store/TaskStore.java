package com.dayflow.store;

import com.dayflow.model.Task;
import com.dayflow.model.TaskView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 任务数据仓库：负责任务的增删和按视图查询。
 */
public class TaskStore {

    private final List<Task> tasks = new ArrayList<>();

    public void add(Task task) {
        tasks.add(task);

        tasks.sort(
                Comparator.comparing(
                        Task::getDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparing(
                        Task::getTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );
    }

    public void remove(Task task) {
        tasks.remove(task);
    }

    /**
     * 按视图筛选需要展示的任务。
     */
    public List<Task> visibleTasks(TaskView view) {
        LocalDate today = LocalDate.now();

        return tasks.stream()
                .filter(task -> switch (view) {
                    case TODAY -> task.getDate().equals(today)
                            && !task.isCompleted();
                    case COMPLETED -> task.isCompleted();
                    case ALL -> true;
                })
                .toList();
    }

    /**
     * 尚未完成的任务总数（不区分视图）。
     */
    public long unfinishedCount() {
        return tasks.stream()
                .filter(task -> !task.isCompleted())
                .count();
    }
}
