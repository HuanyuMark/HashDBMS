package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/25 3:00
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract sealed class DatabaseCompileStream extends CommonCompileStream<CompileCtx<?>> permits ConsumerCompileStream, SupplierCompileStream {

    protected final Database database;

    protected DatabaseCompileStream fatherStream;

    /**
     * 构造主流
     *
     * @param database 数据库
     * @param command  原始命令
     */
    protected DatabaseCompileStream(Database database, @NotNull String command) {
        var tokens = extractTokens(command);
        this.command = Lazy.of(() -> (String.join(" ", tokens)));
        this.tokens = tokens;
        this.database = database;
    }

    /**
     * 构造子流
     *
     * @param database        数据库
     * @param childTokens     子 tokens
     * @param fatherStream    父 流
     * @param shouldNormalize 是否需要规范化
     */
    protected DatabaseCompileStream(Database database, String @NotNull [] childTokens, DatabaseCompileStream fatherStream, boolean shouldNormalize) {
        if (childTokens.length == 0) {
            log.error("compiler error: father tokens: {} child tokens: {}", childTokens, childTokens);
            throw new DBSystemException("see console. fail to extract child tokens");
        }
        if (shouldNormalize) {
            eraseParentheses(childTokens);
            eraseLastSemicolon(childTokens);
        }
        this.command = Lazy.of(() -> String.join(" ", childTokens));
        this.tokens = childTokens;
        this.fatherStream = fatherStream;
        this.database = database;
        if (log.isTraceEnabled()) {
            log.trace("open compile stream: {}", String.join(" ", tokens));
        }
    }

    protected DatabaseCompileStream(Database database, String @NotNull [] childTokens, DatabaseCompileStream fatherStream) {
        this(database, childTokens, fatherStream, true);
    }

    protected DatabaseCompileStream(Database database) {
        this.database = database;
    }

    /**
     * {@param startTokenIndex} 与 {@param endTokenIndex} 最后都会被包含在子流中
     *
     * @param startTokenIndex 开始包括的token索引
     * @param endTokenIndex   结束不包括的token索引
     */
    public SupplierCompileStream forkSupplierCompileStream(int startTokenIndex, int endTokenIndex) {
        if (endTokenIndex < startTokenIndex) {
            log.error("endTokenIndex {} < startTokenIndex {}", endTokenIndex, startTokenIndex);
            throw new DBSystemException();
        }
        var childTokens = Arrays.stream(tokens).skip(startTokenIndex).limit(endTokenIndex - startTokenIndex + 1).toArray(String[]::new);
        return new SupplierCompileStream(database, childTokens, this);
    }

    public ConsumerCompileStream forkConsumerCompileStream(int startTokenIndex, int endTokenIndex, CompileCtx<?> fatherCompileCtx) {
        if (endTokenIndex < startTokenIndex) {
            log.error("endTokenIndex {} < startTokenIndex {}", endTokenIndex, startTokenIndex);
            throw new DBSystemException();
        }
        // 这里相当于取了字串, 那么母串里被摘出的token需要切掉吗?
        var childTokens = Arrays.stream(tokens).skip(startTokenIndex).limit(endTokenIndex - startTokenIndex + 1).toArray(String[]::new);
        return new ConsumerCompileStream(database, childTokens, this, fatherCompileCtx);
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

    public String fatherCommand() {
        if (fatherStream == null) {
            return "";
        }
        return fatherStream.command();
    }

    public Database db() {
        return database;
    }

    @Override
    public void toWrite() {
        // 不调用父流的toWrite,减小递归消耗. 所以要先探测
        if (write) {
            return;
        }
        write = true;
        if (fatherStream != null) {
            fatherStream.toWrite();
        }
    }
}
