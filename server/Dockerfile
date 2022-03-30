# 基础镜像使用jre8-alpine
FROM openjdk:8-jre-alpine
# 作者信息
MAINTAINER lotey <loteye@163.com>
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /tmp
# 将jar包添加到容器中并更名为app.jar
ADD target/stock.jar /app.jar
# 运行过程中创建一个stock.jar文件
#RUN bash -c 'touch /app.jar'
# 设置环境变量
ENV TZ 'Asia/Shanghai'
# 暴露接口
EXPOSE 5000
# 指定docker容器启动时运行jar包
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]