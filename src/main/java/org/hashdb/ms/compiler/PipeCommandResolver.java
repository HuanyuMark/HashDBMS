package org.hashdb.ms.compiler;

/**
 * Date: 2023/11/22 20:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class PipeCommandResolver implements Resolver{
    @Override
    public boolean continueResolve(ResolverContext ctx) {
        return true;
    }

    @Override
    public ResolverContext resolve(ResolverContext ctx) {

        return Resolver.super.resolve(ctx);
    }
}
