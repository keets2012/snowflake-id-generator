package cn.aoho.generator.service.impl;

import cn.aoho.generator.entity.IdEntity;
import cn.aoho.generator.entity.IdMeta;
import cn.aoho.generator.service.IdConverter;
import cn.aoho.generator.service.IdService;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class IdServiceImpl implements IdService {
    // 上一毫秒数
    private static long lastTimestamp = -1L;
    //毫秒内Sequence(0~4095)
    private static long sequence = 0L;
    //机器ID(0-1023)
    private final long workerId;
    //各种元数据
    protected IdMeta idMeta;

    private ConsulClient consulClient;

    private enum Periods {
        START, RUNNING
    }

    /**
     * 构造
     *
     * @param workerId 机器ID((0-1023)
     */
    public IdServiceImpl(long workerId, ConsulClient consulClient) {
        if (workerId > idMeta.MAX_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", idMeta.MAX_ID));
        }
        this.workerId = workerId;
        this.consulClient = consulClient;
        validateStoredTimestamp();
        log.info("worker starting. timestamp left shift {},  worker id bits {}, sequence bits {}, workerid {}", idMeta.TIMESTAMP_LEFT_SHIFT_BITS, idMeta.ID_BITS, idMeta.SEQUENCE_BITS, workerId);
    }

    /**
     * 生成ID（线程安全）
     *
     * @return id
     */
    public synchronized long genId() {
        long timestamp = timeGen();

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟被修改过，回退在上一次ID生成时间之前应当抛出异常！！！
        validateTimestamp(timestamp, lastTimestamp, Periods.RUNNING);

        //如果是同一时间生成的，则进行毫秒内sequence生成
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & IdMeta.SEQUENCE_MASK;
            //溢出处理
            if (sequence == 0) {//阻塞到下一毫秒,获得新时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {//时间戳改变，毫秒内sequence重置
            sequence = 0L;
        }
        //上次生成ID时间截
        lastTimestamp = timestamp;
        consulClient.setKVValue(String.valueOf(workerId), String.valueOf(lastTimestamp));
        //移位并通过或运算组成64位ID
        return ((timestamp - idMeta.START_TIME) << idMeta.TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << idMeta.ID_SHIFT_BITS) | sequence;
    }

    /**
     * 如果当前时间戳小于上一次ID生成的时间戳，说明系统时钟被修改过，回退在上一次ID生成时间之前应当抛出异常！！！
     *
     * @param lastTimestamp 上一次ID生成的时间戳
     * @param timestamp     当前时间戳
     */
    private void validateTimestamp(long timestamp, long lastTimestamp, Periods period) {
        if (timestamp < lastTimestamp) {
            log.error(String.format("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp));
            throw new IllegalStateException(String.format("Clock moved backwards in %s.  Refusing to generate id for %d milliseconds", period, lastTimestamp - timestamp));
        }
    }

    /**
     * checks for timestamp by workerId when server starts.
     * if server starts for the first time, just let it go and log warns.
     * if current timestamp is smaller than the value stored in consul server, throw exception.
     */
    private void validateStoredTimestamp() {
        long current = timeGen();
        Response<GetValue> keyValueResponse = consulClient.getKVValue(String.valueOf(workerId));
        if (keyValueResponse.getValue() != null) {
            lastTimestamp = Long.parseLong(keyValueResponse.getValue().getDecodedValue());
            validateTimestamp(current, lastTimestamp, Periods.START);
        } else {
            log.warn(String.format("clock in consul is null.  Generator works as for the 1st time."));
        }
    }

    /**
     * 阻塞到下一毫秒,获得新时间戳
     *
     * @param lastTimestamp 上次生成ID时间截
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 对时间戳单独进行解析
     *
     * @param time 时间戳
     * @return 生成的Date时间
     */
    public Date transTime(long time) {
        return new Date(time + idMeta.START_TIME);
    }

    /**
     * 根据时间戳和序列号生成ID
     *
     * @param timeStamp 时间戳
     * @param sequence  序列号
     * @return 生成的ID
     */
    public long makeId(long timeStamp, long sequence) {
        return makeId(timeStamp, workerId, sequence);
    }

    /**
     * 根据时间戳、机器ID和序列号生成ID
     *
     * @param timeStamp 时间戳
     * @param worker    机器ID
     * @param sequence  序列号
     * @return 生成的ID
     */
    public long makeId(long timeStamp, long worker, long sequence) {
        IdConverter idConverter = new IdConverterImpl();
        return idConverter.convert(new IdEntity(timeStamp, worker, sequence));
    }
}