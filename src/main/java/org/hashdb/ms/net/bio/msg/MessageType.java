package org.hashdb.ms.net.bio.msg;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.hashdb.ms.net.bio.client.ActHeartbeatMessage;
import org.hashdb.ms.net.bio.client.AuthenticationMessage;
import org.hashdb.ms.net.bio.client.CloseMessage;
import org.hashdb.ms.net.bio.client.CommandMessage;
import org.hashdb.ms.net.bio.service.*;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.ReflectCache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2023/12/1 2:10
 *
 * @author huanyuMake-pecdle
 */
@Deprecated
public enum MessageType {
    HEARTBEAT(HeartbeatMessage.class),
    ACT_HEARTBEAT(ActHeartbeatMessage.class),
    AUTH(AuthenticationMessage.class),
    ACT_AUTH(ActAuthenticationMessage.class),
    DATA_CHUNK(DataChunkMessage.class),
    CLOSE(CloseMessage.class),
    ERROR(ErrorMessage.class),
    COMMAND(CommandMessage.class),
    ACK_COMMAND(ActCommandMessage.class),
    REPLICATION(ReplicationMessage.class),
    REPL_HEARTBEAT(ReplicationHeartbeatMessage.class),
    CONNECT_MASTER(ConnectMasterMessage.class),
    ACT_CONNECT_MASTER(ActConnectMasterMessage.class),
    SYNC(SyncMessage.class),
    VOTE_MASTER(VoteMasterMessage.class),
    CANDIDATE(CandidateMessage.class);

    private final MessageTypeDeserializer deserializer;

    private ReflectCache<? extends Message> messageClass;

    private static volatile Map<Class<? extends Message>, MessageType> messageTypeMap;

    MessageType(Class<? extends Message> messageClass) {
        registerMessageClass(messageClass, this);
        deserializer = null;
    }

    MessageType(Class<? extends Message> messageClass, MessageTypeDeserializer deserializer) {
        registerMessageClass(messageClass, this);
        this.deserializer = deserializer;
    }

    private static void registerMessageClass(Class<? extends Message> messageClass, MessageType type) {
        if (messageTypeMap == null) {
            messageTypeMap = new HashMap<>();
        }
        messageTypeMap.put(messageClass, type);
        type.messageClass = new ReflectCache<>(messageClass);
    }

    public Message deserialize(JsonParser jp, JsonNode rootNode, DeserializationContext context) throws IOException {
        return JsonService.parse(jp, messageClass.clazz());
    }

    public interface MessageTypeDeserializer {
        Message deserialize(JsonParser jp, JsonNode rootNode, DeserializationContext context) throws IOException;
    }

    public ReflectCache<? extends Message> getReflectCache() {
        return messageClass;
    }

    public static MessageType typeOf(Class<? extends Message> messageClass) {
        return messageTypeMap.get(messageClass);
    }
}
