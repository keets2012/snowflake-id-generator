package cn.aoho.generator.service;


import cn.aoho.generator.entity.IdEntity;

/**
 * Created by keets on 2017/9/9.
 */
public interface IdConverter {
    long convert(IdEntity id);

    IdEntity convert(long id);
}
