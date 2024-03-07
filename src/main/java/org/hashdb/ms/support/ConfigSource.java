package org.hashdb.ms.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.StringKits;
import org.hashdb.ms.util.YamlService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Date: 2024/2/20 23:40
 *
 * @author Huanyu Mark
 */
@Slf4j
public abstract class ConfigSource implements AutoCloseable {

    /**
     * 没有分区, 时间换空间
     */
    protected final StringBuilder buffer = new StringBuilder();

    protected static final String lineSeparator = "\n";
    // 有分区, 但是要耗费更多空间, 空间换时间
    // protected final StringBuilder[] blockBuffers = IntStream.range(0, Block.values.length*3).mapToObj(i -> new StringBuilder()).toArray(StringBuilder[]::new);

    /**
     * @param base 操作的这个文件的对象只能有一个, 保证线程安全
     */
    public static ConfigSource open(@NotNull File base) {
        return new FileConfigSource(base);
    }

    public static ConfigSource open(URL url) {
        return new URLConfigSource(url);
    }

    protected ConfigSource() {
    }

    public void transferTo(File dest) throws IOException {
        try (var writer = Files.newBufferedWriter(dest.toPath(), StandardCharsets.UTF_8)) {
            writer.write(buffer.toString());
        }
    }

    private final AtomicReferenceArray<Object> contents = new AtomicReferenceArray<>(Mark.values.length);

    private final ScheduledFuture<?> updateThrottler = AsyncService.setInterval(() -> {
        boolean shouldFlush = false;
        for (int i = 0; i < contents.length(); i++) {
            var content = contents.getAndUpdate(i, c -> null);
            if (content == null) continue;
            shouldFlush = true;
            try {
                doUpdate(Mark.values[i], content);
            } catch (Exception e) {
                log.error("updater: ", e);
                return;
            }
        }
        if (shouldFlush) {
            flush();
        }
    }, 1000);

    public void update(Mark mark, Object content) {
        contents.set(mark.ordinal(), content);
    }

    /**
     * @param content 被 {@link Block} 标注过的类的实例
     */
    public void updateAnnotated(@NotNull Object content) {
        var position = AnnotationUtils.findAnnotation(content.getClass(), Block.class);
        if (position == null) {
            throw new DBSystemException(STR."class '\{content.getClass()}' should be annotated with '\{Block.class}'");
        }
        update(position.value(), content);
    }

    private void doUpdate(Mark mark, Object content) {
        var body = yamlBody(mark.wrap(content));
        var startIndex = matchStart(mark);
        // 如果没有匹配到该文本块
        if (startIndex < 0) {
            if (mark.comment() != null) {
                buffer.append(mark.comment());
            }
            buffer.append(body);
        } else {
            // 成功匹配. 接下来匹配与startLine配对的endLine
            int startReplaceIndex;
            int lastLineIndex = buffer.lastIndexOf(lineSeparator, startIndex);
            int commentIndex = lastLineIndex - mark.rawComment().length();
            var commentLikeLine = buffer.substring(commentIndex, lastLineIndex).trim();

            if (!commentLikeLine.startsWith("#")) {
                buffer.insert(startIndex, mark.comment());
                startReplaceIndex = startIndex + mark.comment().length();
            } else if (!mark.rawComment().equals(commentLikeLine)) {
                buffer.replace(commentIndex, lastLineIndex, mark.rawComment());
                startReplaceIndex = startIndex + (lastLineIndex - commentIndex - mark.rawComment().length());
            } else {
                startReplaceIndex = startIndex;
            }
            int endIndex = buffer.indexOf(mark.endMark(), startIndex);
            if (endIndex < 0) {
                throw Exit.error(log, STR."can not automatically update config '\{mark}'", STR." config mark '\{mark}' is invalid. " +
                        STR."require line '\{mark.endMark()}' to close the mark. please add end line '\{mark.endMark()}' to mark the end of config '\{mark}'");
            }
            buffer.replace(startReplaceIndex, endIndex + mark.endMark().length() + 1 + lineSeparator.length(), body);
        }
    }

