package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/25 3:00
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract sealed class DatabaseCompileStream extends CommonCompileStream<CompileCtx<?>> permits ConsumerCompileStream, SupplierCompileStream {

    private static final int rerunCallbackParallelismExecuteThreshold = Runtime.getRuntime().availableProcessors() > 4 ?
            Runtime.getRuntime().availableProcessors() << 2 : 0;

    protected DatabaseCompileStream fatherStream;

    private List<Runnable> rerunCbs;

//    protected int costExpectant;

    /**
     * 构造主流
     *
     * @param session 会话
     * @param command 原始命令
     */
    protected DatabaseCompileStream(ConnectionSession session, @NotNull String command) {
        super(session);
        if (command.isEmpty()) {
            throw new CommandCompileException("illegal command '" + command + "'");
        }
        var tokens = extractTokens(command);
        this.command = Lazy.of(() -> (String.join(" ", tokens)));
        this.tokens = tokens;
    }

    /**
     * 构造子流
     *
     * @param session         会话
     * @param childTokens     子 tokens
     * @param fatherStream    父 流
     * @param shouldNormalize 是否需要规范化
     */
    protected DatabaseCompileStream(ConnectionSession session, String @NotNull [] childTokens, DatabaseCompileStream fatherStream, boolean shouldNormalize) {
        super(session);
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
        if (log.isTraceEnabled()) {
            log.trace("open compile stream: {}", String.join(" ", tokens));
        }
    }

    protected DatabaseCompileStream(ConnectionSession session, String @NotNull [] childTokens, DatabaseCompileStream fatherStream) {
        this(session, childTokens, fatherStream, true);
    }

    protected DatabaseCompileStream(ConnectionSession session) {
        super(session);
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
        return new SupplierCompileStream(session, childTokens, this);
    }

    public ConsumerCompileStream forkConsumerCompileStream(int startTokenIndex, int endTokenIndex, CompileCtx<?> fatherCompileCtx) {
        if (endTokenIndex < startTokenIndex) {
            log.error("endTokenIndex {} < startTokenIndex {}", endTokenIndex, startTokenIndex);
            throw new DBSystemException();
        }
        // 这里相当于取了字串, 那么母串里被摘出的token需要切掉吗?
        var childTokens = Arrays.stream(tokens).skip(startTokenIndex).limit(endTokenIndex - startTokenIndex + 1).toArray(String[]::new);
        return new ConsumerCompileStream(session, childTokens, this, fatherCompileCtx);
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

    @NotNull
    public Database db() {
        return session.getDatabase();
    }

    @NotNull
    public DatabaseCompileStream rootStream() {
        return fatherStream == null ? this : fatherStream.rootStream();
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

    /**
     * @param cb 在命令被重运行时的回调
     */
    @Override
    public void onRerun(Runnable cb) {
        if (rerunCbs == null) {
            rerunCbs = new LinkedList<>();
        }
        rerunCbs.add(cb);
    }

    @Override
    public void invokeRerunCallback() {
        if (rerunCbs == null) {
            return;
        }
        if (rerunCbs.size() > rerunCallbackParallelismExecuteThreshold) {
            rerunCbs.parallelStream().forEach(Runnable::run);
        } else {
            rerunCbs.forEach(Runnable::run);
        }
    }

    @Override
    public String rerun() {
        if (rerunCbs != null) {
            rerunCbs.parallelStream().forEach(Runnable::run);
        }
        return run();
    }

    @Override
    public CompletableFuture<Object> reExecute() {
        if (rerunCbs == null) {
            return execute();
        }
        if (rerunCbs.size() > rerunCallbackParallelismExecuteThreshold) {
            rerunCbs.parallelStream().forEach(Runnable::run);
        }
        for (Runnable rerunCb : rerunCbs) {
            rerunCb.run();
        }
        return execute();
    }

    /**
     * @return 消耗期望值, 表示执行这个命令预期的所消耗的时间
     */
//    public int costExpectant() {
//        return costExpectant;
//    }
}