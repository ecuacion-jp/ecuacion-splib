# ecuacion-splib

## What is it?

`ecuacion-splib` is a `spring boot` based libraries.
The purpose of it is to diminish the code and accelerate developments as much as possible. 

`spring boot 3` is used in `ecuacion-splib`, and `jakarta EE 10` is used in `ecuacion-lib` and other libraries, utils and apps for compatibility.

## System Requirements

- JDK 21 or above.

## Documentation

(See `Documentation` part of the `README.md` in each module)

## Introduction

1. Put the following tags to your `pom.xml` (put `<repositories>` tag as a child tag of `<project>` tag).

    ```xml
    <repositories> 
        <repository>
            <id>ecuacion-repo-http</id>
            <name>ecuacion-repo-http</name>
            <url>http://maven-repo.ecuacion.jp/public</url>
        </repository>
    </repositories>
    ```

2. Set following `parent` tag to your `pom.xml`.  

    ```xml
	<parent>
		<groupId>jp.ecuacion.splib</groupId>
		<artifactId>ecuacion-splib-parent</artifactId>
	    <!-- Put the latest release version -->
	    <version>x.x.x</version>
	</parent>
    ```

3. Add dependent `ecuacion` modules to your `pom.xml`.  
   (This is the example of `ecuacion-splib-core` module. Check `Introduction` part of `README` in the module you want to add to your project.)

    ```xml
    <dependency>
        <groupId>jp.ecuacion.splib</groupId>
        <artifactId>ecuacion-splib-core</artifactId>
	    <!-- No version tag needed since ecuacion-splib-parent has dependencyManagement versions. -->
    </dependency>
    ```
    
4. Add dependent external modules to your `pom.xml`.  
   (Check `Dependent External Libraries > Manual Load Needed Libraries` part of `README` in the module you want to add to your project.)
