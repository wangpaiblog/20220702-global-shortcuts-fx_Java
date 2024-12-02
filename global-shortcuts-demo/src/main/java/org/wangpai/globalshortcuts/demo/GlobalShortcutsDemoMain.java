package org.wangpai.globalshortcuts.demo;

import com.melloware.jintellitype.JIntellitype;
import org.wangpai.globalshortcuts.GlobalShortcutsFX;
import org.wangpai.globalshortcuts.exception.GlobalShortcutsException;
import org.wangpai.globalshortcuts.model.HookPoint;
import org.wangpai.globalshortcuts.model.JIntellitypeShortcut;
import org.wangpai.globalshortcuts.model.ShortcutPoint;

/**
 * GlobalShortcutsFX 用法示例
 *
 * @since 2022-6-30
 */
public class GlobalShortcutsDemoMain {
    private static void customActivity() {
        try {
            Thread.sleep(1000); // 模拟耗时任务（1 秒）
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * @since 2024-12-2
     */
    public static void main(String[] args) throws GlobalShortcutsException {
        new GlobalShortcutsFX()
                .setMainActivityAction(GlobalShortcutsDemoMain::customActivity)
                .setRepeatable(true)
                .setShortcut(ShortcutPoint.START,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'J'))
                .setShortcut(ShortcutPoint.SUSPEND,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'K'))
                .setShortcut(ShortcutPoint.RESUME,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'J'))
                .setShortcut(ShortcutPoint.EXIT,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'L'))
                .setShortcut(ShortcutPoint.ALL_INSTANCES_EXIT,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'M'))
                .setLifecycleHook(HookPoint.BEFORE_START,
                        () -> System.out.println("######## customActivity 即将开始执行 ########"))
                .setLifecycleHook(HookPoint.BEFORE_SUSPEND,
                        () -> System.out.println("######## customActivity 即将暂停执行 ########"))
                .setLifecycleHook(HookPoint.AFTER_SUSPEND,
                        () -> System.out.println("######## customActivity 已经暂停执行 ########"))
                .setLifecycleHook(HookPoint.BEFORE_RESUME,
                        () -> System.out.println("######## customActivity 即将恢复执行 ########"))
                .setLifecycleHook(HookPoint.BEFORE_EXIT,
                        () -> System.out.println("######## customActivity 即将中止执行 ########"))
                .setLifecycleHook(HookPoint.AFTER_EXIT,
                        () -> System.out.println("######## customActivity 已经中止执行 ########"))
                .setLifecycleHook(HookPoint.BEFORE_ALL_INSTANCES_EXIT,
                        () -> System.out.println("######## 本程序即将终止执行 ########"))
                .setLifecycleHook(HookPoint.AFTER_ALL_INSTANCES_EXIT,
                        () -> System.out.println("######## 本程序已经终止执行 ########"))
                .execute();
    }
}
