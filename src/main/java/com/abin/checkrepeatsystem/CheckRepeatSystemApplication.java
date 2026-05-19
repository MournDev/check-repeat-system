package com.abin.checkrepeatsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CheckRepeatSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckRepeatSystemApplication.class, args);
    }

}
