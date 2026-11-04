package com.dayflow.store;

import com.dayflow.model.Task;
import com.dayflow.model.TaskPriority;
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
                    case HOMEWORK -> task.isCanvasHomework()
                            && !task.isCompleted();
                    case COMPLETED -> task.isCompleted();
                    case ALL -> true;
                    case PRIORITY -> !task.isCompleted();
                })
                .sorted(sortBy(view))
                .toList();
    }

    /**
     * 不同视图的默认排序策略。
     */
    private Comparator<Task> sortBy(TaskView view) {
        if (view == TaskView.PRIORITY) {
            // 按优先级分数从高到低
            return Comparator.comparingDouble(TaskPriority::score).reversed();
        }

        // 其他视图默认按日期 → 时间排序
        return Comparator
                .comparing(Task::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::priority, Comparator.reverseOrder()); // 同时间则优先级高在前
    }

    /**
     * 尚未完成的任务总数。
     */
    public long unfinishedCount() {
        return tasks.stream()
                .filter(task -> !task.isCompleted())
                .count();
    }

    /**
     * 未完成的 Canvas 作业总数。
     */
    public long homeworkCount() {
        return tasks.stream()
                .filter(t -> t.isCanvasHomework() && !t.isCompleted())
                .count();
    }
}
