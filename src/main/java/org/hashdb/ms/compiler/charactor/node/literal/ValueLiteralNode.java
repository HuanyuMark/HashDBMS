package org.hashdb.ms.compiler.charactor.node.literal;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;

/**
 * Date: 2023/12/10 19:04
 *
 * @author Huanyu Mark
 */
public class ValueLiteralNode extends LiteralNode<Object> {
    public ValueLiteralNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public void compile() {

    }
}