    private static String yamlBody(Object content) {
        String body;
        try {
            body = YamlService.toStringWriter(content).toString();
        } catch (Exception e) {
            throw Exit.error(log, STR."can not convert '\{content}' to yaml text", e);
        }
        return body;
    }

    /**
     * 给 {@link #matchStart(Mark)} 用以加快匹配速度.
     * 有几率匹配错误
     */
    private final int[] matchIndexMarks = new int[Mark.values.length];

    private int matchStart(Mark mark) {
        int startIndex;
        // 使用这个marks, 匹配的start字符串可能就不是出现在全文档中的第一次出现的字符串
        int oldStartIndex = matchIndexMarks[mark.ordinal()];
        boolean retry = false;
        // 匹配顶格的startLine
        while (true) {
            startIndex = buffer.indexOf(mark.startMark(), oldStartIndex);
            if (startIndex < 0) {
                if (retry) {
                    return startIndex;
                }
                retry = true;
                matchIndexMarks[mark.ordinal()] = 0;
                oldStartIndex = 0;
                continue;
            }
            int separatorIndex = buffer.lastIndexOf(lineSeparator, startIndex);
            // 顶格
            if ("\n\r".indexOf(buffer.charAt(startIndex - 1)) != -1 || StringKits.isBlank(buffer, separatorIndex - 1, startIndex)) {
                matchIndexMarks[mark.ordinal()] = startIndex;
                return startIndex;
            }
            oldStartIndex = startIndex;
        }
    }

    protected void flush() {
        try (var writer = newWriter()) {
            if (writer instanceof BufferedWriter bufferedWriter) {
                writer.write(buffer.toString());
            }
        } catch (IOException e) {
            throw Exit.error(log, "can not automatically update config file", e);
        }
    }

    abstract protected Writer newWriter() throws IOException;

    abstract public String toString();

    @Override
    public void close() throws Exception {
        updateThrottler.cancel(true);
    }

    public @interface Block {
        Mark value();
    }

    public enum Mark {
        CLUSTER,
        SENTINEL,
        REPLICATION,
        ;
        private final String comment;

        private final String rawComment;
        private final String startMark;

        private final String endMarkKey;

        private final static String endMarkValue = "(DONT EDIT)";
        private final String endMark;
        private static final Mark[] values = values();

        Mark() {
            this("Generated By Hash DB. Don`t write comments here, they would be override.");
        }

        /**
         * @param comment 在start前要注明的文字, 一般就是yaml的注释
         */
        Mark(@Nullable String comment) {
            this.rawComment = STR."#\{comment}";
            this.comment = comment == null ? "" : STR."\{lineSeparator}\{lineSeparator}\{rawComment}\{lineSeparator}";
            this.startMark = name().toLowerCase();
            this.endMarkKey = STR."\{startMark}-end";
            var generator = new HashMap<>();
            generator.put(endMarkKey, endMarkValue);
            try {
                this.endMark = YamlService.toString(generator);
            } catch (JsonProcessingException e) {
                throw new DBSystemException(e);
            }
        }

        Map<String, Object> wrap(Object content) {
            return new TwainEntryMap(startMark, content, endMarkKey, endMarkValue);
        }

        protected static class TwainEntryMap extends AbstractMap<String, Object> {
            private final TwainEntrySet twainEntrySet;

            public TwainEntryMap(String startK, Object startV, String endKey, String endValue) {
                twainEntrySet = new TwainEntrySet(startK, startV, endKey, endValue);
            }

            @NotNull
            @Override
            public Set<Entry<String, Object>> entrySet() {
                return twainEntrySet;
            }
        }

        protected static class TwainEntrySet extends AbstractSet<Map.Entry<String, Object>> {
            private final Map.Entry<String, Object> start;

