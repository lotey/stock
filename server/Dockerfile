# 基础镜像使用jdk8-alpine
FROM registry.cn-hangzhou.aliyuncs.com/lotey/openjdk:8-jdk-alpine
# 作者信息
MAINTAINER lotey <loteye@163.com>
# 创建工作目录
RUN mkdir -p /work
# 切换工作目录
WORKDIR /work
# 其效果是在主机 /var/lib/docker 目录下创建了一个临时文件，并链接到容器的/tmp
VOLUME /work
# 将jar包添加到容器中并更名为app.jar
ADD ./target/stock.jar ./app.jar
# 添加时区环境变量，亚洲，上海
ENV TimeZone=Asia/Shanghai
# 使用软连接，并且将时区配置覆盖 / etc/timezone
RUN ln -snf /usr/share/zoneinfo/$TimeZone /etc/localtime && echo $TimeZone > /etc/timezone
## 设置环境变量和时区
#RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
# 暴露接口
EXPOSE 6000
# 指定docker容器启动时运行jar包
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/work/app.jar"]