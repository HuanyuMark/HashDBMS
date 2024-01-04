package org.hashdb.ms.compiler;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBCreateCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.ReadonlyConnectionSession;
import org.hashdb.ms.net.TransportableConnectionSession;
import org.hashdb.ms.sys.DBSystem;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 1:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public final class SystemCompileStream extends CommonCompileStream<SystemCompileCtx<?>> {
    private static final Lazy<DBSystem> SYSTEM = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    /**
     * 如果该流编译出的指令是读指令, 那么这个 Session 就不为空, 否则为空
     */
    private final ReadonlyConnectionSession session;

    /**
     * 只有写命令会有这个编译结果
     */
    private SystemCompileCtx<?> compileResult;

    public ReadonlyConnectionSession getSession() {
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

    private SystemCompileStream(SystemCompileCtx<?> compileResult, ReadonlyConnectionSession session) {
        this.session = session;
        this.compileResult = compileResult;
    }

    static SystemCompileStream createWithTransportable(SystemCompileCtx<?> compileResult, TransportableConnectionSession session) {
        return new SystemCompileStream(compileResult, session);
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
    @Nullable
    public String run() {
        SystemCompileCtx<?> compileCtx = compile();
        if (compileCtx == null) {
            return null;
        }
        Object result;
        if (compileCtx instanceof DBCreateCtx dbCreateCtx) {
            result = SYSTEM.get().submitOpsTaskSync(dbCreateCtx.getResult());
        } else {
            assert compileCtx.getResult() != null;
            result = compileCtx.getResult().get();
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
        Object result = SYSTEM.get().submitOpsTaskSync(compileResult.executor(session));
        if (result instanceof Boolean ok) {
            return ok ? "SUCC" : "FAIL";
        }
        Object normalizeValue = CompileStream.normalizeValue(result);
        return JsonService.stringfy(normalizeValue == null ? "null" : normalizeValue);
    }
}
