package cn.aoho.generator.service;


import java.util.Date;

/**
 * Created by keets on 2017/9/9.
 */
public interface IdService {
    long genId();

    Date transTime(long time);

    long makeId(long time, long seq);

    long makeId(long time, long seq, long machine);
}
