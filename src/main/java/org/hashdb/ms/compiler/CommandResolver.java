package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.checker.IllegalSpELChecker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2023/11/22 0:45
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
public class CommandResolver {
    private final List<Resolver> preresolvers = new ArrayList<>();

    {

    }

    public ResolverContext resolve(String command) {
        ResolverContext context = new ResolverContext(command,List.of());
        for (Resolver preresolver : preresolvers) {
            if(!preresolver.canResolve(context)) {
                context = preresolver.resolve(context);
            }
            if (!preresolver.continueResolve(context)) {
                return context;
            }
        }
        return context;
    }
}
