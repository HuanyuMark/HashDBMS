package org.hashdb.ms.net.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;

import java.util.UUID;

/**
 * Date: 2023/12/1 3:08
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@EqualsAndHashCode(callSuper = true)
public class AuthenticationMessage extends Message {

    // 下面的属性可以用来验证链接合法性
//    private String username;
//    private String password;
//    @Override
    public MessageType getType() {
        return MessageType.AUTH;
    }
}
