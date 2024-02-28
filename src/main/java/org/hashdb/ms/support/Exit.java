package org.hashdb.ms.support;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.slf4j.Logger;

/**
 * Date: 2024/2/22 13:11
 * 专门用来退出程序的异常
 * 一般这样用:
 * <br/>
 * {@code throw Exit.exception();}
 * <br/>
 * {@code throw Exit.error(log,"some exception",causeException)}
 *
 * @author Huanyu Mark
 */
@Slf4j
public class Exit extends RuntimeException {

    private final int status;

    /**
     * @return 退出应用
     */
    public static Exit exception() {
        HashDBMSApp.exit(1);
        return new Exit(1);
    }

    public static Exit error(String msg, Throwable e) {
        return error(log, msg, e.getMessage());
    }

    /**
     * 打印完错误信息后,直接退出应用
     *
     * @param log   Logger
     * @param msg   错误信息
     * @param cause 原因
     * @return 退出对象
     */
    public static Exit error(Logger log, String msg, String cause) {
        log.error(STR."\{msg}. cause: \{cause}");
        return exception();
    }

    /**
     * 正常的退出程序
     */
    public static Exit normal() {
        HashDBMSApp.exit(0);
        return new Exit(0);
    }

    public Exit(int status) {
        this.status = status;
    }

    public void exit() {
        HashDBMSApp.exit(status);
    }
}
