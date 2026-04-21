package com.abin.checkrepeatsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CheckRepeatSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckRepeatSystemApplication.class, args);
    }

}
