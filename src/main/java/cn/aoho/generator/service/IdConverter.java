package cn.aoho.generator.service;


import cn.aoho.generator.entity.IdEntity;

public interface IdConverter {
    long convert(IdEntity id);

    IdEntity convert(long id);
}
