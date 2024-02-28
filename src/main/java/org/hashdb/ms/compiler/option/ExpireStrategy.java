package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.exception.IllegalValueException;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;

/**
 * Date: 2023/11/28 22:06
 * NX -- Set expiry only when the key has no expiry
 * XX -- Set expiry only when the key has an existing expiry
 * GT -- Set expiry only when the new expiry is greater than current one
 * LT -- Set expiry only when the new expiry is less than current one
 *
 * @author Huanyu Mark
 */
public enum ExpireStrategy {
    DEFAULT((db, value, expireTime, priority) -> {
        value.clearBy(db, expireTime, priority);
    }),
    DEF((db, value, expireTime, priority) -> {
        value.clearBy(db, expireTime, priority);
    }),
    NX((db, value, expireTime, priority) -> {
        if (value.getExpireMilliseconds() != null) {
            return;
        }
        value.clearBy(db, expireTime, priority);
    }),
    XX((db, value, expireTime, priority) -> {
        if (value.getExpireMilliseconds() == null) {
            return;
        }
        value.clearBy(db, expireTime, priority);
    }),
    GT((db, value, expireTime, priority) -> {
        if (expireTime == null) {
            throw new IllegalValueException("option value of 'EXPIRE_STRATEGY' should not be null");
        }
        if (value.getExpireMilliseconds() == null || expireTime > value.getExpireMilliseconds()) {
            return;
        }
        value.clearBy(db, expireTime, priority);
    }),
    LT((db, value, expireTime, priority) -> {
        if (expireTime == null) {
            throw new IllegalValueException("option value of 'EXPIRE_STRATEGY' should not be null");
        }
        if (value.getExpireMilliseconds() == null || expireTime < value.getExpireMilliseconds()) {
            return;
        }
        value.clearBy(db, expireTime, priority);
    });

    public interface ExpireStrategyExecutor {
        void exec(Database db, HValue<?> value, Long expireTime, OpsTaskPriority priority);
    }

    private final ExpireStrategyExecutor strategyExecutor;

    ExpireStrategy(ExpireStrategyExecutor strategyExecutor) {
        this.strategyExecutor = strategyExecutor;
    }

    public void exec(Database db, HValue<?> value, Long expireTime, OpsTaskPriority priority) {
        this.strategyExecutor.exec(db, value, expireTime, priority);
    }
}
