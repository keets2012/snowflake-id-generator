package cn.aoho.generator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Created by keets on 2017/6/28.
 */
@ApiModel(value = "生成ID所需的参数")
@Data
public class AssembleID {
    @Max(1023)
    @Min(0)
    @ApiModelProperty(value = "机器ID")
    @JsonProperty("worker")
    private long machine = -1;

    @ApiModelProperty(value = "时间戳", required = true)
    @JsonProperty("timeStamp")
    private long time = -1;

    @Max(4095)
    @Min(0)
    @ApiModelProperty(value = "序列号", required = true)
    @JsonProperty("sequence")
    private long seq = -1;
}
