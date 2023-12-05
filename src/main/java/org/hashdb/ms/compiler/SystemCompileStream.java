package org.hashdb.ms.compiler;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.exception.CommandCompileException;
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

    final ConnectionSession session;

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

    @Override
    public SystemCompileCtx<?> compile() {
        SystemCompileCtx<?> compileCtx = SystemKeyword.createCtx(token());
        if (compileCtx == null) {
            return null;
        }
        next();
        compileCtx.interpretWith(this);
        return compileCtx;
    }

    @Override
    public String nearString() {
        return "";
    }

    @Override
    public String submit() {
        SystemCompileCtx<?> compileCtx = compile();
        if (compileCtx == null) {
            return null;
        }
        Object result = compileCtx.getResult();
        if (result instanceof Boolean ok) {
            return ok ? "SUCC" : "FAIL";
        }
        Object normalizeValue = CompileStream.normalizeValue(result);
        return JsonService.stringfy(normalizeValue == null ? "null" : normalizeValue);
    }
}
