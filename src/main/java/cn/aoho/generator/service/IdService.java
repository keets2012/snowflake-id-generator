package cn.aoho.generator.service;


import java.util.Date;

public interface IdService {
    long genId();

    Date transTime(long time);

    long makeId(long time, long seq);

    long makeId(long time, long seq, long machine);
}
