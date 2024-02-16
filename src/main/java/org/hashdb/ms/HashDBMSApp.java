package org.hashdb.ms;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.event.ApplicationContextLoadedEvent;
import org.hashdb.ms.net.nio.msg.v1.ActMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.protocol.CodecDispatcher;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Date: ${DATE} ${TIME}
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 因为要自定义设置所以配了, 暴露代理是为了, 可以在本类拿到代理对象,从而在本类或跨类调用代理对象
@EnableConfigurationProperties
public class HashDBMSApp implements ApplicationContextAware {
    private static ConfigurableApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = ((ConfigurableApplicationContext) applicationContext);
    }

    @Data
    public static class MYConfig {
        String file;

        public MYConfig() {
        }

        public MYConfig(String file) {
            this.file = file;
        }
    }

    public static class MyYamlConfig {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        protected String file() {
            return "MyYamlConfig";
        }
    }

    public static class MyYamlSubConfig extends MyYamlConfig {
        @JsonProperty
        private String aabb = "aabb";

        protected String file() {
            return System.getProperty("user.dir");
        }

        protected MyYamlSubConfig() {
        }

        @Override
        public String toString() {
            return "MyYamlSubConfig{" +
                    "aabb='" + aabb + '\'' +
                    '}';
        }
    }

    @NotNull
    static ByteBuf encode(Message<?> msg) {
        var body = msg.getMeta().bodyParser().encode(msg.body());
        var out = ByteBufAllocator.DEFAULT.buffer(body.writerIndex() + 1 + 30);
        // 4 magic
        out.writeBytes(CodecDispatcher.MAGIC_BYTES);
        // 1 version
        out.writeByte(Protocol.HASH_V1.key());
        // 1 deserialize method 其实这个字段可以不记录, 只需要知道消息体类型, 即type字段就知道如何解析了
        // 但是如果去掉这个byte,那么消息头的大小就变成了21,还需要padding补齐,所以干脆就这样的了
        out.writeByte(msg.getMeta().bodyParser().key());
        // 4 type
        out.writeInt(msg.getMeta().key());
        // 8 message id
        out.writeLong(msg.id());
        // 4 body length
        // 如果是应答类消息, 那么body前8个字节就为actId
        if (msg.getMeta().isActMessage()) {
            // expand body length for actId
            out.writeInt(body.writerIndex() + 1 + 8);
            // 4 act message id
            out.writeLong(((ActMessage<?>) msg).actId());
        } else {
            out.writeInt(body.writerIndex() + 1);
        }
        // [body length] body
        out.writeBytes(body);
        return out;
    }

    public static void main(String[] args) {
//        var channel = new EmbeddedChannel(
//                new LoggingHandler("head", LogLevel.INFO),
//                ClientChannelInitializer.frameDecoder(),
//                new CodecDispatcher(),
//                new LoggingHandler("tail", LogLevel.INFO)
//        );
//        var msg = new DefaultActMessage(0, "OK");
//        channel.writeOneOutbound(msg);
//        channel.writeInbound(encode(msg));
        //        Protocol.HASH_V1.codec().encode(channel, msg);
        SpringApplication.run(HashDBMSApp.class, args);
        // 发布一个事件, 让其他模块可以感知到spring容器已经准备好了, context 已经有值了
        // 在idea中,点击左边的耳机符号,可以跳转到监听这个事件的事件监听器
        context.publishEvent(new ApplicationContextLoadedEvent());
//        String userDir = System.getProperty("user.dir");
//        log.info("dir: " + userDir);
//
//        String file = userDir + "/my.yml";
//        YAMLMapper yamlMapper = new YAMLMapper();
//
//        MyYamlSubConfig myYamlConfig = new MyYamlSubConfig();
//        String stringfy = JsonService.stringfy(myYamlConfig);
//        MyYamlSubConfig parse = JsonService.parse(stringfy, MyYamlSubConfig.class);
//        myYamlConfig.setMyConfigs(List.of(new MYConfig("zxczxc"), new MYConfig("45123")));
//        myYamlConfig.setStrings(List.of("123", "456"));
//        String toSave = yamlMapper.writeValueAsString(myYamlConfig);
//        log.info("toSave: {}", toSave);
//        try (FileWriter fileWriter = new FileWriter(file)) {
//            fileWriter.append(toSave);
//        }
//        MyYamlConfig c = yamlMapper.readValue(new FileReader(file), MyYamlConfig.class);
//        log.info("MyYamlConfig: {}", c);
//        log.info("result: {} parse: {}", stringfy, parse);
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}