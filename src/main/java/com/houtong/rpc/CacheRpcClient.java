package com.houtong.rpc;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CacheRpcClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Object getFromOtherNode(String routingKey, String key) {
        Map<String, Object> data = new HashMap<>();
        data.put("operation", "get");
        data.put("key", key);
        return rabbitTemplate.convertSendAndReceive("rpc", routingKey, data);
    }
    public void setToOtherNode(String routingKey, String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put("operation", "set");
        data.put("key", key);
        data.put("value", value);

        rabbitTemplate.convertAndSend("rpc", routingKey, data);
    }

    public Integer deleteFromOtherNode(String routingKey, String key) {
        Map<String, Object> data = new HashMap<>();
        data.put("operation", "delete");
        data.put("key", key);
        return rabbitTemplate.convertSendAndReceiveAsType("rpc", routingKey, data,ParameterizedTypeReference.forType(Integer.class));
    }
}

