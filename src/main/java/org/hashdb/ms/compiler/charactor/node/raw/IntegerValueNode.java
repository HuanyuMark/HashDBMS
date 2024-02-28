package org.hashdb.ms.compiler.charactor.node.raw;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.NodeType;
import org.hashdb.ms.compiler.charactor.node.CompileNode;

/**
 * Date: 2023/12/10 18:54
 *
 * @author Huanyu Mark
 */
public class IntegerValueNode extends CompileNode {
    public IntegerValueNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    public void compile() throws ArrayIndexOutOfBoundsException {

    }

    @Override
    public NodeType nodeType() {
        return null;
    }
}
