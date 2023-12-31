package com.houtong.rpc;

import com.houtong.repository.MemoryDatabase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;


import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Service
public class CacheRpcServer {

    @Autowired
    private MemoryDatabase memoryDatabase;

    @Value("${queue.name}")
    public String queueName;

    @RabbitListener(queues = "${queue.name}")
    @SendTo("rpc.replies")
    public Object processForNode(Map<String, Object> data) {
        String operation = (String) data.get("operation");
        String key = (String) data.get("key");

        switch (operation) {
            case "get":
                if (key == null) {
                    return new NotFoundMarker();
                }
                Object result = memoryDatabase.get(key);
                if (result == null) {
                    return new NotFoundMarker();
                }
                return result;

            case "set":
                try {
                    memoryDatabase.put(key, data.get("value"));
                    return "OK";
                } catch (NullPointerException e) {
                    return "Error: Null key or value";
                } catch (Exception e) { // Catching other general exceptions for robustness
                    return "Error: Unexpected error occurred";
                }

            case "delete":
                Object obj = memoryDatabase.remove(key);
                if(obj != null)
                {
                    return Integer.valueOf(1); // Indicating the key was removed
                }
                else
                {
                    return Integer.valueOf(0);
                }
            default:
                return "ERROR"; // Indicating there was an error processing the request
        }
    }

    // 这个类不需要包含任何方法，它只是标识一个key不存在对应的value。


}

