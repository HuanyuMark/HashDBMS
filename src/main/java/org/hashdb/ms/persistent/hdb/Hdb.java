package org.hashdb.ms.persistent.hdb;

/**
 * Date: 2024/2/27 16:59
 * todo: 非常困难的一个点: 集合类型的数据的完整一致性(在同一时刻看到的数据无论以后如何更改, 都不会影响这一时刻的数据)
 * 思路: 1. 在编译层, 识别出写命令后,分析出会被更改的所有地方, 形成一个修改链
 *      2. 在执行层, 执行到写命令, 且检测到有HDB文件正在生成, 则根据这个修改链,
 *
 * @author Huanyu Mark
 */
public class Hdb extends AbstractHdb {
}
