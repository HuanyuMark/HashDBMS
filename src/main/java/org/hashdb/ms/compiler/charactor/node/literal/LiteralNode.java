package org.hashdb.ms.compiler.charactor.node.literal;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.NodeType;
import org.hashdb.ms.compiler.charactor.node.CompileNode;

import java.util.function.Supplier;

/**
 * Date: 2023/12/10 18:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class LiteralNode<T> extends CompileNode implements Supplier<T> {
    public LiteralNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    public NodeType nodeType() {
        return NodeType.LITERAL;
    }
}