            private final Map.Entry<String, Object> end;

            public TwainEntrySet(String startKey, Object startValue, String endKey, String endValue) {
                this.start = new AbstractMap.SimpleEntry<>(startKey, startValue);
                this.end = new AbstractMap.SimpleEntry<>(endKey, endValue);
            }

            @Override
            public @NotNull Iterator<Map.Entry<String, Object>> iterator() {
                return Stream.of(start, end).iterator();
            }

            @Override
            public int size() {
                return 2;
            }
        }

        public String comment() {
            return comment;
        }

        public String rawComment() {
            return rawComment;
        }

        public String startMark() {
            return startMark;
        }

        public String endMark() {
            return endMark;
        }
    }

    static class FileConfigSource extends ConfigSource {
        private final Path base;

        private String humanRead;

        FileConfigSource(@NotNull File base) {
            if (!base.exists()) {
                log.error("config file '{}' not exist", base);
                throw Exit.exception();
            }
            this.base = base.toPath();
            if (base.length() > 5 << 20) {
                log.warn("config file '{}' is too big (should less then 5KB)", base);
            }
            if (!base.canRead() || !base.canWrite()) {
                log.error("can not read/write config file '{}'", base);
                throw Exit.exception();
            }
            try (var reader = Files.newBufferedReader(this.base, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append(lineSeparator); // 保持原始文件的换行符
                }
            } catch (IOException e) {
                log.error("can not operate config file", e);
                throw Exit.exception();
            }
        }

        @Override
        protected Writer newWriter() throws IOException {
            return Files.newBufferedWriter(base, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return humanRead == null ? (humanRead = STR."FileConfigSource[path=\{base}]") : humanRead;
        }
    }

    static class URLConfigSource extends ConfigSource {
        private final URL url;

        private final Function<URL, Writer> writerProvider;

        private String humanRead;

        protected URLConfigSource(@NotNull URL url) {
            this.url = url;
            var protocol = url.getProtocol();
            if ("http".equals(protocol) || "https".equals(protocol) || "ftp".equals(protocol)) {
                writerProvider = url_ -> {
                    try {
                        var connection = url_.openConnection();
                        connection.setConnectTimeout(7_000);
                        connection.connect();
                        if (connection instanceof HttpURLConnection httpURLConnection) {
                            httpURLConnection.setRequestMethod("POST");
                        }
                        connection.setDoOutput(true);
                        return new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.error(STR."can not automatically upload config modifications. cause: \{e.getMessage()}");
                        return BufferedWriter.nullWriter();
                    }
                };
                writeBuffer(url);
                return;
            }
            if (!"file".equals(protocol)) {
                log.error("config file: unsupported protocol '{}', url: '{}'", protocol, url);
                throw Exit.exception();
            }
            var pathStr = url.getFile().substring(1);
            Path filePath;
            try {
                filePath = Path.of(pathStr);
            } catch (InvalidPathException e) {
                log.error("can not resolve path: '{}'", pathStr);
                throw Exit.exception();
            }
            writerProvider = n -> {
                try {
                    return Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("can not write config file. cause: {}", e.getCause().getMessage());
                    return BufferedWriter.nullWriter();
                }
            };
            writeBuffer(url);
        }

        private void writeBuffer(@NotNull URL url) {
            InputStream in;
            try {
                in = url.openConnection().getInputStream();
            } catch (IOException e) {
                log.error("can not read config file form '{}'", url);
                throw Exit.exception();
            }
            try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append(lineSeparator); // 保持原始文件的换行符
                }
            } catch (IOException e) {
                log.error("can not read config file. cause: {}", e.getMessage());
                throw Exit.exception();
            }
        }

        @Override
        protected Writer newWriter() {
            return writerProvider.apply(url);
        }

        @Override
        public String toString() {
            return humanRead == null ? (humanRead = STR."URLConfigSource[url=\{url}]") : humanRead;
        }
    }
}
