package org.wangpai.globalshortcuts;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.wangpai.globalshortcuts.exception.FatalUsageException;
import org.wangpai.globalshortcuts.exception.IncompatibleShortcutException;
import org.wangpai.globalshortcuts.exception.RepeatedShortcutException;
import org.wangpai.globalshortcuts.hook.BasicHook;
import org.wangpai.globalshortcuts.hook.CallbackExceptionHandler;
import org.wangpai.globalshortcuts.hook.EveryLoopHook;
import org.wangpai.globalshortcuts.hook.ExceptionHandler;
import org.wangpai.globalshortcuts.hook.IgnoreExceptionHandler;
import org.wangpai.globalshortcuts.hook.LogCollector;
import org.wangpai.globalshortcuts.old.HookPoint;
import org.wangpai.globalshortcuts.raw.JIntellitypeShortcut;
import org.wangpai.globalshortcuts.raw.ShortcutPoint;

/**
 * @since 2022-6-25
 */
@Accessors(chain = true)
public class GlobalShortcutsFX {
    private final GlobalShortcutsRegister register = new GlobalShortcutsRegister();

    private volatile boolean isRunning = false; // true 代表正在运行，false 代表暂停运行

    private volatile boolean isStarted = false;

    private volatile boolean needTerminate = false;

    private int sleepingLoop = -999;

    private int runningLoop = -999;

    private Thread globalShortcutsThread = null; // 此线程很快就会自动结束

    private Thread customActivityThread = null;

    private Runnable mainActivityAction = null; // 用户单次任务

    /**
     * 允许快捷键重复
     *
     * @since 2022-9-30
     */
    private boolean repeatable = false;

    private BasicHook beforeTaskStart =
            () -> System.out.println("######## customActivity 即将开始执行 ########");

    private BasicHook beforeTaskSuspend =
            () -> System.out.println("######## customActivity 即将暂停执行 ########");

    private BasicHook afterTaskSuspend =
            () -> System.out.println("######## customActivity 已经暂停执行 ########");

    private BasicHook beforeTaskResume =
            () -> System.out.println("######## customActivity 即将恢复执行 ########");

    private BasicHook beforeInstanceExit =
            () -> System.out.println("######## customActivity 即将中止执行 ########");

    private BasicHook afterInstanceExit =
            () -> System.out.println("######## customActivity 已经中止执行 ########");

    private BasicHook beforeAllInstancesExit =
            () -> System.out.println("######## 本程序即将终止执行 ########");

    private BasicHook afterAllInstancesExit =
            () -> System.out.println("######## 本程序已经终止执行 ########");

    private EveryLoopHook beforeEveryLoopStart =
            runningLoop -> System.out.printf("----- customActivity 第 %d 圈任务准备执行 ------%n", runningLoop);

    private EveryLoopHook afterEveryLoopFinish =
            runningLoop -> System.out.printf("***** customActivity 第 %d 圈任务执行完毕 ******%n", runningLoop);

    private EveryLoopHook beforeEverySleepLoopStart =
            sleepingLoop -> System.out.printf("----- customActivity 即将进入第 %d 圈睡眠 ------%n", sleepingLoop);

    private EveryLoopHook afterEverySleepLoopFinish =
            sleepingLoop -> System.out.printf("***** customActivity 已睡眠了 %d 圈 ******%n", sleepingLoop);

    private LogCollector logLevelInfo =
            logMessage -> System.out.printf("【INFO 级别的日志信息】：%s%n", logMessage);

    private LogCollector logLevelWarn =
            logMessage -> System.out.printf("【WARN 级别的日志信息】：%s%n", logMessage);

    private LogCollector logLevelError =
            logMessage -> System.out.printf("【ERROR 级别的日志信息】：%s%n", logMessage);

    /**
     * @since 2024-12-4
     */
    private ExceptionHandler onTaskException = throwable -> {
        System.err.println("onTaskException 被触发。");
        throwable.printStackTrace();
        return true; // 默认单个任务的执行异常不会影响实例的运行
    };

