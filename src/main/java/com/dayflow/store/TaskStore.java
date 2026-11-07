package com.dayflow.store;

import com.dayflow.model.Task;
import com.dayflow.model.TaskPriority;
import com.dayflow.model.TaskView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务数据仓库：负责任务的增删和按视图查询，所有变更同步写入 SQLite。
 */
public class TaskStore {

    private final List<Task> tasks = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final TaskDatabase db;

    /** 打开默认数据库并加载全部任务。 */
    public TaskStore() {
        this(TaskDatabase.openDefault());
    }

    /** 使用指定数据库（便于测试）。 */
    public TaskStore(TaskDatabase db) {
        this.db = db;
        tasks.addAll(db.loadAll());
    }

    /** 注册数据变更监听（悬浮窗等外部视图用来即时刷新）。 */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** 任务内容被就地修改（如勾选完成）后，落库并通知监听者。 */
    public void touch() {
        for (Task task : tasks) {
            if (task.getId() > 0) {
                db.update(task);
            }
        }
        fireChange();
    }

    private void fireChange() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public void add(Task task) {
        task.setId(db.insert(task));
        tasks.add(task);
        fireChange();
    }

    public void remove(Task task) {
        if (task.getId() > 0) {
            db.delete(task.getId());
        }
        tasks.remove(task);
        fireChange();
    }

    /** 当前是否一条任务都没有（用于决定是否播种演示数据）。 */
    public boolean isEmpty() {
        return tasks.isEmpty();
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
