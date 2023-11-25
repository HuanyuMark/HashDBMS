package org.hashdb.ms.compiler;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.data.Database;

import java.util.List;

/**
 * Date: 2023/11/22 19:10
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@RequiredArgsConstructor
public class ResolverContext {
    private final String command;
    private final List<Resolver> resolverChain;

//    private
    private Database database;
    private Object data;
//    private int cursor = 0;
//    private Resolver currentResolver = resolverChain.isEmpty() ? null : resolverChain.getFirst();
    public void next(){

    }
    public Object getData() {
        return data;
    }

    public String getCommand() {
        return command;
    }
}
