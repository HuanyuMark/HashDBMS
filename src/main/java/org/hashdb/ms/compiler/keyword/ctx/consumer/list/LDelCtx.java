package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;

import java.util.*;

/**
 * Date: 2023/11/29 11:11
 *
 * @author Huanyu Mark
 */
public class LDelCtx extends RandomAccessCtx {

    public LDelCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LDEL;
    }

    @Override
    protected List<Object> access(List<Object> opeList, List<Long> indexes) {
        if (opeList instanceof RandomAccess) {
            return indexes.stream().map(i -> opeList.remove(i.intValue())).toList();
        }

        var result = new LinkedList<>();
        int elIndex;
        // 用尾遍历
        if (opeList.size() - indexes.getFirst() < indexes.getLast()) {
            elIndex = opeList.size() - 1;
            var elIter = opeList.reversed().iterator();
            var indexIter = indexes.reversed().iterator();
            var matchIndex = indexIter.next();
            while (elIter.hasNext()) {
                var el = elIter.next();
                if (elIndex == matchIndex) {
                    result.add(el);
                    elIter.remove();
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
                elIter.remove();
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
