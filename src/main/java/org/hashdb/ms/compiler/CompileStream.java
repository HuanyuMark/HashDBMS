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
    SupplierCompileStream forkSupplierCompileStream(int startTokenIndex, int endTokenIndex);

    String errToken(String token);

    CompileCtx compile();

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

    TokenItr tokenItr();

    TokenItr tokenItr(int startIndex);

    TokenItr descendingTokenItr();

    TokenItr descendingTokenItr(int negativeIndex);


    class TokenItr implements Iterator<String> {
        protected final String[] tokens;
        protected int cursor = 0;

        public TokenItr(String[] tokens) {
            this.tokens = tokens;
        }

        public TokenItr(String[] tokens, int startIndex) {
            this.tokens = tokens;
            cursor = startIndex;
        }
        @Override
        public boolean hasNext() {
            return cursor < tokens.length;
        }

        @Override
        public String next() {
            return tokens[cursor++];
        }

        public int cursor() {
            return cursor;
        }
    }

    class DescTokenItr extends TokenItr {
        private int cursor;

        public DescTokenItr(String[] tokens) {
            super(tokens);
            cursor = tokens.length - 1;
        }

        public DescTokenItr(String[] tokens, int negativeStartIndex) {
            super(tokens);
            cursor = tokens.length + negativeStartIndex;
        }
        @Override
        public boolean hasNext() {
            return cursor >= 0;
        }
        @Override
        public String next() {
            return tokens[cursor--];
        }
        public int cursor() {
            return cursor;
        }
    }
}
