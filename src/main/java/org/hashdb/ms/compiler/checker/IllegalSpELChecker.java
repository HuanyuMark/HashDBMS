package org.hashdb.ms.compiler.checker;

import org.hashdb.ms.exception.IllegalExpressionException;
import org.hashdb.ms.compiler.Resolver;
import org.hashdb.ms.compiler.ResolverContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 2023/11/22 19:14
 * 防止 SpEL 表达式注入
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class IllegalSpELChecker implements Resolver {
    private final Pattern illegalPattern = Pattern.compile(".*T(.*).*");
    @Override
    public boolean continueResolve(ResolverContext ctx) {
        String command = ctx.getCommand();
        Matcher matcher = illegalPattern.matcher(command);
        if (!matcher.find()) {
            return true;
        }
        int foundIndex = matcher.start();
        String fragment = command.substring(foundIndex);
        throw new IllegalExpressionException(
                "found illegal SpEL near [index: "+foundIndex+"] : '"+ fragment+"'"
        );
    }
}
