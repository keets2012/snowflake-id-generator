package cn.aoho.generator.config;

import cn.aoho.generator.service.IdConverter;
import cn.aoho.generator.service.IdService;
import cn.aoho.generator.service.SnowflakeId;
import cn.aoho.generator.service.impl.IdConverterImpl;
import cn.aoho.generator.service.impl.IdServiceImpl;
import cn.aoho.generator.service.impl.SnowflakeIdWorker;
import com.ecwid.consul.v1.ConsulClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Created by keets on 2017/9/9.
 */
@Configuration
public class ServiceConfig {

    @Value("${generate.worker:11}")
    private long workId;

    @Value("${generate.dateCenterId:10}")
    private long dateCenterId;

    @Value("${spring.cloud.consul.host:localhost}")
    private String host;

    @Value("${spring.cloud.consul.port:8500}")
    private int port;

    @Bean
    @Order(1)
    public ConsulClient consulClient() {
        return new ConsulClient(host, port);
    }

    @Bean
    public IdService idService() {
        return new IdServiceImpl(workId, consulClient());
    }

    @Bean
    public IdConverter idConverter() {
        return new IdConverterImpl();
    }

    @Bean
    @ConditionalOnProperty(value = "generate.origin")
    public SnowflakeId snowflakeId() {
        return new SnowflakeIdWorker(workId, dateCenterId);
    }


}
