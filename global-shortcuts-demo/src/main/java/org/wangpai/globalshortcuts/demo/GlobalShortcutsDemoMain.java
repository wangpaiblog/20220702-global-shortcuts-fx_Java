package org.wangpai.globalshortcuts.demo;

import com.melloware.jintellitype.JIntellitype;
import org.wangpai.globalshortcuts.GlobalShortcutsFX;
import org.wangpai.globalshortcuts.exception.GlobalShortcutsException;
import org.wangpai.globalshortcuts.model.JIntellitypeShortcut;

import static org.wangpai.globalshortcuts.model.GlobalShortcutsLifecycle.AFTER_ALL_INSTANCES_EXIT;
import static org.wangpai.globalshortcuts.model.GlobalShortcutsLifecycle.AFTER_EXIT;
import static org.wangpai.globalshortcuts.model.GlobalShortcutsLifecycle.AFTER_SUSPEND;
import static org.wangpai.globalshortcuts.model.GlobalShortcutsLifecycle.BEFORE_RESUME;
import static org.wangpai.globalshortcuts.model.GlobalShortcutsLifecycle.BEFORE_START;

public class GlobalShortcutsDemoMain {
    private static void customActivity() {
        try {
            Thread.sleep(1000); // 模拟耗时任务（1 秒）
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws GlobalShortcutsException {
        new GlobalShortcutsFX()
                .setMainActivityAction(GlobalShortcutsDemoMain::customActivity)
                .setRepeatable(true)
                .setShortcut(BEFORE_START,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'J'),
                        () -> System.out.println("######## customActivity 开始执行 ########")
                )
                .setShortcut(AFTER_SUSPEND,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'K'),
                        () -> System.out.println("######## customActivity 暂停执行 ########")
                )
                .setShortcut(BEFORE_RESUME,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'J'),
                        () -> System.out.println("######## customActivity 恢复执行 ########")
                )
                .setShortcut(AFTER_EXIT,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'L'),
                        () -> System.out.println("######## customActivity 中止执行 ########")
                )
                .setShortcut(AFTER_ALL_INSTANCES_EXIT,
                        new JIntellitypeShortcut(JIntellitype.MOD_SHIFT + JIntellitype.MOD_ALT, 'M'),
                        () -> System.out.println("######## 本程序终止执行 ########")
                )
                .execute();
    }
}
