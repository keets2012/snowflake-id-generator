package cn.aoho.generator.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdEntity implements Serializable {
    private long timeStamp;
    private long worker;
    private long sequence;
}
