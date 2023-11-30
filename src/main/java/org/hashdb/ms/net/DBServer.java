package org.hashdb.ms.net;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.Compiler;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.sys.DBSystem;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Date: 2023/12/1 1:26
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
public class DBServer {
    @Resource
    private DBSystem system;

    @Resource
    private DBServerConfig serverConfig;

    @EventListener(SpringReadyEvent.class)
    public void ready(){
        log.info("Server is ready");
        Scanner scanner = new Scanner(System.in);
        ConnectionSession session = new ConnectionSession();
        Compiler compiler = Compiler.create(session);
        while (true) {
            try {
                String command = scanner.nextLine();
                if(command.equals("exit")){
                    System.exit(0);
                }
                String result = compiler.run(command);
                System.out.println(result);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
}
