# snowflake升级版全局id生成

## 1. 背景
分布式系统或者微服务架构基本都采用了分库分表的设计，全局唯一id生成的需求变得很迫切。   
传统的单体应用，使用单库，数据库中自增id可以很方便实现。分库之后，首先需要分库键，分库键必然不能重复，所以传统的做法并不能满足需求。概括下来，那业务系统对ID号的要求有哪些呢？

>1.全局唯一性：不能出现重复的ID号，既然是唯一标识，这是最基本的要求。   
>2.趋势递增：在MySQL InnoDB引擎中使用的是聚集索引，由于多数RDBMS使用B-tree的数据结构来存储索引数据，在主键的选择上面我们应该尽量使用有序的主键保证写入性能。   
3.单调递增：保证下一个ID一定大于上一个ID，例如事务版本号、IM增量消息、排序等特殊需求。   
4.信息安全：如果ID是连续的，恶意用户的扒取工作就非常容易做了，直接按照顺序下载指定URL即可；如果是订单号就更危险了，竞对可以直接知道我们一天的单量。所以在一些应用场景下，会需要ID无规则、不规则。   

其中第3和第4点是互斥的。除了功能性需求，还有性能和可靠性的需求：

> - 平均延迟和TP999延迟都要尽可能低；   
> - 可用性5个9；
> - 高QPS。
  
## 2. 进阶历程
自从项目从单体应用拆分成微服务架构后，对全局id部分做了些摸索。
### 2.1 uuid
刚开始拆分业务，id主键都是使用uuid字符串。   
UUID(Universally Unique Identifier)的标准型式包含32个16进制数字，以连字号分为五段，形式为8-4-4-4-12的36个字符。类似这样的字符串：`dc5adf0a-d531-11e5-95aa-3c15c2d22392`。128位，根本不用担心不够用。生成的方法也很简单：

```java
UUID userId = UUID.randomUUID();
```
uuid全球唯一，本地生成，没有网络消耗，产生的性能绝对可以满足。  
其缺点也是显而易见的，比较占地方，和INT类型相比，存储一个UUID要花费更多的空间。
使用UUID后，URL显得冗长，不够友好。ID作为主键时在特定的环境会存在一些问题，比如做DB主键的场景下，UUID就非常不适用：

- MySQL官方有明确的建议主键要尽量越短越好，36个字符长度的UUID不符合要求。
- 对MySQL索引不利：如果作为数据库主键，在InnoDB引擎下，UUID的无序性可能会引起数据位置频繁变动，严重影响性能。

