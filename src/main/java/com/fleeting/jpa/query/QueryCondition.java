package com.fleeting.jpa.query;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cxx on 2017/8/31.
 */
public class QueryCondition implements Serializable{
    // /属性名称查询条件名称
    private String property;
    // 条件规则
    private Operators oper = Operators.EQ;
    // 条件设置的值
    private Object value;
    // between查询条件的第一个参数
    private Object firstValue;

    private List<QueryCondition> conditions;

    private LinkType linkType = null;

    public LinkType getLinkType() {
        return linkType;
    }

    public List<QueryCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<QueryCondition> conditions,LinkType linkType) {
        this.conditions = conditions;
        this.linkType = linkType;
    }

    /**
     * between
     *
     * @param value
     * @param property
     * @param firstValue
     */
    public QueryCondition(Object firstValue, String property, Object value) {
        super();
        this.property = property;
        this.value = value;
        this.firstValue = firstValue;
        this.oper = Operators.BETWEEN;
    }
    /**
     * 除between之外
     *
     * @param property
     * @param oper
     * @param value
     */
    public QueryCondition(String property, Operators oper, Object value) {
        this.property = property;
        this.oper = oper;
        likehandler(oper, value);
    }

    /**
     * 模糊查询条件
     *
     * @param property
     * @param value
     * @param isLeft
     *            true 左边模糊，否则右模糊
     */
    public QueryCondition(String property, String value, boolean isLeft) {
        this.property = property;
        this.oper = Operators.LIKE;
        if (isLeft) {
            this.value = "%" + value;
        } else {
            this.value = value + "%";
        }
    }

    /***
     * IN 条件集合
     *
     * @param property
     * @param values
     */
    public <T> QueryCondition(String property, List<T> values) {
        this.property = property;
        this.oper = Operators.IN;
        this.value = values;
    }

    public QueryCondition(String property,Operators oper){
        if (oper.equals(Operators.IS_NULL) || oper.equals(Operators.IS_NOT_NULL)){
            this.property = property;
            this.oper = oper;
        }else{
            throw new IllegalArgumentException("illegal oper");
        }
    }

    private void likehandler(Operators oper, Object value) {
        if (Operators.LIKE.equals(oper)) {
            this.value = "%" + value + "%";
        } else {
            this.value = value;
        }
    }

    public QueryCondition() {
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public Operators getOper() {
        return oper;
    }

    public void setOper(Operators oper) {
        this.oper = oper;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public void setFirstValue(Object firstValue) {
        this.firstValue = firstValue;
    }

    /**
     * 得到查询条件集合
     *
     * @param QueryConditions
     * @return
     */
    public final static Set<QueryCondition> getQueryConditions(QueryCondition... QueryConditions) {
        if (QueryConditions != null && QueryConditions.length > 0) {
            Set<QueryCondition> condi = new HashSet<QueryCondition>(QueryConditions.length);
            for (QueryCondition e : QueryConditions) {
                condi.add(e);
            }
            return condi;
        } else {
            return Collections.emptySet();
        }

    }
}
