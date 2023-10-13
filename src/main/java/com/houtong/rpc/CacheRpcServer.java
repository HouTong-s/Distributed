package com.houtong.rpc;

import com.houtong.repository.MemoryDatabase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.Map;

@Service
public class CacheRpcServer {

    @Autowired
    private MemoryDatabase memoryDatabase;

    @Value("${queue.name}")
    public String queueName;

    @RabbitListener(queues = "${queue.name}")
    public Object processForNode(Map<String, Object> data) {
        String operation = (String) data.get("operation");
        String key = (String) data.get("key");

        switch (operation) {
            case "get":
                if (key == null) {
                    return null;
                }
                return memoryDatabase.get(key);

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
                    return 1; // Indicating the key was removed
                }
                else
                {
                    return 0;
                }
            default:
                return "ERROR"; // Indicating there was an error processing the request
        }
    }
}

