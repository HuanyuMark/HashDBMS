package org.hashdb.ms.compiler;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.exception.DBExternalException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/25 2:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface CompileStream<R> {

    String errToken(String token);

    static Object normalizeValue(Object result){
        if(result instanceof HValue<?> hValue){
            return hValue.data();
        }
        if(result instanceof List<?> ls){
            return ls.stream().map(CompileStream::normalizeValue).toList();
        }
        if(result instanceof Set<?> ls){
            return ls.stream().map(CompileStream::normalizeValue).toList();
        }
        if(result instanceof Map<?,?> map){
            return map.entrySet().stream().map((entry)-> new PlainPair<>(entry.getKey(),normalizeValue(entry.getValue()))).collect(Collectors.toMap(PlainPair::key,PlainPair::value));
        }
        return result;
    }

    R compile();

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
