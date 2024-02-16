package org.hashdb.ms.compiler.keyword;

/**
 * Date: 2023/11/25 2:34
 *
 * @author huanyuMake-pecdle
 */
public interface OpsCandidate {
    /**
     * @param keyword 二级关键字
     * @param value   一级关键子子句执行完毕后返回的值
     */
    boolean support(String keyword, Object value);
}
