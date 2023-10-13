package com.houtong.controller;

import com.houtong.config.NodeConfig;
import com.houtong.repository.MemoryDatabase;
import com.houtong.rpc.CacheRpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
            List<CompletableFuture<Object>> futures = new ArrayList<>();

            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> rpcClient.getFromOtherNode(node, key));
                    futures.add(future);
                }
            }

            // 等待所有异步任务完成，并查找非null的值
            value = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
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
            List<CompletableFuture<Object>> futures = new ArrayList<>();

            // 使用CompletableFuture来异步获取值
            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> rpcClient.getFromOtherNode(node, key));
                    futures.add(future);
                }
            }

            // 等待所有的异步任务完成
            List<Object> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            for (int i = 0; i < results.size(); i++) {
                if (results.get(i) != null) {
                    String node = nodeConfig.getNodes().get(i);
                    rpcClient.setToOtherNode(node, key, value);
                    keyExistsInOtherNode = true;
                    break;
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
            memoryDatabase.remove(key);
            return new ResponseEntity<>("1", HttpStatus.OK);
        } else {
            AtomicReference<ResponseEntity<String>> response = new AtomicReference<>(new ResponseEntity<>("0", HttpStatus.OK));
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger completedTasks = new AtomicInteger(0);  // 这个计数器用于跟踪已完成的任务数量
            int totalTasks = (int) nodeConfig.getNodes().stream().filter(node -> !node.equals(nodeConfig.getCurrentNode())).count();

            for (String node : nodeConfig.getNodes()) {
                if (!node.equals(nodeConfig.getCurrentNode())) {
                    CompletableFuture.supplyAsync(() -> (Integer) rpcClient.deleteFromOtherNode(node, key))
                            .thenAccept(result -> {
                                if(result == 1 && response.get().equals(new ResponseEntity<>("0", HttpStatus.OK))) {
                                    //有一个其他节点成功删除了key
                                    response.set(new ResponseEntity<>("1", HttpStatus.OK));
                                    latch.countDown();
                                } else if (completedTasks.incrementAndGet() == totalTasks) {  // 所有的任务都完成了
                                    latch.countDown();
                                }
                            });
                }
            }

            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return response.get();
        }
    }



}
