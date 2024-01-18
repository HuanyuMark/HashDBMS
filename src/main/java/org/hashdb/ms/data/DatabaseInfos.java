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
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class DatabaseInfos implements Serializable {
    public static final DatabaseInfos NULL = new DatabaseInfos(-1, "NULL", new Date(0));
    @Serial
    private static final long serialVersionUID = 632895L;
    protected final int id;

    protected final String name;
    protected final Date createTime;
    @Setter
    protected Date lastSaveTime = new Date();

    @Override
    public String toString() {
        return JsonService.toString(this);
    }

    public String getName() {
        return name;
    }
}
