package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Date: 2023/11/29 11:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LGetGtx extends RandomAccessCtx {
    public LGetGtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LGET;
    }

    @Override
    protected List<Object> access(List<Object> opeList, List<Long> indexes) {
        var result = new LinkedList<>();
        int elIndex;
        // 用尾遍历
        if(opeList.size() - indexes.getFirst() < indexes.getLast()) {
            elIndex = opeList.size() - 1;
            var elIter = opeList.reversed().iterator();
            var indexIter = indexes.reversed().iterator();
            var matchIndex = indexIter.next();
            while (elIter.hasNext()) {
                var el = elIter.next();
                if (elIndex == matchIndex) {
                    result.add(el);
                    try {
                        matchIndex = indexIter.next();
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
                --elIndex;
            }
            return Collections.unmodifiableList(result);
        }
        // 用头遍历
        elIndex = 0;
        var elIter = opeList.iterator();
        var indexIter = indexes.iterator();
        var matchIndex = indexIter.next();
        while (elIter.hasNext()) {
            var el = elIter.next();
            if (elIndex == matchIndex) {
                result.add(el);
                try {
                    matchIndex = indexIter.next();
                } catch (NoSuchElementException e) {
                    break;
                }
            }
            ++elIndex;
        }
        return Collections.unmodifiableList(result);
    }

}
