**jpa4springboot**

先简单介绍一下spring-data-jpa：

在实际开发中，对数据库的操作无非就“增删改查”，就最为普遍的单表操作而言，除了表和字段不同外，语句都是类似的，开发人员需要写大量类似而枯燥的语句来完成业务逻辑。

之前的项目上整合Hibernate，我们以操作Java实体的方式最终将数据改变映射到数据库表中。

为了解决抽象的各个java实体基本的增删改查，我们一般都会用泛型抽象简化出一个模板的Dao加以继承。但是这样使用依然不是很方便，我们需要针对每一个实体继承模板Dao接口，再编写一个实现。

spring-data-jpa为你提供了模板，还可以帮你实现接口。我们只需要写一个接口就能对数据进行访问。

     public interface UserRepository extends JpaRepository<User, Long> {
          User findByName(String name);
          @Query("from User u where u.name=:name")
          User findUser(@Param("name") String name);
     }
 
就这样一段代码，无需编写实现类，在项目启动的时候底层会自动帮你产生相应的实现类。 

更多的内容可以参考官方文档，写的特别详细。

对于一些简单通用的sql，spring-data-jpa可以很优雅的帮你解决。但是复杂一点的，使用起来就有点力不重心了，所以这里针对这种情况进行优化。

jpa4springboot则是在这个之上添加自定义，目标是简化开发，提升开发效率。

**使用说明**

1.仅限于springboot项目
2.当前parent版本是1.5.6.RELEASE

**简单上手**
第一步，添加依赖（把项目拉下来后直接打到本地maven库就好了，可以自行部署到自有的maven库）

     <groupId>com.fleeting</groupId>
     <artifactId>jpa4springboot</artifactId>
     <version>1.0-SNAPSHOT</version>

第二步，在项目入口处添加

     @EnableJpaRepositories(repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class)

第三步，声明对应的Repository，继承BaseRepository


     public interface ProductRepository extends BaseRepository<Product,Long> {

     }


以上三步后就可以使用spring-data-jpa和扩展的功能。

springboot-jpa-data的使用可以参考官方文档，这里做了扩展，以下为扩展的内容：

1. getList

使用注解QueryField来标记实体类中需要作为查询条件的字段，配合springmvc使用简单方便快捷：

     @RequestMapping("/prodlist")
     public String prodlist(Product product,Integer page,Map<String,Object> resultMap){
         if (page == null)
             page = 0;
         Page<Product> all = productDao.getList(product,new PageRequest(page,20));
         resultMap.put("pageData",all);
         return "background/prod/prodlist";
     }

2. getListByCondition

使用QueryCondition来封装where条件，所见即所得：

     List<QueryCondition> conditions = new ArrayList<>();
     QueryCondition cd = new QueryCondition("haha","prodName","鲨鱼");
     QueryCondition cd1 = new QueryCondition("prodName", Operators.LIKE,"鲨鱼");
     QueryCondition cd2 = new QueryCondition("prodDesc", Operators.IS_NULL);
     QueryCondition cd3 = new QueryCondition("prodCode", Operators.IS_NOT_NULL);
     QueryCondition cd4 = new QueryCondition("prodName", Operators.NOT_IN,Arrays.asList("鲨鱼"));
     QueryCondition cd5 = new QueryCondition("prodCode", Operators.EQ, "大鲨鱼");
     cd.setConditions(Arrays.asList(cd1), LinkType.OR);
     cd1.setConditions(Arrays.asList(cd2),LinkType.AND);
     cd2.setConditions(Arrays.asList(cd3,cd4),LinkType.OR);
     conditions.add(cd5);
     conditions.add(cd);
     productDao.getListByCondition(conditions)
     productDao.getListByCondition(conditions,new PageRequest(0,20))

这里实现了一个稍微复杂的sql。

3.update

     int update(Map<String, Object> newValues, List<QueryCondition> conditions);

后续会继续优化改进，欢迎提出各种意见和建议。