    /**
     * @since 2024-12-4
     */
    private ExceptionHandler onInstanceException = throwable -> {
        System.err.println("onExecutorException 被触发。");
        throwable.printStackTrace();
        return false;
    };

    /**
     * @since 2024-12-4
     */
    private IgnoreExceptionHandler onIgnoreException = throwable -> {
        System.out.println("onIgnoreException 被触发。");
        throwable.printStackTrace();
    };

    /**
     * 当此回调被触发时，
     *
     * @since 2024-12-5
     */
    private CallbackExceptionHandler onCallbackException = throwable -> {
        System.err.println("错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
        throwable.printStackTrace();
    };

    /**
     * @since 2024-12-5
     */
    public static FirstBuilder builder() {
        return new FirstBuilder();
    }

    /**
     * 如果对不同生命周期设置了同一个快捷键，则当此快捷键按下时，视为同时触发了所有相同快捷键的生命周期。这有可能会有危险
     *
     * 不支持为同一生命周期设置多个快捷键
     *
     * @since 2022-9-30
     */
    private GlobalShortcutsFX setShortcut(ShortcutPoint shortcutPoint, JIntellitypeShortcut shortcut)
            throws RepeatedShortcutException, IncompatibleShortcutException {
        if (!this.repeatable && this.register.isShortcutExist(shortcut)) {
            throw new RepeatedShortcutException("异常：此快捷键已存在");
        }
        this.register.registerShortcut(shortcutPoint, shortcut);
        if (!this.register.checkTheLogicOfShortcuts()) {
            throw new IncompatibleShortcutException("异常：此快捷键重复且不兼容");
        }
        return this;
    }

    /**
     * @since 2022-7-27
     */
    public void execute() {
        if (this.globalShortcutsThread == null) {
            this.globalShortcutsThread = new Thread(this::addGlobalShortcuts);
            this.globalShortcutsThread.setName("globalShortcutsThread" + GlobalShortcutsRegister.random.nextInt());
        }
        this.globalShortcutsThread.start(); // 开启子线程来运行
    }

    /**
     * @since 2022-6-25
     * @lastModified 2022-9-30
     */
    private void run() {
        // 此处初始化圈数值应该为 1，而不是 0。因为最开始执行前的显示的圈数也需要从 1 开始
        this.sleepingLoop = 1;
        this.runningLoop = 1;
        while (!this.needTerminate) {
            if (this.isRunning) {
                this.sleepingLoop = 1; // 重置睡眠圈数
                this.runHook(this.beforeEveryLoopStart, this.runningLoop);
                try { // 此处必须使用 try 块吞掉所有可能的异常，否则本线程容易因注入代码抛出异常而无声中止
                    if (this.mainActivityAction == null) {
                        this.runHook(this.logLevelError, "编程错误：mainActivityAction 为 null");
                    } else {
                        this.mainActivityAction.run();
                    }
                } catch (Throwable throwable) {
                    if (!this.runHook(this.onTaskException, throwable)) {
                        this.runHook(this.logLevelError, "customActivity 中抛出了未捕获的异常，程序将被迫中止");
                        this.runHook(this.beforeInstanceExit);
                        this.destroy();
                        this.runHook(this.afterInstanceExit);
                    }
                }
                // 回调必须先于自增语句开始执行，否则第 1 圈会变成第 0 圈
                this.runHook(this.afterEveryLoopFinish, this.runningLoop);
                ++this.runningLoop;
            } else {
                if (this.sleepingLoop == 1) {
                    // 在此处，才能说明任务已经成功被暂停
                    this.runHook(this.afterTaskSuspend);
                }
                this.runningLoop = 1; // 重置运行圈数
                this.runHook(this.beforeEverySleepLoopStart, this.runningLoop);
                try {
                    Thread.interrupted(); // 在休眠前清除中断标志，否则有可能导致接下来的那次休眠失败
                    Thread.sleep(Integer.MAX_VALUE); // 休眠等待恢复运行
                    ++this.sleepingLoop;
                    this.runHook(this.afterEverySleepLoopFinish, this.runningLoop);
                } catch (InterruptedException exception) {
                    this.runHook(this.onIgnoreException, exception);
                    this.sleepingLoop = 1; // 重置睡眠圈数
                }
            }
        }
    }

    /**
     * @since 2022-6-25
     * @lastModified 2024-12-2
     */
    private void addGlobalShortcuts() {
        this.customActivityThread = new Thread(this::run); // 开启子线程来运行
        this.customActivityThread.setName("customActivityThread" + GlobalShortcutsRegister.random.nextInt());
        HotkeyListener hotkeyListener = oneOfShortcutId -> {
            // 查找用户本次快捷键触发的生命周期
            var lifecycles = this.register.getLifecycles(oneOfShortcutId);
            if (lifecycles == null || lifecycles.isEmpty()) {
                // 此分支不应该发生。如果发生了，说明作者的编程出了问题
                this.runHook(this.logLevelError, "编程错误：用户的快捷键没有绑定任何生命周期结点！");
                return;
            }
            if (!this.isStarted && !lifecycles.contains(ShortcutPoint.START)) {
                this.runHook(this.logLevelWarn, "任务任务尚未开始");
                return;
            }
            if (!this.isStarted && lifecycles.contains(ShortcutPoint.START)) {
                // 先执行用户注入的代码，然后才真正执行“开始”
                this.runHook(this.beforeTaskStart);
                this.isStarted = true;
                this.isRunning = true;
                this.customActivityThread.start();
                return;
            }

            if (lifecycles.contains(ShortcutPoint.RESUME)) {
                if (this.isRunning) {
                    this.runHook(this.logLevelWarn, "任务正在运行，不需要恢复运行");
                } else {
                    // 先执行用户注入的代码，然后才真正执行“恢复运行”
                    this.runHook(this.beforeTaskResume);
                    this.isRunning = true;
                    this.customActivityThread.interrupt();
                }
            }

            if (lifecycles.contains(ShortcutPoint.SUSPEND)) {
                if (this.isRunning) {
                    this.runHook(this.beforeTaskSuspend);
                    this.isRunning = false;
                    this.customActivityThread.interrupt();
                    // 因为任务是异步的，所以到此处还不能说明任务已经成功被暂停。所以不能调用
                    // this.runHook(this.afterTaskSuspend);
                } else {
                    this.runHook(this.logLevelWarn, "任务已暂停，不需要再暂停");
                }
            }

            if (lifecycles.contains(ShortcutPoint.EXIT)) {
                this.runHook(this.beforeInstanceExit);
                this.destroy();
                this.runHook(this.afterInstanceExit);
            }

            if (lifecycles.contains(ShortcutPoint.ALL_INSTANCES_EXIT)) {
                this.runHook(this.beforeAllInstancesExit);
                this.register.destroyAllData();
                this.runHook(this.afterAllInstancesExit);
                System.exit(0); // 关闭主程序
            }
        };
        this.register.addHotKeyListener(hotkeyListener); // 添加监听
        this.runHook(this.logLevelInfo, "正在监听快捷键...");
        // 运行本方法的 globalShortcutsThread 线程将在运行完本方法之后立刻就结束了
    }

    /**
     * @since 2024-12-5
     */
    private void runHook(BasicHook hook) {
        if (hook != null) {
            try {
                hook.callback();
            } catch (Throwable throwable) {
                this.runHook(this.logLevelError,
                        "错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
                this.whenOnCallbackException(throwable);
            }
        }
    }

    /**
     * @since 2024-12-5
     */
    private void runHook(EveryLoopHook hook, int runningLoop) {
        if (hook != null) {
            try {
                hook.callback(runningLoop);
            } catch (Throwable throwable) {
                this.runHook(this.logLevelError,
                        "错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
                this.whenOnCallbackException(throwable);
            }
        }
    }

    /**
     * @since 2024-12-5
     */
    private boolean runHook(ExceptionHandler hook, Throwable throwable) {
        if (hook != null) {
            try {
                return hook.handle(throwable);
            } catch (Throwable throwable2) {
                this.runHook(this.logLevelError,
                        "错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
                this.whenOnCallbackException(throwable2);
                return false; // 遇到回调中抛出异常，说明本程序的使用者错误地进行编程。那么程序就没有必要进行执行下去了
            }
        } else {
            return true; // 默认单个任务的执行异常不会影响实例的运行
        }
    }

    /**
     * @since 2024-12-5
     */
    private void runHook(IgnoreExceptionHandler hook, Throwable throwable) {
        if (hook != null) {
            try {
                hook.handle(throwable);
            } catch (Throwable throwable2) {
                this.runHook(this.logLevelError,
                        "错误：onCallbackException 被触发！请不要在回调中抛出异常！请修改代码使得回调中不会抛出异常");
                this.whenOnCallbackException(throwable2);
            }
        }
    }

    /**
     * @since 2024-12-5
     */
    private void runHook(LogCollector hook, String logMessage) {
        if (hook != null) {
            try {
                hook.handle(logMessage);
            } catch (Throwable throwable2) {
                // 此处不能再调用日志回调，否则将陷入回调死循环
                this.whenOnCallbackException(throwable2);
            }
        }
    }

    /**
     * @since 2024-12-5
     */
    private void whenOnCallbackException(Throwable throwable) {
        if (this.onCallbackException != null) {
            try {
                this.onCallbackException.handle(throwable);
            } catch (Throwable throwable2) {
                // 此处不能再调用日志回调，否则将陷入回调死循环
                throw new FatalUsageException("致命错误：禁止在【捕获【在其它回调中抛出的异常】的回调】中再抛出异常。" +
                        "如果不加干涉，这会让程序陷入抛出异常-捕获异常死循环", throwable2);
            }
        }
    }

    /**
     * 此方法只会销毁本实例的资源，但不会调用退出回调。因为退出回调有两种（实例退出和全部实例退出），请在调用本方法之后自行选择相应的回调
     *
     * @since 2024-12-5
     */
    public GlobalShortcutsFX destroy() {
        this.terminateCustomThread();
        this.register.removeRegisteredData();
        return this;
    }

    /**
     * 终止 customActivityThread 线程
     *
     * @since 2022-9-30
     */
    protected GlobalShortcutsFX terminateCustomThread() {
        this.isRunning = false;
        this.needTerminate = true;
        this.customActivityThread.interrupt();
        return this;
    }

    /**
     * 本类不能改成静态的，因为其调用了外部类的方法
     *
     * @since 2022-9-30
     */
    private class GlobalShortcutsRegister {
        public static final Random random = new Random();

        private static final List<GlobalShortcutsRegister> globalRegisters = new CopyOnWriteArrayList<>();

        private final JIntellitype jintellitype = JIntellitype.getInstance();

        private HotkeyListener hotkeyListener = null;

        /**
         * 考虑到允许用户创建本类的多个实例，为了在多个实例下，本类的快捷键标识符不重复，所以设置此字段
         *
         * 使用 DualHashBidiMap 是为了支持通过 id 来反向搜索到 lifecycle。
         * DualHashBidiMap 只能用于 1 对 1 映射，否则只要 key 或 value 存在相同，就会发生相应的覆盖
         *
         * @since 2022-9-30
         */
        private final DualHashBidiMap<ShortcutPoint, Integer> idOfLifecycles = new DualHashBidiMap<>();

        /**
         * 储存每个生命周期对应的快捷键
         *
         * @since 2022-9-30
         */
        private final Map<ShortcutPoint, JIntellitypeShortcut> lifecycleBindShortcut = new ConcurrentHashMap<>();

        /**
         * 储存同一快捷键对应的生命周期
         *
         * @since 2022-9-30
         */
        private final Map<JIntellitypeShortcut, Set<ShortcutPoint>> shortcutBindLifecycle =
                new ConcurrentHashMap<>();

        /**
         * @since 2022-9-30
         */
        public GlobalShortcutsRegister() {
            for (var lifecycle : ShortcutPoint.values()) {
                int code = random.nextInt();
                this.idOfLifecycles.put(lifecycle, code);
            }
            globalRegisters.add(this);
        }

        /**
         * @since 2022-9-30
         */
        public void addHotKeyListener(HotkeyListener listener) {
            this.hotkeyListener = listener;
            this.jintellitype.addHotKeyListener(this.hotkeyListener); // 添加监听
        }

        /**
         * @since 2022-9-30
         */
        public void registerShortcut(ShortcutPoint shortcutPoint, JIntellitypeShortcut shortcut) {
            var id = this.idOfLifecycles.get(shortcutPoint);
            this.jintellitype.registerHotKey(id, shortcut.getModifier(), shortcut.getKeycode());
            this.lifecycleBindShortcut.put(shortcutPoint, shortcut);
            this.collectShortcuts();
        }

        /**
         * 收集有相同快捷键的 lifecycle
         *
         * @since 2022-9-30
         */
        private void collectShortcuts() {
            for (var pair : this.lifecycleBindShortcut.entrySet()) {
                var lifecycle = pair.getKey();
                var shortcut = pair.getValue();
                if (this.shortcutBindLifecycle.containsKey(shortcut)) {
                    var lifecycles = this.shortcutBindLifecycle.get(shortcut);
                    lifecycles.add(lifecycle);
                } else {
                    var lifecycles = new HashSet<ShortcutPoint>();
                    lifecycles.add(lifecycle);
                    this.shortcutBindLifecycle.put(shortcut, lifecycles);
                }
            }
        }

        /**
         * 根据 lifecycle 的 shortcutId，查找与之有相同快捷键的 lifecycle
         *
         * @since 2022-9-30
         */
        public Set<ShortcutPoint> getLifecycles(int shortcutId) {
            // 根据 shortcutId 查找对应的 lifecycle
            var lifecycle = this.idOfLifecycles.getKey(shortcutId);
            var shortcut = this.lifecycleBindShortcut.get(lifecycle);
            return this.shortcutBindLifecycle.get(shortcut);
        }

        /**
         * @since 2022-9-30
         */
        public JIntellitypeShortcut getShortcut(HookPoint hookPoint) {
            return this.lifecycleBindShortcut.get(hookPoint);
        }

        /**
         * @return true 表示检查通过（无错误）
         * @since 2022-9-30
         */
        public boolean checkTheLogicOfShortcuts() {
            for (var lifecycles : this.shortcutBindLifecycle.values()) {
                if (lifecycles.size() == 1) {
                    continue;
                }
                // 如果是将“开始”、“恢复运行”快捷键合并
                if (lifecycles.size() == 2
                        && lifecycles.contains(ShortcutPoint.START)
                        && lifecycles.contains(ShortcutPoint.RESUME)) {
                    continue; // continue 表示合法
                }

                return false; // 只要有上述规定外的其它行为，即视为非法
            }
            return true; // 如果前面没有检测出问题，则视为合法
        }

        /**
         * 判断此快捷键是否已经存在
         *
         * @since 2022-9-30
         */
        public boolean isShortcutExist(JIntellitypeShortcut shortcut) {
            var lifecycles = this.shortcutBindLifecycle.get(shortcut);
            if (lifecycles == null || lifecycles.isEmpty()) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * 此方法不会销毁全局 JIntellitype 线程，只会清除本实例注册过的快捷键
         *
         * @since 2022-9-30
         */
        public void removeRegisteredData() {
            this.jintellitype.removeHotKeyListener(this.hotkeyListener);
            for (var lifecycleId : this.idOfLifecycles.values()) {
                this.jintellitype.unregisterHotKey(lifecycleId);
            }
        }

        /**
         * @since 2022-9-30
         */
        public void callOuterTerminateCustomThread() {
            GlobalShortcutsFX.this.terminateCustomThread();
        }

        /**
         * 销毁所有 GlobalShortcutsFx 实例的数据
         *
         * @since 2022-9-30
         */
        public void destroyAllData() {
            for (var register : globalRegisters) {
                register.callOuterTerminateCustomThread();
                register.removeRegisteredData();
            }
            this.jintellitype.cleanUp();
        }
    }

    /**
     * 此类不能设置为 private，本类不能设置为外部类。本类的方法设置方法均不是线程安全的
     *
     * @since 2024-12-4
     */
    public static class FirstBuilder {
        // 考虑继承，此字段必须使用 protected，否则子类无法继承本类。因为一个对象 super 构造器方法必须先于该对象的任何非构造器方法
        protected final GlobalShortcutsFX globalShortcutsFX;

        /**
         * @since 2024-12-5
         */
        private FirstBuilder() {
            this.globalShortcutsFX = new GlobalShortcutsFX();
        }

        /**
         * @since 2024-12-5
         */
        protected FirstBuilder(GlobalShortcutsFX globalShortcutsFX) {
            this.globalShortcutsFX = globalShortcutsFX;
        }

        /**
         * @since 2024-12-5
         */
        public FirstBuilder withRepeatable(boolean repeatable) {
            this.globalShortcutsFX.repeatable = repeatable;
            return this;
        }

        /**
         * 检查 GlobalShortcutsFX 的必要数据是否已经初始化完全了
         *
         * @since 2024-12-5
         */
        protected void checkIntegrity() {
            // 暂时不需要做什么
        }

        /**
         * @since 2024-12-4
         */
        public SecondBuilder nextBuilder() {
            this.checkIntegrity();
            return new SecondBuilder(this.globalShortcutsFX);
        }
    }

    /**
     * 此类不能设置为 private，本类不能设置为外部类。本类的方法设置方法均不是线程安全的
     *
     * @since 2024-12-4
     */
    public static class SecondBuilder {
        // 考虑继承，此字段必须使用 protected，否则子类无法继承本类。因为一个对象 super 构造器方法必须先于该对象的任何非构造器方法
        protected final GlobalShortcutsFX globalShortcutsFX;

        /**
         * @since 2024-12-5
         */
        protected SecondBuilder(GlobalShortcutsFX globalShortcutsFX) {
            this.globalShortcutsFX = globalShortcutsFX;
        }

        /**
         * 设置自定义任务
         *
         * @since 2024-12-5
         */
        public SecondBuilder setMainActivityAction(Runnable customActivity) {
            if (customActivity == null) {
                throw new FatalUsageException("customActivity 不能为 null");
            }
            this.globalShortcutsFX.mainActivityAction = customActivity;
            return this;
        }

        /**
         * 如果对不同生命周期设置了同一个快捷键，则当此快捷键按下时，视为同时触发了所有相同快捷键的生命周期。这有可能会有危险
         *
         * 不支持为同一生命周期设置多个快捷键
         *
         * @since 2024-12-5
         */
        public SecondBuilder setShortcut(ShortcutPoint shortcutPoint, JIntellitypeShortcut shortcut)
                throws RepeatedShortcutException, IncompatibleShortcutException {
            this.globalShortcutsFX.setShortcut(shortcutPoint, shortcut);
            return this;
        }

        /**
         * 设置自定义任务开始之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeTaskStart(BasicHook callback) {
            this.globalShortcutsFX.beforeTaskStart = callback;
            return this;
        }

        /**
         * 设置自定义任务暂停之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeTaskSuspend(BasicHook callback) {
            this.globalShortcutsFX.beforeTaskSuspend = callback;
            return this;
        }

        /**
         * 设置自定义任务暂停之后的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder afterTaskSuspend(BasicHook callback) {
            this.globalShortcutsFX.afterTaskSuspend = callback;
            return this;
        }

        /**
         * 设置自定义任务恢复运行之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeTaskResume(BasicHook callback) {
            this.globalShortcutsFX.beforeTaskResume = callback;
            return this;
        }

        /**
         * 设置本实例退出之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeInstanceExit(BasicHook callback) {
            this.globalShortcutsFX.beforeInstanceExit = callback;
            return this;
        }

        /**
         * 设置本实例退出之后的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder afterInstanceExit(BasicHook callback) {
            this.globalShortcutsFX.afterInstanceExit = callback;
            return this;
        }

        /**
         * 设置全部实例退出之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeAllInstancesExit(BasicHook callback) {
            this.globalShortcutsFX.beforeAllInstancesExit = callback;
            return this;
        }

        /**
         * 设置本全部实例退出之后的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder afterAllInstancesExit(BasicHook callback) {
            this.globalShortcutsFX.afterAllInstancesExit = callback;
            return this;
        }

        /**
         * 设置自定义任务每圈进入之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeEveryLoopStart(EveryLoopHook callback) {
            this.globalShortcutsFX.beforeEveryLoopStart = callback;
            return this;
        }

        /**
         * 设置自定义任务每圈完成之后的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder afterEveryLoopFinish(EveryLoopHook callback) {
            this.globalShortcutsFX.afterEveryLoopFinish = callback;
            return this;
        }

        /**
         * 设置自定义任务每圈进入睡眠等待状态之前的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder beforeEverySleepLoopStart(EveryLoopHook callback) {
            this.globalShortcutsFX.beforeEverySleepLoopStart = callback;
            return this;
        }

        /**
         * 设置自定义任务每圈睡眠之后的回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder afterEverySleepLoopFinish(EveryLoopHook callback) {
            this.globalShortcutsFX.afterEverySleepLoopFinish = callback;
            return this;
        }

        /**
         * 设置任务级别的异常回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setOnTaskException(ExceptionHandler callback) {
            this.globalShortcutsFX.onTaskException = callback;
            return this;
        }

        /**
         * 设置实例级别的异常回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setOnInstanceException(ExceptionHandler callback) {
            this.globalShortcutsFX.onInstanceException = callback;
            return this;
        }

        /**
         * 设置忽略异常回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setOnIgnoreException(IgnoreExceptionHandler callback) {
            this.globalShortcutsFX.onIgnoreException = callback;
            return this;
        }

        /**
         * 设置【捕获【在其它回调中抛出的异常】的回调】
         *
         * @since 2024-12-5
         */
        public SecondBuilder setOnCallbackException(CallbackExceptionHandler callback) {
            this.globalShortcutsFX.onCallbackException = callback;
            return this;
        }

        /**
         * 设置 INFO 级别的日志信息回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setLogLevelInfo(LogCollector logCollector) {
            this.globalShortcutsFX.logLevelInfo = logCollector;
            return this;
        }

        /**
         * 设置 WARN 级别的日志信息回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setLogLevelWarn(LogCollector logCollector) {
            this.globalShortcutsFX.logLevelWarn = logCollector;
            return this;
        }

        /**
         * 设置 ERROR 级别的日志信息回调
         *
         * @since 2024-12-5
         */
        public SecondBuilder setLogLevelError(LogCollector logCollector) {
            this.globalShortcutsFX.logLevelError = logCollector;
            return this;
        }

        /**
         * 检查 GlobalShortcutsFX 的必要数据是否已经初始化完全了
         *
         * @since 2024-12-5
         */
        protected void checkIntegrity() {
            // 暂时不需要做什么
        }

        /**
         * @since 2024-12-5
         */
        public GlobalShortcutsFX build() {
            this.checkIntegrity();
            return this.globalShortcutsFX;
        }
    }
}
