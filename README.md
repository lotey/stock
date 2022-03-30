# A股爬虫监控

## 模块说明
- common-基础模块
- kzz-可转债监控
- mock-模拟测试
- server-股票监控

## 使用说明
- 项目使用java语言开发，使用springboot+mybatis框架，intelj idea IDE,maven构建

## 开发步骤
- 安装或配置好idea/eclipse,maven,git,mysql
- git克隆本工程
- 使用工程内db目录的mysql脚本初始化mysql数据库
- 导入克隆的项目到idea中
- 修改模块内的application.yml配置文件，修改数据库信息，钉钉通知地址，熊猫代理信息
- 使用maven命令构建和打包，mvn clean install -Dmaven.test.skip=true

# 最后
## 如果喜欢本项目请star或fork
