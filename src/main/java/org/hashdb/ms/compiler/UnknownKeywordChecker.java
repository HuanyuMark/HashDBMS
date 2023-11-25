package org.hashdb.ms.compiler;

/**
 * Date: 2023/11/22 20:10
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class UnknownKeywordChecker implements Resolver {

    @Override
    public boolean continueResolve(ResolverContext ctx) {
        return false;
    }
}
