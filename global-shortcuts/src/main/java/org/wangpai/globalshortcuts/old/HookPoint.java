package org.wangpai.globalshortcuts.old;

/**
 * 2024年12月4日：此类已失去其作用，只是作为一个列出所有 Hook 回调的大纲存在
 *
 * @since 2022-9-30
 */
public enum HookPoint {
    BEFORE_START, // 在即将开始前的那个时间点

    BEFORE_SUSPEND, // 在即将暂停前的那个时间点
    AFTER_SUSPEND, // 在刚刚暂停后的那个时间点

    BEFORE_RESUME, // 在即将恢复运行前的那个时间点

    BEFORE_EXIT, // 本 GlobalShortcutsFx 实例退出前的那个时间点
    AFTER_EXIT, // 本 GlobalShortcutsFx 实例退出后的那个时间点

    BEFORE_ALL_INSTANCES_EXIT, // 实例全部销毁前的那个时间点。当需要所有 GlobalShortcutsFx 实例全部销毁不再使用时，使用此状态
    AFTER_ALL_INSTANCES_EXIT, // 实例全部销毁后的那个时间点。当需要所有 GlobalShortcutsFx 实例全部销毁不再使用时，使用此状态

    WHEN_EXCEPTION_HAPPEN, // 当异常发生之后

    BEFORE_EVERY_LOOP_START, // 在每一圈任务执行前调用

    AFTER_EVERY_LOOP_FINISH, // 在每一圈任务执行后调用
}

