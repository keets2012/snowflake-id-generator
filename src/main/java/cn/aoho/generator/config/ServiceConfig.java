package cn.aoho.generator.config;

import cn.aoho.generator.service.IdConverter;
import cn.aoho.generator.service.IdService;
import cn.aoho.generator.service.SnowflakeId;
import cn.aoho.generator.service.impl.IdConverterImpl;
import cn.aoho.generator.service.impl.IdServiceImpl;
import cn.aoho.generator.service.impl.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by keets on 2017/9/9.
 */
@Configuration
public class ServiceConfig {

    @Value("${generate.worker:1000}")
    private static long workId;

    @Value("${generate.dateCenterId:500}")
    private static long dateCenterId;

    @Bean
    public IdService idService() {
        return new IdServiceImpl(workId);
    }

    @Bean
    public IdConverter idConverter() {
        return new IdConverterImpl();
    }

    @Bean
    public SnowflakeId snowflakeId() {
        return new SnowflakeIdWorker(workId, dateCenterId);
    }

}
