package org.hashdb.ms.net.service;

import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:37
 * 给各个从节点投票的消息, 如果该结点收到的投票数=从节点个数-1,
 * 则该结点升格为候选结点 然后给其它所有从节点发送 CandidateMessage
 * 比对谁最新票数达标, 最先的达标的结点就升格为主结点
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class VoteMasterMessage extends ServiceMessage {
    @Override
    public MessageType getType() {
        return MessageType.VOTE_MASTER;
    }
}
