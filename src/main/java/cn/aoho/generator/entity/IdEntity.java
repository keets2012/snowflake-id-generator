package cn.aoho.generator.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by keets on 2017/9/9.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdEntity implements Serializable {
    private long timeStamp;
    private long worker;
    private long sequence;
}
