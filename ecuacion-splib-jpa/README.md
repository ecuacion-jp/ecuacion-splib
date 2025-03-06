# ecuacion-splib-jpa

## What is it?

`ecuacion-splib-jpa` is a `spring boot` based JPA related libraries.

## System Requirements

- JDK 21 or above.

## Dependent Ecuacion Libraries

### Automatically Loaded Libraries

- `jp.ecuacion.lib:ecuacion-splib-core`
- `jp.ecuacion.lib:ecuacion-lib-jpa`

(modules depending on `ecuacion-splib-core`)
- `jp.ecuacion.lib:ecuacion-lib-core`

### Manual Load Needed Libraries

(none)

## Dependent External Libraries

### Automatically Loaded Libraries

- `org.springframework.boot:spring-boot-starter-data-jpa`

(modules depending on `ecuacion-splib-core`)
- `org.springframework.boot:spring-boot-starter-validation`

(modules depending on `ecuacion-lib-core`)
- `org.apache.commons:commons-lang3`
- `org.apache.commons:commons-exec` (@Deprecated. It will be removed in the future release)

### Manual Load Needed Libraries

(modules depending on `ecuacion-lib-core`)
- `jakarta.mail:jakarta.mail-api` (If you want to use the mail related utility: `jp.ecuacion.lib.core.util.MailUtil`. `org.springframework.boot:spring-boot-starter-mail` is also fine.)


## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-splib-jpa/)

## Introduction

Check [Introduction](https://github.com/ecuacion-jp/ecuacion-splib) part of `README` in `ecuacion-splib`.  
The description of dependent `ecuacion` modules is as follows.

```xml
<dependency>
    <groupId>jp.ecuacion.splib</groupId>
    <artifactId>ecuacion-splib-jpa</artifactId>
    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
</dependency>
```

## Features

### Group

#### Description

`Group` is the feature that accounts in a same group share the data, 
but accounts can't see the data belonging to other groups.

#### Requirements

1. filtering the data of other groups

1. administrators can access data of any group.


#### Implementations

- `requirement 1.` is realized by (not JPA's, but) Hibernate's `@Filter`.  


- The name of the `@Filter` is fixed: `groupFilter`.


- This feature is not mandatory. If you want to use the feature, 
  you need to create a class extends this.

- With web app `SplibAccountControllerAdvice` supports to enable the `@Filter`.
  
#### How To Use

- Define `@FilterDef` and `@Filter` in `Entity`.

```java
@FilterDef(name = "groupFilter", 
    parameters = @ParamDef(name = "groupId", type = LongJavaType.class),
    defaultCondition = "GROUP_ID = :groupId")
```

```java
@Filter(name = "groupFilter")
```

- Create `AppAccountControllerAdvice` class extending `SplibAccountControllerAdvice`, or `SplibJpaAccountControllerAdvice` if the app uses JPA.

```java
@ControllerAdvice
public class AppAccountControllerAdvice extends SplibJpaAccountControllerAdvice {

}
```

#### Constraint

- When you use native queries (spring `@query` with `native = true` or standard JPA native query), 
  `group` feature is not suppoted. If you have to use them, filter those records manually because standard JPA, especially `entityManager#find()` is not supported by Hibernate's `@Filter` feature.

### Soft Delete

#### Description

`Soft Delete` is the feature that 
the "deleted" mark is set to the soft-delete column instead of physically delete it
when they are deleted by user operations.

The only reason of the existence of this feature is to keep the deleted data in the database.
it can be used for investigations of bugs or recoverying the deleted data
(not by the system feature, but the manual task).  

The point is, in the business view the record is definitely deleted.  
So users cannot see the deleted records or recover the deleted data by themselves.

#### Requirements

1. excluding soft-deleted records from search results

1. physically deleting the soft-deleted record 
when inserting a new record with the same unique key as soft-deleted one


#### Implementations

- `requirement 1.` is realized by (not JPA's, but) Hibernate's `@Filter`.  
  (We did not use `@Where` because there's no way to disable it. 
  It can be avoided by using native sqls but it's not very handy.)


- The name of the `@Filter` is fixed: `softDeleteFilter`.


- `requirement 2.` is realized by AOP. `SplibSoftDeleteAdvice` class is the advice class which handles it.
  `SplibSoftDeleteAdvice#save` is called by AOP right before the `Repository#save` method is called.  
  
  The advice class is able to delete soft deleted record physically because soft delete filter is disabled right before the deletion.
  The group filter is also disabled at the same time so that the soft deleted record can be deleted even thogh it belongs to the other group than the account who trying to create a new record.

- This feature is not mandatory. If you want to use the feature, 
  you need to create a class extends this.


- unique constraint other than the one linked with the natural key is not supported for now. It's the issue #22.

- With web app `SplibAccountControllerAdvice` supports to enable the `@Filter`.

#### How To Use

- Define `@FilterDef` and `@Filter` in `Entity`.

```java
@FilterDef(name = "softDeleteFilter", defaultCondition = "DEL_FLG = false")
```

```java
@Filter(name = "softDeleteFilter")
```

- Create `AppSoftDeleteAdvice` class extending `SplibSoftDeleteAdvice`.

```java
@Aspect
@Component
public class SoftDeleteAdvice extends SplibSoftDeleteAdvice {

}
```

- Create `AppAccountControllerAdvice` class extending `SplibAccountControllerAdvice`, or `SplibJpaAccountControllerAdvice` if the app uses JPA.

```java
@ControllerAdvice
public class AppAccountControllerAdvice extends SplibJpaAccountControllerAdvice {

}
```

- Create repository classes extending `SplibRepository` and implement methods below as follows. (It's an example of `Acc` repository.)

```java
  @Query(nativeQuery = true, 
      value = "select * from ACC where ID = :#{#entity.id} and del_flg = true")
  Optional<Acc> findByIdAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") Acc entity);
  
  @Query(nativeQuery = true, 
      value = "select * from ACC where mail_address = :#{#entity.mailAddress} and del_flg = true")
  Optional<Acc> findByNaturalKeyAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") Acc entity);
  
  @Modifying
  @Query(nativeQuery = true, 
      value = "delete from ACC where ID = :#{#entity.id} and del_flg = true")
  void deleteByIdAndSoftDeleteFieldTrueFromAllGroups(@Param("entity") Acc entity);
}
```

#### Constraint

- When you use native queries (spring `@query` with `native = true` or standard JPA native query), 
  `Soft Delete` feature is not suppoted. If you have to use them, filter those records manually because standard JPA, especially `entityManager#find()` is not supported by Hibernate's `@Filter` feature.
