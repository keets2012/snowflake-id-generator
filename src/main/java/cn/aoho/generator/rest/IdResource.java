package cn.aoho.generator.rest;

import cn.aoho.generator.entity.AssembleID;
import cn.aoho.generator.entity.IdEntity;
import cn.aoho.generator.service.IdConverter;
import cn.aoho.generator.service.IdService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by keets on 2017/9/9.
 */

@Api(value = "/")
@Slf4j
@Path(value = "/api")
public class IdResource {

    @Autowired
    private IdService idService;

    @Autowired
    private IdConverter idConverter;


    @Path(value = "/id")
    @GET
    @ApiOperation(value = "生成ID", httpMethod = "GET",
            notes = "成功返回ID",
            response = long.class
    )
    public Response genId() {
        return Response.ok().entity(idService.genId()).build();
    }

    @Path("/id/{id:[0-9]*}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "对ID进行解析", httpMethod = "GET",
            notes = "成功返回解析后的ID（json格式）",
            response = IdEntity.class
    )
    public Response explainId(@ApiParam(value = "要解析的ID", required = true) @PathParam("id") long id) {
        log.info("id is {}", id);
        return Response.ok().entity(idConverter.convert(id)).build();
    }

    @Path("/time/{time:[0-9]*}")
    @GET
    @ApiOperation(value = "对时间戳进行解析", httpMethod = "GET",
            notes = "成功返回yyyy-MM-dd HH:mm:ss格式的日期时间",
            response = String.class
    )
    public Response transTime(@ApiParam(value = "要解析的时间戳", required = true) @PathParam("time") long time) {
        log.info("time is {}", time);
        return Response.ok().entity(DateFormatUtils.format(idService.transTime(time), "yyyy-MM-dd HH:mm:ss")).build();
    }

    @Path("/id")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "传入相应参数生成ID", httpMethod = "POST",
            notes = "成功返回ID",
            response = long.class
    )
    public Response makeId(@ApiParam(value = "传入的相应参数（json格式）", required = true) AssembleID makeID) {
        long worker = makeID.getMachine();
        long time = makeID.getTime();
        long sequence = makeID.getSeq();
        log.info("worker is {}", worker);
        log.info("time is {}", time);
        log.info("sequence is {}", sequence);

        if (time == -1 || sequence == -1) {
            throw new IllegalArgumentException("Both time and sequence are required.");
        }

        long ret = worker == -1 ? idService.makeId(time, sequence) : idService.makeId(time, worker, sequence);
        return Response.ok().entity(ret).build();
    }
}