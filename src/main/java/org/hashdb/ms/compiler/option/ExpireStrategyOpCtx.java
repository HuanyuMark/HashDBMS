package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/28 22:05
 *
 * @author huanyuMake-pecdle
 */
public class ExpireStrategyOpCtx extends EnumOpCtx<ExpireStrategy> {

    public ExpireStrategyOpCtx() {
        super(ExpireStrategy.DEFAULT);
    }

    @Override
    protected Class<? extends Enum<ExpireStrategy>> getEnumClass() {
        return ExpireStrategy.class;
    }

    @Override
    public Options key() {
        return Options.EXPIRE_STRATEGY;
    }
}
