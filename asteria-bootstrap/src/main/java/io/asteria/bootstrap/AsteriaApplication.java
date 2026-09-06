package io.asteria.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@EnableScheduling
@MapperScan(value = "io.asteria", annotationClass = Mapper.class)
@SpringBootApplication(scanBasePackages = "io.asteria")
public class AsteriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsteriaApplication.class, args);
    }
}
