package org.wangpai.globalshortcuts.demo;

import com.melloware.jintellitype.JIntellitype;
import org.wangpai.globalshortcuts.GlobalShortcutsFX;
import org.wangpai.globalshortcuts.exception.GlobalShortcutsException;
import org.wangpai.globalshortcuts.raw.JIntellitypeShortcut;
import org.wangpai.globalshortcuts.raw.ShortcutPoint;

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
        GlobalShortcutsFX.builder()
                .withRepeatable(true)
                .nextBuilder()
                .setMainActivityAction(GlobalShortcutsDemoMain::customActivity)
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
                .beforeTaskStart(() -> System.out.println("######## customActivity 即将开始执行 ########"))
                .beforeTaskSuspend(() -> System.out.println("######## customActivity 即将暂停执行 ########"))
                .afterTaskSuspend(() -> System.out.println("######## customActivity 已经暂停执行 ########"))
                .beforeTaskResume(() -> System.out.println("######## customActivity 即将恢复执行 ########"))
                .beforeInstanceExit(() -> System.out.println("######## customActivity 即将中止执行 ########"))
                .afterInstanceExit(() -> System.out.println("######## customActivity 已经中止执行 ########"))
                .beforeAllInstancesExit(() -> System.out.println("######## 本程序即将终止执行 ########"))
                .afterAllInstancesExit(() -> System.out.println("######## 本程序已经终止执行 ########"))
                .beforeEveryLoopStart(runningLoop ->
                        System.out.printf("----- customActivity 第 %d 圈任务准备执行 ------%n", runningLoop))
                .afterEveryLoopFinish(runningLoop ->
                        System.out.printf("***** customActivity 第 %d 圈任务执行完毕 ******%n", runningLoop))
                .beforeEverySleepLoopStart(sleepingLoop ->
                        System.out.printf("----- customActivity 即将进入第 %d 圈睡眠 ------%n", sleepingLoop))
                .afterEverySleepLoopFinish(sleepingLoop ->
                        System.out.printf("***** customActivity 已睡眠了 %d 圈 ******%n", sleepingLoop))
                .setLogLevelInfo(logMessage -> System.out.printf("【INFO 级别的日志信息】：%s%n", logMessage))
                .setLogLevelWarn(logMessage -> System.out.printf("【WARN 级别的日志信息】：%s%n", logMessage))
                .setLogLevelError(logMessage -> System.out.printf("【ERROR 级别的日志信息】：%s%n", logMessage))
                .setOnTaskException(throwable -> {
                    System.err.println("onTaskException 被触发。");
                    throwable.printStackTrace();
                    return true; // 默认单个任务的执行异常不会影响实例的运行
                })
                .setOnInstanceException(throwable -> {
                    System.err.println("onExecutorException 被触发。");
                    throwable.printStackTrace();
                    return false;
                })
                .setOnIgnoreException(throwable -> {
                    System.out.println("onIgnoreException 被触发。");
                    throwable.printStackTrace();
                })
                .setOnCallbackException(throwable -> {
                    System.err.println("错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
                    throwable.printStackTrace();
                })
                .build()
                .execute();
    }
}
