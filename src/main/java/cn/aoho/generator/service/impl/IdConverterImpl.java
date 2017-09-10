package cn.aoho.generator.service.impl;

import cn.aoho.generator.entity.IdEntity;
import cn.aoho.generator.entity.IdMeta;
import cn.aoho.generator.service.IdConverter;

/**
 * Created by keets on 2017/9/9.
 */
public class IdConverterImpl implements IdConverter {
    public long convert(IdEntity id) {
        long ret = 0;

        ret |= id.getSequence();

        ret |= id.getWorker() << IdMeta.SEQUENCE_BITS;

        ret |= id.getTimeStamp() << IdMeta.TIMESTAMP_LEFT_SHIFT_BITS;

        return ret;
    }

    public IdEntity convert(long id) {
        IdEntity ret = new IdEntity();

        ret.setSequence(id & IdMeta.SEQUENCE_MASK);

        ret.setWorker((id >>> IdMeta.SEQUENCE_BITS) & IdMeta.ID_MASK);

        ret.setTimeStamp((id >>> IdMeta.TIMESTAMP_LEFT_SHIFT_BITS) & IdMeta.TIMESTAMP_MASK);

        return ret;
    }
}
