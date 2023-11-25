package org.hashdb.ms.compiler.keyword;

/**
 * Date: 2023/11/25 2:33
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum ListKeyword implements OpsCandidate {
    ;

    @Override
    public boolean support(String keyword, Object value) {
        return false;
    }
}
