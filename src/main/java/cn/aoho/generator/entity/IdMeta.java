package cn.aoho.generator.entity;

/**
 * Created by keets on 2017/6/28.
 */
public class IdMeta {
    //开始时间截 (从2015-01-01起)
    public static final long START_TIME = 1420041600000L;
    // 机器ID所占位数
    public static final long ID_BITS = 10L;
    // 机器ID最大值1023 (此移位算法可很快计算出n位二进制数所能表示的最大十进制数)
    public static final long MAX_ID = ~(-1L << ID_BITS);
    //Sequence所占位数
    public static final long SEQUENCE_BITS = 12L;
    //机器ID偏移量12
    public static final long ID_SHIFT_BITS = SEQUENCE_BITS;
    //时间戳的偏移量12+10=22
    public static final long TIMESTAMP_LEFT_SHIFT_BITS = SEQUENCE_BITS + ID_BITS;
    // Sequence掩码4095
    public static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    // 机器ID掩码1023
    public static final long ID_MASK = ~(-1L << ID_BITS);
    // 时间戳掩码2的41次方减1
    public static final long TIMESTAMP_MASK = ~(-1L << 41L);

    /**
     * 构造方法
     */
    private IdMeta() {
        //构造方法
    }
}
