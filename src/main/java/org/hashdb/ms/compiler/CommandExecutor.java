package org.hashdb.ms.compiler;

import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.ConnectionSessionModel;
import org.hashdb.ms.net.exception.NoDatabaseSelectedException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

/**
 * Date: 2023/11/30 14:10
 * 每一个会话都需要有一个 SystemCompileStreamFactory 来生产 编译流
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CommandExecutor {
    private final ConnectionSessionModel session;

//    private final boolean useCache;

    protected CommandExecutor(ConnectionSessionModel session) {
        this.session = session;
//        this.useCache = session.getLocalCommandCache().getCacheSize() >= 0 && session.getLocalCommandCache().getAliveTime() >= -1;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull CommandExecutor create(ConnectionSessionModel session) {
        return new CommandExecutor(session);
    }

    /**
     * 在本地立即执行命令
     * 因为现在网络层需要提前编译命令, 分析命令是读还是写, 所以, 一般都不能直接在本地运行.
     * 所以需要使用 {@link #compile(String command)} 提前编译命令, 拿到可传输的编译结果
     * 再判断读/写, 再根据该服务器的主从属性, 决定是否要发送编译结果, 若发送, 则编译结果会在
     * 接收端执行. 否则, 可以调用 {@link TransportableCompileResult#run()} 在本机立即执行
     */
    @Deprecated
    public String run(String command) throws ExecutionException {
        return runWithCache(session, command);
    }

    public static String runWithCache(ConnectionSessionModel session, String command) {
        // 从缓存中取出编译结果
        var transientValue = session.getLocalCommandCache().get(command);
        if (transientValue != null) {
            return transientValue.get().rerun();
        }
        var compileStream = new SystemCompileStream(session, command);
        var execRes = compileStream.run();
        if (execRes != null) {
            return execRes;
        }
        var db = session.getDatabase();
        if (db == null) {
            throw NoDatabaseSelectedException.of();
        }
        var supplierCompileStream = new SupplierCompileStream(session, compileStream.tokens, null, false);
        // 存入缓存
        session.getLocalCommandCache().putRaw(command, supplierCompileStream);
        return supplierCompileStream.run();
    }

    /**
     * 命令经过编译后, 就可以判断出命令的读写属性, 网络层需要使用该方法
     *
     * @param command 命令
     * @return 可传输的编译结果
     */
    public TransportableCompileResult compile(String command) {
        // 查询缓存
        var transientValue = session.getLocalCommandCache().get(command);
        if (transientValue != null) {
            return new TransportableCompileResult(transientValue.get(), true);
        }
        var compileStream = new SystemCompileStream(session, command);
        var systemCompileCtx = compileStream.compile();
        if (systemCompileCtx != null) {
            return new TransportableCompileResult(compileStream, false);
        }
        var db = session.getDatabase();
        if (db == null) {
            throw new DBClientException("No database selected");
        }
        var supplierCompileStream = new SupplierCompileStream(session, compileStream.tokens, null, false);
        // 放入缓存
        session.getLocalCommandCache().putRaw(command, supplierCompileStream);
        return new TransportableCompileResult(supplierCompileStream, false);
    }
}
