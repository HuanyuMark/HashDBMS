package org.hashdb.ms.compiler;

/**
 * Date: 2023/11/22 19:05
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface Resolver {
    boolean continueResolve(ResolverContext ctx);
    default boolean canResolve(ResolverContext ctx) {
        return true;
    }
    default ResolverContext resolve(ResolverContext ctx) {
        return ctx;
    };
}
