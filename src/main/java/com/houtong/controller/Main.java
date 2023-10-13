package com.houtong.controller;

import com.houtong.config.NodeConfig;
import com.houtong.repository.MemoryDatabase;
import com.houtong.rpc.CacheRpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/")
public class Main {

    @Autowired
    private MemoryDatabase memoryDatabase;

    @Autowired
    private CacheRpcClient rpcClient;

    private AtomicInteger counter = new AtomicInteger(0);

    @Autowired
    private NodeConfig nodeConfig;

    @GetMapping("/{key}")
    public ResponseEntity<Object> getKey(@PathVariable("key") String key) {
        Object value = memoryDatabase.get(key);

        if (value == null) {
            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    value = rpcClient.getFromOtherNode(node, key);
                    if (value != null){
                        break;
                    }
                }
            }
        }

        if (value == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(value, HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<String> setKeyValue(@RequestBody Map<String, Object> data) {
        Map.Entry<String, Object> entry;
        try {
            entry = data.entrySet().iterator().next();
        } catch (NoSuchElementException | NullPointerException e) {
            return new ResponseEntity<>("Error: Invalid data provided", HttpStatus.BAD_REQUEST);
        }
        String key = entry.getKey();
        Object value = entry.getValue();

        if (memoryDatabase.containsKey(key)) {
            memoryDatabase.put(key, value);  // 更新当前节点的值
        } else {
            boolean keyExistsInOtherNode = false;
            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    // 检查其他节点
                    Object valueInOtherNode = rpcClient.getFromOtherNode(node, key);
                    if (valueInOtherNode != null) {
                        rpcClient.setToOtherNode(node , key, value);
                        keyExistsInOtherNode = true;
                        break;
                    }
                }
            }
            // 如果所有节点都没有这个key，根据策略存储它
            if (!keyExistsInOtherNode) {
                String nextNode = nodeConfig.getNodes().get((counter.incrementAndGet() % nodeConfig.getNodes().size()));
                if (nodeConfig.getCurrentNode().equals(nextNode)) {
                    memoryDatabase.put(key, value);  // 存储在当前节点
                } else {
                    rpcClient.setToOtherNode(nextNode , key, value);
                }
            }
        }

        return ResponseEntity.ok("Processed");
    }


    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteKey(@PathVariable("key") String key) {
        if (memoryDatabase.containsKey(key)) {
            memoryDatabase.remove(key);  // 删除当前节点的值
            return new ResponseEntity<>("1", HttpStatus.OK);
        } else {
            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    Integer delete_num = rpcClient.deleteFromOtherNode(node, key);
                    if(delete_num == 1)
                    {
                        return new ResponseEntity<>("1", HttpStatus.OK);
                    }
                }
            }
        }
        return new ResponseEntity<>("0", HttpStatus.OK);
    }



}
