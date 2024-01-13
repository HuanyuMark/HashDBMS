package org.hashdb.ms.data.task;

import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.data.result.PopPushResult;
import org.hashdb.ms.exception.DBClientException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Date: 2023/11/23 23:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ListOpsTaskSupplier extends RefDataTypeOpsTaskSupplier {
    @Contract(pure = true)
    public static @NotNull OpsTask<List<?>> lPush(List<?> container, List<?> addList) {
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(() -> {
            for (Object o : addList) {
                container_.addFirst(o);
            }
            return container_;
        });
    }

    @Contract(pure = true)
    public static @NotNull OpsTask<List<?>> rPush(List<?> container, List<?> addList) {
        UnmodifiableCollections.check(container);
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(() -> {
//            container_.stream()
            container_.addAll(addList);
            return container_;
        });
    }

    @Contract(pure = true)
    public static @NotNull OpsTask<?> lPop(List<?> container) {
        UnmodifiableCollections.check(container);
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(container_::removeFirst);
    }

    @Contract(pure = true)
    public static @NotNull OpsTask<?> rPop(List<?> container) {
        UnmodifiableCollections.check(container);
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(container_::removeLast);
    }

    /**
     * 倒叙后,不影响原数组
     *
     * @param list 原始链表
     * @return 不可变的 倒序链表视图
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull OpsTask<List<?>> reversedView(List<?> list) {
        return OpsTask.of(() -> Collections.unmodifiableList(list.reversed()));
    }

    public static @NotNull OpsTask<List<?>> reverseSelf(List<?> list) {
        UnmodifiableCollections.check(list);
        return OpsTask.of(() -> {
            Collections.reverse(list);
            return list;
        });
    }

    public static @NotNull OpsTask<List<?>> range(List<?> list, int start, int end) {
        int normalizedStart = start >= 0 ? start : list.size() + start;
        int normalizedEnd = end >= 0 ? end : list.size() + end;
        if (normalizedStart < 0 || normalizedEnd < 0 || normalizedStart > normalizedEnd) {
            throw new IndexOutOfBoundsException();
        }
        int listSize = normalizedEnd - normalizedStart;
        return OpsTask.of(() -> list.stream().skip(start).limit(listSize).toList());
    }

    /**
     * @param list  容器
     * @param count 左右同时弹出的个数 比如, count 为1, 就左右同时弹出1个,共计弹出两个
     * @return 若最后链表剩一个元素, 则返回[NULL,NULL], 否则返回 [L1,L2…R2,R1] (数字表示被弹出的次序)
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull OpsTask<List<?>> trim(List<?> list, int count) {
        UnmodifiableCollections.check(list);
        return OpsTask.of(() -> {
            List<Object> result = new LinkedList<>();
            for (int i = 0; i < count; i++) {
                if (list.size() < 2) {
                    break;
                }
                result.addFirst(list.removeFirst());
                result.addLast(list.removeLast());
            }
            return result;
        });
    }

    public static OpsTask<PopPushResult> rPopLPush(List<?> container, int popCount, List<?> pushList) {
        UnmodifiableCollections.check(container);
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(() -> {
            List<Object> popList = new LinkedList<>();
            for (int i = 0; i < popCount; i++) {
                popList.add(container_.removeLast());
            }
            container_.addAll(pushList);
            return new PopPushResult(container_.size(), popList);
        });
    }

    public static OpsTask<PopPushResult> lPopRPush(List<?> container, int popCount, List<?> pushList) {
        UnmodifiableCollections.check(container);
        @SuppressWarnings("unchecked")
        List<Object> container_ = (List<Object>) container;
        return OpsTask.of(() -> {
            List<Object> popList = new LinkedList<>();
            for (int i = 0; i < popCount; i++) {
                popList.add(container_.removeFirst());
            }
            container_.addAll(0, pushList);
            return new PopPushResult(container_.size(), popList);
        });
    }

    public static @NotNull OpsTask<List<?>> set(List<?> list, @NotNull List<PlainPair<Integer, Object>> values) {
        UnmodifiableCollections.check(list);
        @SuppressWarnings("unchecked")
        var list_ = (List<Object>) list;
        var values_ = values.stream().map(e -> e.key() < 0 ? new PlainPair<>(list_.size() + e.key(), e.value()) : e).sorted(Comparator.comparingInt(PlainPair::key)).toList();
        int largestIndex = values_.getLast().key();
        int smallestIndex = values_.getFirst().key();
        if (smallestIndex < 0 || largestIndex >= list_.size()) {
            throw new DBClientException(new IndexOutOfBoundsException());
        }
        return OpsTask.of(() -> {
            if (values_.isEmpty()) {
                return List.of();
            }
            List<Object> result = new ArrayList<>();
            int elIndex = 0;
            var elIter = list_.listIterator();
            var indexIter = values_.listIterator();
            while (elIter.hasNext() && indexIter.hasNext()) {
                Object el = elIter.next();
                PlainPair<Integer, ?> entry = indexIter.next();
                if (elIndex == entry.key()) {
                    result.add(el);
                    elIter.set(entry.value());
                }
                ++elIndex;
            }
            return Collections.unmodifiableList(result);
        });
    }

    public static @NotNull OpsTask<List<?>> get(@NotNull List<?> list, List<Integer> indexes) {
        List<Integer> indexes_ = sortIndexes(list.size(), indexes);
        return OpsTask.of(() -> {
            var result = new LinkedList<>();
            int elIndex = 0;
            var elIter = list.iterator();
            var indexIter = indexes_.iterator();
            while (elIter.hasNext() && indexIter.hasNext()) {
                var el = elIter.next();
                if (elIndex == indexIter.next()) {
                    result.add(el);
                }
                ++elIndex;
            }
            return Collections.unmodifiableList(result);
        });
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull OpsTask<List<?>> del(List<?> list, List<Integer> indexes) {
        UnmodifiableCollections.check(list);
        var indexes_ = sortIndexes(list.size(), indexes);
        return OpsTask.of(() -> {
            var result = new LinkedList<>();
            int elIndex = 0;
            var elIter = list.iterator();
            var indexIter = indexes_.iterator();
            while (elIter.hasNext() && indexIter.hasNext()) {
                var el = elIter.next();
                if (elIndex == indexIter.next()) {
                    result.add(el);
                    elIter.remove();
                } else {
                    ++elIndex;
                }
            }
            return Collections.unmodifiableList(result);
        });
    }


    public static @NotNull List<Integer> sortIndexes(int bound, @NotNull List<Integer> indexes) {
        var result = indexes.stream().map(i -> i < 0 ? bound + i : i).sorted().toList();
        int largestIndex = result.getLast();
        int smallestIndex = result.getFirst();
        if (smallestIndex < 0 || largestIndex >= bound) {
            throw new DBClientException(new IndexOutOfBoundsException());
        }
        return result;
    }
}