### 2.2 数据库生成
以MySQL举例，利用给字段设置`auto_increment_increment`和`auto_increment_offset`来保证ID自增，每次业务使用下列SQL读写MySQL得到ID号。
参考了[Leaf](https://tech.meituan.com/MT_Leaf.html)的实现思想:

- id server每次批量从数据库取号段，本地缓存这个号段，并且设置阈值，当达到0.8（已用与号段容量的比值），自动去获取一个新的号段，更新本地缓存的号段。
- id client，即具体的调用服务实例，在本地也做一个缓存，实现和id server的缓存差不多，这样做的目的是为了减轻id服务端的压力，同时减少了rpc调用的网络消耗。

以上方案，其缺点是：

1. 号段存在浪费，无论哪个客户端还是服务端重启都会浪费号段。
2. 号段是直接自增，不够随机，对外暴露信息过多。
3. DB宕机会造成整个系统不可用。虽然在DB宕机之后，利用缓存还能进行短暂供号，但是数据库的依赖还是很重。Leaf采用的一般做法是高可用容灾:

>采用一主两从的方式，同时分机房部署，Master和Slave之间采用半同步方式同步数据。同时使用DBProxy做主从切换。当然这种方案在一些情况会退化成异步模式，甚至在非常极端情况下仍然会造成数据不一致的情况，但是出现的概率非常小。

![主从](http://ovci9bs39.bkt.clouddn.com/master-slave.png "主从方式")

## 3. snowflake方案
### 3.1 介绍
考虑到上述方案的缺陷，笔者调查了其他的生成方案，snowflake就是其中一种方案。   
趋势递增和不够随机的问题，在snowflake完全可以解决，Snowflake ID有64bits长，由以下三部分组成：

![snowflake](http://ovci9bs39.bkt.clouddn.com/snowflake-64bit.jpg "snowflake")

1. 第一位为0，不用。
2. timestamp—41bits,精确到ms，那就意味着其可以表示长达(2^41-1)/(1000360024*365)=139.5年，另外使用者可以自己定义一个开始纪元（epoch)，然后用(当前时间-开始纪元）算出time，这表示在time这个部分在140年的时间里是不会重复的，官方文档在这里写成了41bits，应该是写错了。另外，这里用time还有一个很重要的原因，就是可以直接更具time进行排序，对于twitter这种更新频繁的应用，时间排序就显得尤为重要了。

3. machine id—10bits,该部分其实由datacenterId和workerId两部分组成，这两部分是在配置文件中指明的。  

	- datacenterId，方便搭建多个生成uid的service，并保证uid不重复，比如在datacenter0将机器0，1，2组成了一个生成uid的service，而datacenter1此时也需要一个生成uid的service，从本中心获取uid显然是最快最方便的，那么它可以在自己中心搭建，只要保证datacenterId唯一。如果没有datacenterId，即用10bits，那么在搭建一个新的service前必须知道目前已经在用的id，否则不能保证生成的id唯一，比如搭建的两个uid service中都有machine id为100的机器，如果其server时间相同，那么产生相同id的情况不可避免。
	- workerId是实际server机器的代号，最大到32，同一个datacenter下的workerId是不能重复的。它会被注册到consul上，确保workerId未被其他机器占用，并将host:port值存入，注册成功后就可以对外提供服务了。

4. sequence id —12bits,该id可以表示4096个数字，它是在time相同的情况下，递增该值直到为0，即一个循环结束，此时便只能等到下一个ms到来，一般情况下4096/ms的请求是不太可能出现的，所以足够使用了。

### 3.2 实现思路
snowflake方案，id服务端生成，不依赖DB，既能保证性能，且生成的id足够随机。每一毫秒，一台worker可以生成4096个id，如果超过，会阻塞到下一毫秒生成。   
对于那些并发量很大的系统来说,显然是不够的, 那么这个时候就是通过datacenterId和workerId来做区分,这两个ID,分别是5bit,共10bit,最大值是1024(0-1023)个, 在这种情况下,snowflake一毫秒理论上最大能够生成的ID数量是约42W个,这是一个非常大的基数了,理论上能够满足绝大多数系统的并发量。

该方案依赖于系统时钟，需要考虑时钟回拨的问题。本地缓存上一次请求的lastTimestamp，一个线程过来获取id时，首先校验当前时间是否小于上一次ID生成的时间戳。如果小于说明系统时钟被修改过，回退在上一次ID生成时间之前应当抛出异常！如此可以解决运行中，系统时钟被修改的问题。

另一种情况是，server服务启动时，系统的时间被回拨（虽然比较极端，还是列在考虑中），这样有可能与之前生成的id冲突，全局不唯一。这边解决方法是利用项目的服务发现与注册组件consul，在consul集群存储最新的lastTimestamp，key为对应的machine-id。consul的一致性基于raft算法，并利用Gossip协议：
>Consul uses a gossip protocol to manage membership and broadcast messages to the cluster. All of this is provided through the use of the Serf library.

具体的协议算法，可以参考[Gossip](https://www.consul.io/docs/internals/gossip.html)。   
每次server实例启动时，实例化id生成bean的时候，会首先校验当前时间与consul集群中该worker对应的lastTimestamp大小，如果当前时间偏小，则抛出异常，服务启动失败并报警。

实例化时，进行校验：

```java
    public IdServiceImpl(long workerId, ConsulClient consulClient) {
        if (workerId > idMeta.MAX_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", idMeta.MAX_ID));
        }
        this.workerId = workerId;
        this.consulClient = consulClient;
        validateStoredTimestamp();
        log.info("worker starting. timestamp left shift {},  worker id bits {}, sequence bits {}, workerid {}", idMeta.TIMESTAMP_LEFT_SHIFT_BITS, idMeta.ID_BITS, idMeta.SEQUENCE_BITS, workerId);
    }
```

校验函数：

```java
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
```

validateTimestamp: 

```java
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
```

获取id方法：

```java
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
```

**本文的源码地址：   
GitHub：https://github.com/keets2012/snowflake-id-generator      
码云： https://gitee.com/keets/snowflake-id-generator**

### 订阅最新文章，欢迎关注我的公众号

![微信公众号](http://ovci9bs39.bkt.clouddn.com/qrcode_for_gh_ca56415d4966_430.jpg)


----
参考：   

1. [www.consul.io](https://www.consul.io/docs)
2. [leaf](https://tech.meituan.com/MT_Leaf.html)
3. [Twitter的分布式自增ID算法snowflake (Java版)](http://www.cnblogs.com/relucent/p/4955340.html)
