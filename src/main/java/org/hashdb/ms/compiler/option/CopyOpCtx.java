package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/27 22:53
 * 是否拷贝原值, 如果拷贝原值, 则对新值的修改不会影响到旧值
 * 有两种典型的使用案例:
 * 1. 对于一个字段, 可能需要在多个地方使用, 但是在使用时, 可能需要对其进行修改, 但是不希望对其他地方的使用造成影响
 * 2. 如果这条命令的返回值是 Immutable , 则可以将其转为新的 mutable 对象, 然后对这个 mutable 对象进行修改
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CopyOpCtx extends BooleanOpCtx {
    public CopyOpCtx(Boolean defaultValue) {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return Options.COPY;
    }
}
