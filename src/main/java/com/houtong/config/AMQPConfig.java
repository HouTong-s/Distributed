package com.houtong.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class AMQPConfig {

    @Bean
    public Queue node1Queue() {
        return new Queue("node1");
    }

    @Bean
    public Queue node2Queue() {
        return new Queue("node2");
    }

    @Bean
    public Queue replyQueue(){
        return new Queue("rpc.replies");
    }
    @Bean
    public Queue node3Queue() {
        return new Queue("node3");
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("rpc");
    }

    @Bean
    public Binding bindingNode1(DirectExchange exchange) {
        return BindingBuilder.bind(node1Queue()).to(exchange).with("node1");
    }

    @Bean
    public Binding bindingNode2(DirectExchange exchange) {
        return BindingBuilder.bind(node2Queue()).to(exchange).with("node2");
    }

    @Bean
    public Binding bindingNode3(DirectExchange exchange) {
        return BindingBuilder.bind(node3Queue()).to(exchange).with("node3");
    }
}
