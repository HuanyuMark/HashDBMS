package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/26 20:29
 * 高优先级删除 配置
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class HExpireOpCtx extends LongOpCtx{
    public static final Long DEFAULT_VALUE = ExpireOpCtx.DEFAULT_EXPIRE_AFTER_MILLISECONDS;
    public HExpireOpCtx() {
        super(DEFAULT_VALUE);
    }
    @Override
    public Options key() {
        return Options.HEXPIRE;
    }
}
