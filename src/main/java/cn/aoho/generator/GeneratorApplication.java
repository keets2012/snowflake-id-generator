package cn.aoho.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class GeneratorApplication {

    public static void main(String[] args) {
        log.info("start execute GeneratorApplication....\n");
        SpringApplication.run(GeneratorApplication.class, args);
        log.info("end execute GeneratorApplication....\n");
    }
}
