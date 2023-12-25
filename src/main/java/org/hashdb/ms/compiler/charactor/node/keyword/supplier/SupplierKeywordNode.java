package org.hashdb.ms.compiler.charactor.node.keyword.supplier;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.node.KeywordNode;
import org.hashdb.ms.compiler.charactor.node.keyword.consumer.ConsumerKeywordNode;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

import java.util.function.Supplier;

/**
 * Date: 2023/12/8 16:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SupplierKeywordNode extends KeywordNode {

    protected ConsumerKeywordNode consumer;

    public SupplierKeywordNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    abstract public SupplierKeyword name();

    protected abstract Supplier<Object> doSupply();

    final public Supplier<Object> supplier() {
        return () -> {
            var result = doSupply().get();
            return consumer == null ? result : consumer.consumer().apply(result);
        };
    }
}
