package org.hashdb.ms.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hashdb.ms.util.JsonService;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * Date: 2023/11/21 12:53
 *
 * @author Huanyu Mark
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class DatabaseInfos implements Serializable {
    public static final DatabaseInfos NULL = new DatabaseInfos(-1, "NULL", new Date(0));
    @Serial
    private static final long serialVersionUID = 632851392687995L;
    protected final int id;

    @Getter
    protected final String name;
    protected final Date createTime;
    @Setter
    protected Date lastSaveTime = new Date();

    @Override
    public String toString() {
        return JsonService.toString(this);
    }

}
