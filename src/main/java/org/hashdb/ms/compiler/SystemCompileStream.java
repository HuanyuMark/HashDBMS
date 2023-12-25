package org.hashdb.ms.compiler;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBCreateCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBShowCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBUseCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.sys.DBSystem;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Lazy;

/**
 * Date: 2023/11/30 1:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public final class SystemCompileStream extends CommonCompileStream<SystemCompileCtx<?>> {
    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    private static final Object NULL_RESULT = new Object();

    /**
     * 如果该流编译出的指令是读指令, 那么这个 Session 就不为空, 否则为空
     */
    private final ConnectionSession session;

    /**
     * 只有写命令会有这个编译结果
     */
    private SystemCompileCtx<?> compileResult;

    public ConnectionSession getSession() {
        return session;
    }

    public SystemCompileStream(ConnectionSession session, String command) {
        this.session = session;
        this.tokens = extractTokens(command);
        if (tokens.length == 0) {
            throw new CommandCompileException("illegal command '" + command + "'");
        }
        this.command = Lazy.of(() -> String.join(" ", tokens));
        eraseLastSemicolon(tokens);
        eraseParentheses(tokens);
    }

    private SystemCompileStream(SystemCompileCtx<?> compileResult) {
        this.session = null;
        this.compileResult = compileResult;
    }

    static SystemCompileStream createWithTransportable(SystemCompileCtx<?> compileResult) {
        return new SystemCompileStream(compileResult);
    }

    @Override
    public SystemCompileCtx<?> compile() {
        SystemCompileCtx<?> compileCtx = SystemKeyword.createCtx(token());
        if (compileCtx == null) {
            return null;
        }
        next();
        compileCtx.compileWithStream(this);
        return compileCtx;
    }

    @Override
    public String nearString() {
        return "";
    }


    @Override
    public String run() {
        SystemCompileCtx<?> compileCtx = compile();
        if (compileCtx == null) {
            return null;
        }
        Object result = NULL_RESULT;
        if (compileCtx instanceof DBUseCtx dbUseCtx) {
            assert dbUseCtx.getResult() != null;
            result = dbUseCtx.getResult().get();
        }
        if (compileCtx instanceof DBCreateCtx dbCreateCtx) {
            result = SYSTEM.get().submitOpsTaskSync(dbCreateCtx.getResult());
        }
        if (compileCtx instanceof DBShowCtx dbShowCtx) {
            assert dbShowCtx.getResult() != null;
            result = dbShowCtx.getResult().get();
        }
        if (NULL_RESULT == result) {
            throw new DBSystemException("unexpected result");
        }
        if (result instanceof Boolean ok) {
            return ok ? "SUCC" : "FAIL";
        }
        Object normalizeValue = CompileStream.normalizeValue(result);
        return JsonService.stringfy(normalizeValue == null ? "null" : normalizeValue);
    }

    @Override
    public String runWithExecutor() {
        // 一般都是数据库系统的写命令, 所以就扔进队列里执行
        Object result = SYSTEM.get().submitOpsTaskSync(compileResult.executor());
        if (result instanceof Boolean ok) {
            return ok ? "SUCC" : "FAIL";
        }
        Object normalizeValue = CompileStream.normalizeValue(result);
        return JsonService.stringfy(normalizeValue == null ? "null" : normalizeValue);
    }
}
