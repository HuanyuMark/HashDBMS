package org.hashdb.ms.compiler;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.ConnectionSessionModel;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * Date: 2023/11/30 1:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class CommonCompileStream<R extends CompilerNode> implements CompileStream<R> {

    protected final ConnectionSessionModel session;

    protected Lazy<String> command;
    /**
     * 存放token的序列可以改成 {@link LinkedList},
     * 再初始化一个{@link LinkedList#listIterator()}, 来作为主遍历器
     * 然后每次调用 {#link #next} {#prev} 的时候, 调用 {@link ListIterator#nextIndex()} 或
     * {@link ListIterator#previousIndex()} 更新cursor即可
     * 但是, 如果在 fork出子流时,使用 {@link LinkedList#subList(int form, int to)} 获取
     * 子链表(这个链表是一个视图, 即对视图的操作会影响到原链表) 这导致了:
     * 因为要去掉包裹内联命令的左右两个括号, 会直接影响到原链表的 token 序列
     */
    @JsonProperty
    protected String[] tokens;
    protected int cursor = 0;

    @JsonProperty
    protected boolean write = false;

    protected CommonCompileStream(ConnectionSessionModel session) {
        this.session = session;
    }

    /**
     * 解析命令字符串, 按照json字符串, 空格分割 的规则, 进行分割
     *
     * @param command 原始命令字符串
     */
    protected static String @NotNull [] extractTokens(@NotNull String command) {
        List<String> tokenList = new LinkedList<>();
        List<String> wordQueue = new LinkedList<>();
        List<DatabaseCompileStream.Symbol> jsonSymbolQueue = new LinkedList<>();
//        LinkedList<Symbol> parenthesesQueue = new LinkedList<>();
        // 其实也可以用 循环 substring 来取得每个字符(UTF-8字符)的字符串
        String[] charts = command.split("");
        for (int index = 0; index < charts.length; index++) {
            String curStr = charts[index];
            switch (curStr) {
                case " " -> {
                    // token may be  SET k1 v1 k2 v2
                    if (jsonSymbolQueue.isEmpty()) {
                        if (!wordQueue.isEmpty()) {
                            String token = String.join("", wordQueue);
                            tokenList.add(token);
                            wordQueue.clear();
                        }
                        continue;
                    }
                }
                case "[" -> jsonSymbolQueue.add(new DatabaseCompileStream.Symbol(index, "["));
                case "]" -> {
                    // token may be [1, 2 ,   3]
                    if (jsonSymbolQueue.isEmpty()) {
                        throw new CommandCompileException("missing open symbol '['. match close symbol ']' in command '" + command + "' at index " + index + "");
                    }
                    wordQueue.add(curStr);
                    if ("[".equals(jsonSymbolQueue.getLast().content())) {
                        jsonSymbolQueue.removeLast();
                        if (jsonSymbolQueue.isEmpty()) {
                            String token = String.join("", wordQueue);
                            tokenList.add(token);
                            wordQueue.clear();
                        }
                    }
                    continue;
                }
                case "{" -> jsonSymbolQueue.addLast(new DatabaseCompileStream.Symbol(index, "{"));
                case "}" -> {
                    // token may be {   "a":123, "b": [1, "789", {}]}
                    if (jsonSymbolQueue.isEmpty()) {
                        throw new CommandCompileException("missing open symbol '{'. match close symbol '}' in command '" + command + "' at index " + index + "");
                    }
                    wordQueue.add(curStr);
                    if ("{".equals(jsonSymbolQueue.getLast().content)) {
                        jsonSymbolQueue.removeLast();
                        if (jsonSymbolQueue.isEmpty()) {
                            String token = String.join("", wordQueue);
                            tokenList.add(token);
                            wordQueue.clear();
                        }
                    }
                    continue;
                }
                case "\"" -> {
                    // token may be "str   str"
                    if (jsonSymbolQueue.isEmpty()) {
                        jsonSymbolQueue.add(new DatabaseCompileStream.Symbol(index, "\""));
                    } else if ("\"".equals(jsonSymbolQueue.getLast().content())) {
                        jsonSymbolQueue.removeLast();
                    }
                }
            }
            wordQueue.add(curStr);
        }
        if (!jsonSymbolQueue.isEmpty()) {
            throw new CommandCompileException("fail to match json symbol: '" + jsonSymbolQueue + "' in command: '" + command + "'");
        } else if (!wordQueue.isEmpty()) {
            tokenList.add(String.join("", wordQueue));
        }

        if (tokenList.isEmpty()) {
            throw new CommandCompileException("illegal command '" + command + "'");
        }

        var tokens = tokenList.toArray(String[]::new);
        CommonCompileStream.eraseLastSemicolon(tokens);
        CommonCompileStream.eraseParentheses(tokens);
        if (log.isTraceEnabled()) {
            log.trace("open compile stream: {}", String.join(" ", tokens));
        }
        return tokens;
    }

    /**
     * 消除tokens里最后一个token的末尾分号
     */
    protected static void eraseLastSemicolon(String @NotNull [] tokens) {
        int lastTokenIndex = tokens.length - 1;
        var lastToken = tokens[lastTokenIndex];
        int lastTokenLastCharIndex = lastToken.length() - 1;
        if (';' == lastToken.charAt(lastTokenLastCharIndex)) {
            tokens[lastTokenIndex] = lastToken.substring(0, lastTokenLastCharIndex);
        }
    }

    /**
     * 去掉所有嵌套的左右括号, 比如 "((()))" 这种, 但是 "(( ()))" 这种是非法的, 必须严格对称匹配才行
     */
    protected static void eraseParentheses(String @NotNull [] tokens) {
        int last = tokens.length - 1;
        while (true) {
            var firstToken = tokens[0];
            var lastToken = tokens[last];
            int lastIndexOfLastToken = lastToken.length() - 1;
            if (firstToken.charAt(0) != '(' || lastToken.charAt(lastIndexOfLastToken) != ')') {
                break;
            }
            try {
                tokens[0] = firstToken.substring(1);
                tokens[last] = lastToken.substring(0, lastIndexOfLastToken);
            } catch (IndexOutOfBoundsException e) {
                throw new CommandCompileException("fail to match '()' on tokens: '[" + firstToken + "," + lastToken + "]' of command '" + String.join(" ", tokens) + "'");
            }
        }
    }

    @Override
    public String errToken(String token) {
        return " msg: {\"token\":\"" + token + "\",\"near\":\"" + nearString() + "\"}";
    }

    @Override
    public String command() {
        return command.get();
    }

    @Override
    public String nextToken() throws ArrayIndexOutOfBoundsException {
        return tokens[++cursor];
    }

    @Override
    public String prevToken() {
        return tokens[--cursor];
    }

    @Override
    public String peekToken(int offset, Function<Integer, DBClientException> outOfRange) {
        int i = cursor + offset;
        try {
            return tokens[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw outOfRange.apply(i);
        }
    }

    @Override
    public void next() {
        ++cursor;
    }

    @Override
    public void prev() {
        --cursor;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public String token(Function<Integer, DBClientException> outOfRange) throws DBClientException {
        try {
            return token();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw outOfRange.apply(cursor);
        }
    }

    @Override
    public String token() throws ArrayIndexOutOfBoundsException {
        return tokens[cursor];
    }


    @Override
    public int tokenSize() {
        return tokens.length;
    }

    @Override
    public void jumpTo(int newCursor) {
        cursor = newCursor;
    }

    @Override
    public void end() {
        cursor = tokens.length;
    }

    @Override
    public TokenItr tokenItr() {
        return new TokenItr();
    }

    @Override
    public TokenItr tokenItr(int startIndex) {
        return new TokenItr(startIndex);
    }

    @Override
    public TokenItr descendingTokenItr() {
        return new DescTokenItr();
    }

    @Override
    public TokenItr descendingTokenItr(int negativeIndex) {
        return new DescTokenItr(negativeIndex);
    }

    public void reset() {
        cursor = 0;
    }

    protected record Symbol(int index, String content) {
        @Override
        public String toString() {
            return "(symbol: '" + content + "' at index " + index + ")";
        }
    }


    public class TokenItr implements Iterator<String> {
        protected int cursor;

        public TokenItr() {
        }

        public TokenItr(int startIndex) {
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

    public class DescTokenItr extends TokenItr {
        public DescTokenItr() {
            super(tokens.length);
        }

        public DescTokenItr(int negativeStartIndex) {
            super(tokens.length + negativeStartIndex);
        }

        @Override
        public boolean hasNext() {
            return cursor >= 0;
        }

        @Override
        public String next() {
            return tokens[--cursor];
        }

        public int cursor() {
            return cursor;
        }
    }

    @Override
    public boolean isWrite() {
        return write;
    }

    public ConnectionSessionModel session() {
        return session;
    }

    public void toWrite() {
        write = true;
    }
}
