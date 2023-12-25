package org.hashdb.ms.compiler.charactor.node.keyword.supplier;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

import java.util.function.Supplier;

/**
 * Date: 2023/12/8 16:48
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class GetNode extends SupplierKeywordNode {
    public GetNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    public void compile() {
        doCompile();
    }

    protected void doCompile() {
        while (true) {
            char token;
            try {
                token = stream.nextToken();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }


        }
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.GET;
    }

    @Override
    protected Supplier<Object> doSupply() {
        return null;
    }
}
