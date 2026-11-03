package com.dayflow.util;

import com.dayflow.model.Task;

import java.util.List;

/**
 * AssignmentMapper 的转换结果，除了有效 Task 列表还带过滤统计，
 * 方便 UI 层向用户解释"为什么导入 0 个"。
 */
public record AssignmentFilterResult(
        /** 通过过滤的有效任务。 */
        List<Task> tasks,
        /** Canvas 原始作业总数。 */
        int totalRaw,
        /** 因无 due_at / 无 title / 解析失败被跳过的数量。 */
        int skippedMalformed,
        /** 因 due_at 早于今天被过滤的数量。 */
        int skippedPast) {
}
