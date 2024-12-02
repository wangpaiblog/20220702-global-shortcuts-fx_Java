package org.wangpai.globalshortcuts.model;

/**
 * @since 2024-12-2
 */
public enum ShortcutPoint {
    START, // 开始任务
    SUSPEND, // 暂停任务
    RESUME, // 恢复任务
    EXIT, // 本 GlobalShortcutsFx 实例退出
    ALL_INSTANCES_EXIT, // 所有 GlobalShortcutsFx 实例全部销毁不再使用
}

