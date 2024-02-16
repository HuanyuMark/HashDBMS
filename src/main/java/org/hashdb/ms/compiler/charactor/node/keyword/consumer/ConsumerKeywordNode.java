package org.hashdb.ms.compiler.charactor.node.keyword.consumer;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.node.KeywordNode;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Date: 2023/12/10 20:38
 *
 * @author huanyuMake-pecdle
 */
public abstract class ConsumerKeywordNode extends KeywordNode {

    private ConsumerKeywordNode nextConsumerNode;

    protected ConsumerKeywordNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Contract(pure = true)
    final public @NotNull Function<Object, Object> consumer() {
        return v -> {
            var result = doConsume().apply(v);
            return nextConsumerNode == null ? result : nextConsumerNode.consumer().apply(result);
        };
    }

    @Override
    abstract public ConsumerKeyword name();

    abstract protected Function<Object, Object> doConsume();
}
