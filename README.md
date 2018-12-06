---
order: 1
title: 使用Docker部署Spring Boot
type: 进阶
---

编译部署docker镜像，可以直接通过docker命令工具直接编译，也可以通过第三方插件进行编译

### 初始化项目

通过[spring boot initializer](https://start.spring.io/)初始化spring boot 项目.  

![5c08dcb426a79](https://i.loli.net/2018/12/06/5c08dcb426a79.png)

添加GreetingController

```
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@RestController
public class DemoApplication {

	@GetMapping("/greeting")
	public String greeeting(@RequestParam(name="name", required=false, defaultValue="World")String name){
		return "Hello " + name;
	}


	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}

```

通过mvn clean package检验代码是否可以正常打包，mvn spring-boor:run 检验代码是否正常对外提供服务

```
mvn clean package                      //打包
mvn spring-boot:run                    //运行
curl http://localhost:8080/greeting?name=Jet   //请求
```

### 直接通过docker命令编译

添加Dockerfile. Dockerfile 是包含了一系列docker命令，这些命令会逐行执行，最终打包生成最终的docker image文件

```

FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS
ADD target/demo-0.0.1-SNAPSHOT.jar demo.jar
EXPOSE 8080
# ENTRYPOINT exec java $JAVA_OPTS -jar demo.jar
# For Spring-Boot project, use the entrypoint below to reduce Tomcat startup time.
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar demo.jar

```

- From : 命令表明构建依赖openjdk，首先会拉取openjdk镜像

- Volume:  挂载一个目录

- ARG:  定义参数，参数在docker build 之前有效，编译完成后参数不起作用

- ENV：定义更新环境变量，如设置版本号，它的作用域比ARG强，可以持久化

- ADD：拷贝本地文件等效COPY， 不过ADD还可以支持本地解压

- EXPOSE:  容器向外暴露的端口

- ENTRYPOINT：用来设置执行image 的命令，可以是基本的shell命令也可以是shell脚本

***编译：***

```
docker build -f Dockerfile -t demo .
Sending build context to Docker daemon  16.78MB
Step 1/7 : FROM openjdk:8-jdk-alpine
 ---> 97bc1352afde
Step 2/7 : VOLUME /tmp
 ---> Using cache
 ---> 58512f70c863
Step 3/7 : ARG JAVA_OPTS
 ---> Using cache
 ---> d053606ae55e
Step 4/7 : ENV JAVA_OPTS=$JAVA_OPTS
 ---> Using cache
 ---> 684e9b009f8a
Step 5/7 : ADD target/demo-0.0.1-SNAPSHOT.jar demo.jar
 ---> f67a0ecff8fe
Step 6/7 : EXPOSE 8080
 ---> Running in 6a57431c30ad
Removing intermediate container 6a57431c30ad
 ---> 89de8f18cd24
Step 7/7 : ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar demo.jar
 ---> Running in 35a3471d7e24
Removing intermediate container 35a3471d7e24
 ---> 1e1cf9f9fe34
Successfully built 1e1cf9f9fe34
Successfully tagged demo:latest
```

***镜像列表:***

```
docker image ls
REPOSITORY                  TAG                 IMAGE ID            CREATED             SIZE
demo                        latest              1e1cf9f9fe34        3 minutes ago       119MB
```



***运行：***

```
docker run -p 8080:8080 demo                      //运行
curl http://192.168.99.100:8080/greeting?name=Jet //查看
```



### 通过maven plugin编译docker

maven 提过了很多插件用来编译打包docker镜像，spotify和fabric 都有提供相应的maven plugin，这里使用fabric提供的插件

```
    <build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>io.fabric8</groupId>
				<artifactId>docker-maven-plugin</artifactId>
				<version>${docker.version}</version>
				<configuration>
					<dockerHost>tcp://192.168.99.100:2376</dockerHost>
                    <verbose>true</verbose>
                    <images>
                        <image>
                            <name>demo</name>
                            <build>
                                <dockerFile>${basedir}/Dockerfile</dockerFile>
                            </build>
                        </image>
                    </images>
				</configuration>
			</plugin>
		</plugins>
	</build>
```

- dockerHost 指定了docker-machine的地址

- image需要指定image name以及dockerfile的路径



***编译运行：***

```
 mvn clean package -DskipTests docker:build
[INFO] Scanning for projects...
[INFO]
[INFO] --------------------------< com.example:demo >--------------------------
[INFO] Building demo 0.0.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- maven-clean-plugin:3.1.0:clean (default-clean) @ demo ---
[INFO] Deleting /Users/jet/Downloads/demo/target
[INFO]
[INFO] --- maven-resources-plugin:3.1.0:resources (default-resources) @ demo ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 1 resource
[INFO] Copying 0 resource
[INFO]
[INFO] --- maven-compiler-plugin:3.8.0:compile (default-compile) @ demo ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 1 source file to /Users/jet/Downloads/demo/target/classes
[INFO]
[INFO] --- maven-resources-plugin:3.1.0:testResources (default-testResources) @ demo ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/jet/Downloads/demo/src/test/resources
[INFO]
[INFO] --- maven-compiler-plugin:3.8.0:testCompile (default-testCompile) @ demo ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 1 source file to /Users/jet/Downloads/demo/target/test-classes
[INFO]
[INFO] --- maven-surefire-plugin:2.22.1:test (default-test) @ demo ---
[INFO] Tests are skipped.
[INFO]
[INFO] --- maven-jar-plugin:3.1.0:jar (default-jar) @ demo ---
[INFO] Building jar: /Users/jet/Downloads/demo/target/demo-0.0.1-SNAPSHOT.jar
[INFO]
[INFO] --- spring-boot-maven-plugin:2.1.1.RELEASE:repackage (repackage) @ demo ---
[INFO] Replacing main artifact with repackaged archive
[INFO]
[INFO] --- docker-maven-plugin:0.27.1:build (default-cli) @ demo ---
[INFO] Building tar: /Users/jet/Downloads/demo/target/docker/demo/tmp/docker-build.tar
[INFO] DOCKER> [demo:latest]: Created docker-build.tar in 561 milliseconds
[INFO] DOCKER> Step 1/7 : FROM openjdk:8-jdk-alpine
[INFO] DOCKER>
[INFO] DOCKER> ---> 97bc1352afde
[INFO] DOCKER> Step 2/7 : VOLUME /tmp
[INFO] DOCKER>
[INFO] DOCKER> ---> Using cache
[INFO] DOCKER> ---> 58512f70c863
[INFO] DOCKER> Step 3/7 : ARG JAVA_OPTS
[INFO] DOCKER>
[INFO] DOCKER> ---> Using cache
[INFO] DOCKER> ---> d053606ae55e
[INFO] DOCKER> Step 4/7 : ENV JAVA_OPTS=$JAVA_OPTS
[INFO] DOCKER>
[INFO] DOCKER> ---> Using cache
[INFO] DOCKER> ---> 684e9b009f8a
[INFO] DOCKER> Step 5/7 : ADD target/demo-0.0.1-SNAPSHOT.jar demo.jar
[INFO] DOCKER>
[INFO] DOCKER> ---> 3151b0bb4247
[INFO] DOCKER> Step 6/7 : EXPOSE 8080
[INFO] DOCKER>
[INFO] DOCKER> ---> Running in 6600553b0b55
[INFO] DOCKER> Removing intermediate container 6600553b0b55
[INFO] DOCKER> ---> 520cbfaa987d
[INFO] DOCKER> Step 7/7 : ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar demo.jar
[INFO] DOCKER>
[INFO] DOCKER> ---> Running in 0274c2b75dd2
[INFO] DOCKER> Removing intermediate container 0274c2b75dd2
[INFO] DOCKER> ---> b9575c986d05
[INFO] DOCKER> Successfully built b9575c986d05
[INFO] DOCKER> Successfully tagged demo:latest
[INFO] DOCKER> [demo:latest]: Built image sha256:b9575
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 12.386 s
[INFO] Finished at: 2018-12-06T18:25:27+08:00
[INFO] ------------------------------------------------------------------------
```

### 代码

详细代码可见 [docker-demo](https://github.com/JetQin/docker-demo)

### 参考

[Best practices for writing Dockerfiles](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
