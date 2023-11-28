package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.DBExternalException;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/25 3:00
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class TokenCompileStream implements CompileStream {

    protected final Lazy<String> command;

    /**
     * 存放token的序列可以改成 {@link LinkedList},
     * 再初始化一个{@link LinkedList#listIterator()}, 来作为主遍历器
     * 然后每次调用 {#link #next} {#prev} 的时候, 调用 {@link ListIterator#nextIndex()} 或
     * {@link ListIterator#previousIndex()} 更新cursor即可
     * 但是, 如果在 fork出子流时,使用 {@link LinkedList#subList(int form, int to)} 获取
     * 子链表(这个链表是一个视图, 即对视图的操作会影响到原链表) 这导致了:
     * 因为要去掉包裹内联命令的左右两个括号, 会直接影响到原链表的 token 序列
     */
    protected final String[] tokens;

    protected final Database database;

    protected TokenCompileStream fatherStream;

    protected int cursor = 0;

    /**
     * 构造主流
     *
     * @param database 数据库
     * @param command  原始命令
     */
    protected TokenCompileStream(Database database, @NotNull String command) {
        var tokens = extractTokens(command);
        this.command = Lazy.of(() -> (String.join(" ", tokens)));
        this.tokens = tokens;
        this.database = database;
    }

    /**
     * 构造子流
     *
     * @param database     数据库
     * @param childTokens  子 tokens
     * @param fatherStream 父 流
     */
    protected TokenCompileStream(Database database, String @NotNull [] childTokens, TokenCompileStream fatherStream) {
        if (childTokens.length == 0) {
            log.error("compiler error: father tokens: {} child tokens: {}", childTokens, childTokens);
            throw new DBInnerException("see console. fail to extract child tokens");
        }
        eraseParentheses(childTokens);
        eraseLastSemicolon(childTokens);
        this.command = Lazy.of(() -> String.join(" ", childTokens));
        this.tokens = childTokens;
        this.fatherStream = fatherStream;
        this.database = database;
        if (log.isTraceEnabled()) {
            log.trace("open compile stream: {}", String.join(" ", tokens));
        }
    }

    /**
     * 解析命令字符串, 按照json字符串, 空格分割 的规则, 进行分割
     *
     * @param command 原始命令字符串
     */
    protected static String @NotNull [] extractTokens(@NotNull String command) {
        List<String> tokenList = new LinkedList<>();
        List<String> wordQueue = new LinkedList<>();
        List<Symbol> jsonSymbolQueue = new LinkedList<>();
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
                            String token = java.lang.String.join("", wordQueue);
                            tokenList.add(token);
                            wordQueue.clear();
                        }
                        continue;
                    }
                }
                case "[" -> jsonSymbolQueue.add(new Symbol(index, "["));
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
                case "{" -> jsonSymbolQueue.addLast(new Symbol(index, "{"));
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
                        jsonSymbolQueue.add(new Symbol(index, "\""));
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
        eraseLastSemicolon(tokens);
        eraseParentheses(tokens);
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

    /**
     * {@param startTokenIndex} 与 {@param endTokenIndex} 最后都会被包含在子流中
     *
     * @param startTokenIndex 开始包括的token索引
     * @param endTokenIndex   结束不包括的token索引
     */
    @Override
    public SupplierCompileStream forkSupplierCompileStream(int startTokenIndex, int endTokenIndex) {
        if (endTokenIndex < startTokenIndex) {
            log.error("endTokenIndex {} < startTokenIndex {}", endTokenIndex, startTokenIndex);
            throw new DBInnerException();
        }
        var childTokens = Arrays.stream(tokens).skip(startTokenIndex).limit(endTokenIndex - startTokenIndex + 1).toArray(String[]::new);
        return new SupplierCompileStream(database, childTokens, this);
    }

    public ConsumerCompileStream forkConsumerCompileStream(int startTokenIndex, int endTokenIndex, CompileCtx<?> fatherCompileCtx) {
        if (endTokenIndex < startTokenIndex) {
            log.error("endTokenIndex {} < startTokenIndex {}", endTokenIndex, startTokenIndex);
            throw new DBInnerException();
        }
        // 这里相当于取了字串, 那么母串里被摘出的token需要切掉吗?
        var childTokens = Arrays.stream(tokens).skip(startTokenIndex).limit(endTokenIndex - startTokenIndex + 1).toArray(String[]::new);
        return new ConsumerCompileStream(database, childTokens, this, fatherCompileCtx);
    }

    @Override
    public String errToken(String token) {
        return " msg: {\"token\":\"" + token + "\",\"near\":\"" + nearString() + "\"}";
    }

    @Override
    public String nearString() {
        int realCursor = cursor;
        var printTokens = tokens;
        var fs = fatherStream;
        while (fs != null) {
            realCursor += fs.cursor();
            printTokens = fs.tokens;
            fs = fs.fatherStream;
        }
        return Arrays.stream(printTokens).limit(realCursor).collect(Collectors.joining(" "));
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
    public String peekToken(int offset, Function<Integer, DBExternalException> outOfRange) {
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
    public String token(Function<Integer, DBExternalException> outOfRange) throws DBExternalException {
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
        return new TokenItr(tokens);
    }

    @Override
    public TokenItr tokenItr(int startIndex) {
        return new TokenItr(tokens, startIndex);
    }

    @Override
    public TokenItr descendingTokenItr() {
        return new DescTokenItr(tokens);
    }

    @Override
    public TokenItr descendingTokenItr(int negativeIndex) {
        return new DescTokenItr(tokens, negativeIndex);
    }

    public Database db() {
        return database;
    }

    protected record Symbol(int index, String content) {
        @Override
        public String toString() {
            return "(symbol: '" + content + "' at index " + index + ")";
        }
    }

    public static class TokenItr implements Iterator<String> {
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

    public static class DescTokenItr extends TokenItr {
        private int cursor;

        public DescTokenItr(String[] tokens) {
            super(tokens);
            cursor = tokens.length;
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
            return tokens[--cursor];
        }

        public int cursor() {
            return cursor;
        }
    }
}
