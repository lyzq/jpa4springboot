package com.fleeting.jpa.repository;

import com.fleeting.jpa.query.QueryCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by cxx on 2017/9/2.
 */
@NoRepositoryBean
public interface BaseRepository<T,ID extends Serializable> extends JpaRepository<T,ID> {

    @Deprecated
    List<T> getListBySQL(String sql);

    List<T> getList(T t);

    Page<T> getList(T t, Pageable pageable);

    List<T> getListByCondition(List<QueryCondition> conditions);

    Page<T> getListByCondition(List<QueryCondition> conditions,Pageable pageable);

    @Transactional
    int update(Map<String, Object> newValues, List<QueryCondition> conditions);
}