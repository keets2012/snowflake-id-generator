package cn.aoho.generator.config;

import cn.aoho.generator.rest.IdResource;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by keets on 2017/9/9.
 */
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(IdResource.class);
        // External
    }

    @PostConstruct
    public void init() {
        this.register(ApiListingResource.class, SwaggerSerializers.class);
    }
}
