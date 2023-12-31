package org.hashdb.ms.compiler;

import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.exception.DBClientException;

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
public interface CompileStream<R extends CompilerNode> {

    String errToken(String token);

    static Object normalizeValue(Object result) {
        if (result instanceof HValue<?> hValue) {
            return hValue.data();
        }
        if (result instanceof List<?> ls) {
            return ls.parallelStream().map(CompileStream::normalizeValue).toList();
        }
        if (result instanceof Set<?> ls) {
            return ls.parallelStream().map(CompileStream::normalizeValue).toList();
        }
        if (result instanceof Map<?, ?> map) {
            return map.entrySet().parallelStream().map((entry) -> new PlainPair<>(entry.getKey(), normalizeValue(entry.getValue()))).collect(Collectors.toMap(PlainPair::key, PlainPair::value));
        }
        return result;
    }

    R compile();

    String nearString();

    String command();

    String nextToken() throws ArrayIndexOutOfBoundsException;

    String prevToken();

    String peekToken(int offset, Function<Integer, DBClientException> outOfRange);

    void next();

    void prev();

    int cursor();

    String token(Function<Integer, DBClientException> outOfRange) throws DBClientException;

    String token() throws ArrayIndexOutOfBoundsException;

    int tokenSize();

    void jumpTo(int newCursor);

    void end();

    Iterator<String> tokenItr();

    Iterator<String> tokenItr(int startIndex);

    Iterator<String> descendingTokenItr();

    Iterator<String> descendingTokenItr(int negativeIndex);

    default String run() {
        return null;
    }

    boolean isWrite();

    default String runWithExecutor() {
        return null;
    }
}
