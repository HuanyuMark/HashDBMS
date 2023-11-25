package org.hashdb.ms.compiler;

/**
 * Date: 2023/11/24 18:54
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class StreamCursor {
    private int cursor = 0;
    void next(){
        ++cursor;
    }
    void prev(){
        --cursor;
    }
    int cursor(){
        return cursor;
    }
}
