package org.hashdb.ms.compiler;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.exception.DBExternalException;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Date: 2023/11/25 2:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface CompileStream {

    String errToken(String token);

    CompileCtx<?> compile();

    String nearString();

    String command();

    String nextToken() throws ArrayIndexOutOfBoundsException;

    String prevToken();

    String peekToken(int offset, Function<Integer, DBExternalException> outOfRange);

    void next();

    void prev();

    int cursor();

    String token(Function<Integer, DBExternalException> outOfRange) throws DBExternalException;

    String token() throws ArrayIndexOutOfBoundsException;

    int tokenSize();

    void jumpTo(int newCursor);

    void end();

    Iterator<String> tokenItr();

    Iterator<String> tokenItr(int startIndex);

    Iterator<String> descendingTokenItr();

    Iterator<String> descendingTokenItr(int negativeIndex);

}
