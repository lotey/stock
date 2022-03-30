/*
 Navicat Premium Data Transfer

 Source Server         : bcc
 Source Server Type    : MySQL
 Source Server Version : 80023
 Source Host           : 127.0.0.1:3306
 Source Schema         : stock

 Target Server Type    : MySQL
 Target Server Version : 80023
 File Encoding         : 65001

 Date: 30/03/2022 21:06:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint(20) NOT NULL COMMENT '编号',
  `value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据值',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签名',
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类型',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `sort` int(11) NOT NULL COMMENT '排序（升序）',
  `parent_id` bigint(20) NULL DEFAULT 0 COMMENT '父级编号',
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标记',
  `create_by` bigint(20) NOT NULL COMMENT '创建者',
  `create_date` datetime(0) NOT NULL COMMENT '创建时间',
  `update_by` bigint(20) NOT NULL COMMENT '更新者',
  `update_date` datetime(0) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_avg_price
-- ----------------------------
DROP TABLE IF EXISTS `t_avg_price`;
CREATE TABLE `t_avg_price`  (
  `id` bigint(0) NOT NULL,
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `avg` decimal(10, 2) NULL DEFAULT NULL,
  `avg5` decimal(10, 2) NULL DEFAULT NULL,
  `avg10` decimal(10, 2) NULL DEFAULT NULL,
  `avg20` decimal(10, 2) NULL DEFAULT NULL,
  `avg30` decimal(10, 2) NULL DEFAULT NULL,
  `last_10_trend` int(0) NULL DEFAULT -1 COMMENT '近10天5日线趋势；-1:未知；0：平；1:上涨；2:下跌',
  `last_10_month_trend` int(0) NULL DEFAULT -1 COMMENT '近10天30日线趋势；-1:未知；0：平；1:上涨；2:下跌',
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '股票-行业-概念关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_cu_monitor
-- ----------------------------
DROP TABLE IF EXISTS `t_cu_monitor`;
CREATE TABLE `t_cu_monitor`  (
  `id` bigint(0) NOT NULL,
  `date` date NULL DEFAULT NULL,
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '监控类型；A:普通类；B：抄底类；C：加速类；D：妖股类',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态；0:最新；1:历史',
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dict
-- ----------------------------
DROP TABLE IF EXISTS `t_dict`;
CREATE TABLE `t_dict`  (
  `id` bigint(20) NOT NULL,
  `code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型；0:股票1:可转债',
  `cir_market_value` decimal(10, 2) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dict_prop
-- ----------------------------
DROP TABLE IF EXISTS `t_dict_prop`;
CREATE TABLE `t_dict_prop`  (
  `id` bigint(0) NOT NULL COMMENT '主键',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票编码',
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票名称',
  `company_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司名称',
  `province` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '省份',
  `industry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行业',
  `idy_segment` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '细分行业',
  `plate` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '板块概念',
  `lyr` decimal(10, 2) NULL DEFAULT NULL COMMENT '市盈率（静）',
  `ttm` decimal(10, 2) NULL DEFAULT NULL COMMENT '市盈率（动）',
  `ceo` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '董事长',
  `artificial_person` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '法人代表',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '办公地址',
  `latest_lift_ban` date NULL DEFAULT NULL COMMENT '最近解禁日期',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_his_quotation
-- ----------------------------
DROP TABLE IF EXISTS `t_his_quotation`;
CREATE TABLE `t_his_quotation`  (
  `id` bigint(0) NOT NULL COMMENT '主键',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票编码',
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票名称',
  `date` date NULL DEFAULT NULL COMMENT '交易日期',
  `current` decimal(15, 3) NULL DEFAULT NULL COMMENT '当前价格',
  `init` decimal(15, 3) NULL DEFAULT NULL COMMENT '初始价，即昨天的收盘价',
  `open` decimal(15, 3) NULL DEFAULT NULL COMMENT '开盘价',
  `high` decimal(15, 3) NULL DEFAULT NULL COMMENT '最高价',
  `low` decimal(15, 3) NULL DEFAULT NULL COMMENT '最低价',
  `close` decimal(15, 3) NULL DEFAULT NULL COMMENT '关盘价',
  `volume` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易量',
  `volume_amt` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易额',
  `avg` decimal(15, 3) NULL DEFAULT NULL COMMENT '均价',
  `offset_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '偏移率，即涨跌百分比',
  `count` int(0) NULL DEFAULT NULL COMMENT '抓取次数',
  `source_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '源数据',
  `create_time` timestamp(3) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp(3) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE,
  INDEX `index_date_code`(`date`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_kzz_quotation
-- ----------------------------
DROP TABLE IF EXISTS `t_kzz_quotation`;
CREATE TABLE `t_kzz_quotation`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票编码',
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票名称',
  `date` date NULL DEFAULT NULL COMMENT '交易日期',
  `current` decimal(15, 3) NULL DEFAULT NULL COMMENT '当前价格',
  `init` decimal(15, 3) NULL DEFAULT NULL COMMENT '初始价，即昨天的收盘价',
  `open` decimal(15, 3) NULL DEFAULT NULL COMMENT '开盘价',
  `high` decimal(15, 3) NULL DEFAULT NULL COMMENT '最高价',
  `low` decimal(15, 3) NULL DEFAULT NULL COMMENT '最低价',
  `close` decimal(15, 3) NULL DEFAULT NULL COMMENT '关盘价',
  `volume` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易量',
  `volume_amt` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易额',
  `offset_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '偏移率，即涨跌百分比',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_limit_up_down
-- ----------------------------
DROP TABLE IF EXISTS `t_limit_up_down`;
CREATE TABLE `t_limit_up_down`  (
  `id` bigint(20) NOT NULL,
  `date` date NULL DEFAULT NULL,
  `up_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `down_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_proxy_ip
-- ----------------------------
DROP TABLE IF EXISTS `t_proxy_ip`;
CREATE TABLE `t_proxy_ip`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `ip` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'IP',
  `port` int(11) NULL DEFAULT NULL COMMENT '端口',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型;HTTP/HTTPS',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_quo_idp
-- ----------------------------
DROP TABLE IF EXISTS `t_quo_idp`;
CREATE TABLE `t_quo_idp`  (
  `id` bigint(0) NOT NULL COMMENT '主键',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票编码',
  `idp_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行业或概念名称',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型；0：行业；1：概念',
  `p_idy_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属行业',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_quotation
-- ----------------------------
DROP TABLE IF EXISTS `t_quotation`;
CREATE TABLE `t_quotation`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票编码',
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '股票名称',
  `date` date NULL DEFAULT NULL COMMENT '交易日期',
  `current` decimal(15, 3) NULL DEFAULT NULL COMMENT '当前价格',
  `init` decimal(15, 3) NULL DEFAULT NULL COMMENT '初始价，即昨天的收盘价',
  `open` decimal(15, 3) NULL DEFAULT NULL COMMENT '开盘价',
  `high` decimal(15, 3) NULL DEFAULT NULL COMMENT '最高价',
  `low` decimal(15, 3) NULL DEFAULT NULL COMMENT '最低价',
  `close` decimal(15, 3) NULL DEFAULT NULL COMMENT '关盘价',
  `volume` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易量',
  `volume_amt` decimal(15, 3) NULL DEFAULT NULL COMMENT '交易额',
  `offset_rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '偏移率，即涨跌百分比',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_quotation_detail
-- ----------------------------
DROP TABLE IF EXISTS `t_quotation_detail`;
CREATE TABLE `t_quotation_detail`  (
  `id` bigint(18) NOT NULL,
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `data1` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `data2` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `data3` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `data4` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_recommand
-- ----------------------------
DROP TABLE IF EXISTS `t_recommand`;
CREATE TABLE `t_recommand`  (
  `id` bigint(20) NOT NULL,
  `date` date NULL DEFAULT NULL,
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `data_list` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_up_down
-- ----------------------------
DROP TABLE IF EXISTS `t_up_down`;
CREATE TABLE `t_up_down`  (
  `id` bigint(20) NOT NULL,
  `date` date NULL DEFAULT NULL,
  `up_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `down_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_date`(`date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
