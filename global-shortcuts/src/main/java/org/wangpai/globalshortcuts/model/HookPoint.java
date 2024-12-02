package org.wangpai.globalshortcuts.model;

/**
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
}

