package com.fleeting.jpa.repository;

import com.fleeting.jpa.annotation.QueryField;
import com.fleeting.jpa.query.LinkType;
import com.fleeting.jpa.query.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by cxx on 2017/9/2.
 */
public class BaseRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T,ID>
        implements BaseRepository<T,ID> {

    private static final Logger logger = LoggerFactory.getLogger(BaseRepositoryImpl.class);
    private final EntityManager entityManager;
    private Class<T> clazz;

    //父类没有不带参数的构造方法，这里手动构造父类
    public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
        this.clazz = domainClass;
    }

    //通过EntityManager来完成查询
    @Override
    public List<T> getListBySQL(String sql) {
        return entityManager.createNativeQuery(sql).getResultList();
    }

    @Override
    public List<T> getList(T t) {
        return this.findAll(getPredicateByProp(t));
    }

    @Override
    public Page<T> getPageList(T t, Pageable pageable) {
        return this.findAll(getPredicateByProp(t),pageable);
    }

    @Override
    public List<T> getListByCondition(List<QueryCondition> conditions) {
        if (conditions == null || conditions.size() == 0)
            throw new IllegalArgumentException("query conditions cannot empty");
//        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//        CriteriaQuery<T> cq = cb.createQuery(this.clazz);
//        Root<T> root = cq.from(this.clazz);
//        cq.where(this.getPredicates(conditions,cb,root));
//        Query query = entityManager.createQuery(cq);
//        return query.getResultList();
        return this.findAll(getPredicateByCondition(conditions));
    }

    @Override
    public Page<T> getListByCondition(List<QueryCondition> conditions,Pageable pageable) {
        if (conditions == null || conditions.size() == 0)
            throw new IllegalArgumentException("query conditions cannot empty");
        return this.findAll(getPredicateByCondition(conditions),pageable);
    }

    @Override
    public int update(Map<String, Object> newValues, List<QueryCondition> conditions) {
        if (newValues == null || newValues.size() == 0) {
            throw new IllegalArgumentException("update newValue cannot be empty");
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(this.clazz);
        Root<T> root = update.from(this.clazz);
        //set new value
        for (String key : newValues.keySet()) {
            update.set(key,newValues.get(key));
        }
        //set where
        if (conditions != null && conditions.size() > 0){
            update.where(this.getPredicates(conditions,cb,root));
        }
        Query query = entityManager.createQuery(update);
        return query.executeUpdate();
    }

    private Predicate[] getPredicates(List<QueryCondition> conditions,CriteriaBuilder cb,Root<T> root){
        List<Predicate> predicateList = new ArrayList<Predicate>();
        for (QueryCondition condition : conditions){
            Predicate depthFirst = getPredicteDepthFirst(condition, cb,root);
            if (depthFirst != null)
                predicateList.add(depthFirst);
        }
        return predicateList.toArray(new Predicate[0]);
    }
    //深度优先解析 QueryCondition.conditions
    private Predicate getPredicteDepthFirst(QueryCondition condition,CriteriaBuilder cb,Root<T> root){
        List<QueryCondition> cds = condition.getConditions();
        List<Predicate> depth = new ArrayList<Predicate>();
        if (cds != null && cds.size() > 0){
            for (QueryCondition cd : cds){
                depth.add(getPredicteDepthFirst(cd, cb, root));
            }
        }
        if (depth.size() > 0){
            if (condition.getLinkType() != null && condition.getLinkType().equals(LinkType.OR)){
                return cb.or(getPredicate(cb,condition,root),cb.and(depth.toArray(new Predicate[0])));
            }else{
                return cb.and(getPredicate(cb,condition,root),cb.and(depth.toArray(new Predicate[0])));
            }
        }else{
            return getPredicate(cb,condition,root);
        }
    }

    //解析当个condition
    private Predicate getPredicate(CriteriaBuilder cb,QueryCondition condition,Root<T> root){
        Predicate predicate = null;
        Object value = condition.getValue();
        switch (condition.getOper()){
            case EQ:
                predicate = cb.equal(root.get(condition.getProperty()), value);
                break;
            case GE:
                if (value instanceof Comparable){
                    predicate = cb.greaterThanOrEqualTo(root.<Comparable>get(condition.getProperty()),(Comparable) value);
                }else{
                    throw new IllegalArgumentException("must impl 'Comparable'");
                }
                break;
            case GT:
                if (value instanceof Comparable){
                    predicate = cb.greaterThan(root.<Comparable>get(condition.getProperty()),(Comparable) value);
                }else{
                    throw new IllegalArgumentException("must impl 'Comparable'");
                }
                break;
            case LE:
                if (value instanceof Comparable){
                    predicate = cb.lessThanOrEqualTo(root.<Comparable>get(condition.getProperty()),(Comparable) value);
                }else{
                    throw new IllegalArgumentException("must impl 'Comparable'");
                }
                break;
            case LT:
                if (value instanceof Comparable){
                    predicate = cb.lessThan(root.<Comparable>get(condition.getProperty()),(Comparable) value);
                }else{
                    throw new IllegalArgumentException("must impl 'Comparable'");
                }
                break;
            case IN:
                if (value instanceof Collection){
                    predicate = root.get(condition.getProperty()).in((Collection)value);
                }else{
                    throw new IllegalArgumentException("'IN' value must instanceof 'Collection'");
                }
                break;
            case LIKE:
                if (value instanceof String){
                    predicate = cb.like(root.<String>get(condition.getProperty()),(String) value);
                }else{
                    throw new IllegalArgumentException("'LIKE' value must be instanceof 'String'");
                }
                break;
            case NOT_EQ:
                predicate = cb.notEqual(root.get(condition.getProperty()), value);
                break;
            case NOT_IN:
                if (value instanceof Collection){
                    predicate = cb.not(root.get(condition.getProperty()).in((Collection) value));
                }else{
                    throw new IllegalArgumentException("'NOT_IN' value must instanceof 'Collection'");
                }
                break;
            case BETWEEN:
                if (condition.getFirstValue() == null)
                    throw new IllegalArgumentException("firstValue of 'BETWEEN' can not be null");
                if (value.getClass().equals(condition.getFirstValue().getClass())){
                    if (value instanceof Comparable){
                        predicate = cb.between(root.<Comparable>get(condition.getProperty()),(Comparable)condition.getFirstValue(),(Comparable)value);
                        break;
                    }else{
                        throw new IllegalArgumentException("must impl 'Comparable'");
                    }
                }else{
                    throw new IllegalArgumentException("params of 'BETWEEN' must be same type");
                }
            case IS_NULL:
                predicate = root.get(condition.getProperty()).isNull();
                break;
            case IS_NOT_NULL:
                predicate = root.get(condition.getProperty()).isNotNull();
                break;
            default:
                throw new IllegalArgumentException("unsupported operator!");
        }
        return predicate;
    }

    private <T> Specification<T> getPredicateByProp(T o){
        final Object local = o;
        return new Specification<T>(){
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder) {
                Map<String, Object> propValue = null;
                try {
                    propValue = getQueryPropMap(local.getClass(), local);
                } catch (Exception e) {
                    logger.error("illegal access prop",e);
                }
                if (propValue != null && propValue.size() > 0){
                    List<Predicate> ps = new ArrayList<>();
                    for (String key : propValue.keySet()){
                        ps.add(criteriaBuilder.equal(root.get(key), propValue.get(key)));
                    }
                    return criteriaBuilder.and(ps.toArray(new Predicate[0]));
                }
                return criteriaBuilder.conjunction();
            }
        };
    }

    private Specification<T> getPredicateByCondition(final List<QueryCondition> conditions){
        return new Specification<T>() {
            @Override
            public Predicate toPredicate(Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.and(getPredicates(conditions,criteriaBuilder,root));
            }
        };
    }

    private static Map<String,Object> getQueryPropMap(Class clazz,Object o) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Field[] fields = clazz.getDeclaredFields();
        Map<String,Object> resultMap = new HashMap<>();
        for (Field field : fields){
            QueryField queryField = field.getAnnotation(QueryField.class);
            if (queryField == null)
                continue;
            Object fv;
            Method method = clazz.getMethod(getMethodName(field.getName()));
            fv = method.invoke(o);
            if (fv != null){
                if (fv instanceof String){
                    if (((String) fv).length() > 0){
                        resultMap.put(field.getName(),fv);
                    }
                }else{
                    resultMap.put(field.getName(),fv);
                }
            }
        }
        return resultMap;
    }

    public static String getMethodName(String fieldName){
        char[] cs = fieldName.toCharArray();
        cs[0]-=32;
        return "get"+String.valueOf(cs);
    }
}
