package org.wangpai.globalshortcuts.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @since 2022-9-30
 */
@Accessors(chain = true)
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public abstract class GlobalShortcutsException extends RuntimeException {
    private String exceptionMsg;
    private Object data;

    /**
     * 因为本类是抽象类，所以此构造器可以声明为 protected，
     * 但它的非抽象子类的构造器只能声明为 public
     */
    protected GlobalShortcutsException() {
        super();
    }

    protected GlobalShortcutsException(String msg) {
        this(msg, null);
    }

    protected GlobalShortcutsException(String msg, Object obj) {
        super(msg);
        this.setExceptionMsg(msg);
        this.setData(obj);
    }
}
