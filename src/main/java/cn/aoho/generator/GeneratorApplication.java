package cn.aoho.generator;

import cn.keets.swagger.EnableSwagger2Doc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

@SpringBootApplication
@Slf4j
@EnableFeignClients
@EnableDiscoveryClient
@EnableSwagger2Doc
public class GeneratorApplication {

    public static void main(String[] args) {
        log.info("start execute GeneratorApplication....\n");
        SpringApplication.run(GeneratorApplication.class, args);
        log.info("end execute GeneratorApplication....\n");
    }
}
