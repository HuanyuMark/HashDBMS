package org.hashdb.ms.compiler;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.util.Lazy;

/**
 * Date: 2023/11/30 1:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SystemCompileStream extends CommonCompileStream {
    public SystemCompileStream(String command) {
        this.tokens = extractTokens(command);
        this.command = Lazy.of(() -> String.join(" ", tokens));
        eraseLastSemicolon(tokens);
        eraseParentheses(tokens);
    }

    @Override
    public String errToken(String token) {
        return null;
    }

    @Override
    public CompileCtx<?> compile() {
        return null;
    }

    @Override
    public String nearString() {
        return null;
    }

}
