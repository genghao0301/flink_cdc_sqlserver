package com.vk.cdc;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * flink cdc sqlserver->mysql
 * cdc sql  只能实现单表 无法实现多表
 */
public class FlinkSSCDC {
    public static void main(String[] args) {
        //TODO 1.基本环境准备
        Configuration conn = new Configuration();
        //设置端口访问的范围
        conn.setString(RestOptions.BIND_PORT, "8083-8089");
        //1.1 流处理环境
//        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conn);
        //1.2 表执行环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        //1.3 设置并行度
        env.setParallelism(3);
        //设置检查点
        env.enableCheckpointing(5000L);
        //建立第一张表
        tableEnv.executeSql("CREATE TABLE biz_house_info (\n" +
                "   SITE_CODE VARCHAR,\n" +
                "   HOUSE_CODE VARCHAR,\n" +
                "   HOUSE_NAME VARCHAR,\n" +
                "   TEMPERATURE VARCHAR,\n" +
                "   TEMPERATE_ZONE VARCHAR,\n" +
                "   PRIMARY KEY (SITE_CODE) NOT ENFORCED\n" +
                " ) WITH (\n" +
                "    'connector' = 'sqlserver-cdc',\n" +
                "    'hostname' = 'prod01.public.b8f689b2a39a.database.chinacloudapi.cn',\n" +
                "    'port' = '3342',\n" +
                "    'username' = 'dmp_reader',\n" +
                "    'password' = 'dmp_reader_67*2#_',\n" +
                "    'database-name' = 'DMP_DB',\n" +
                "    'schema-name' = 'dbo',\n" +
                "    'table-name' = 'BIZ_HOUSE_INFO'\n" +
//                "    'debezium.snapshot.mode' = 'initial_only'" +
                ")");
//        tableEnv.executeSql("select * from biz_house_info").print();
        //建立第二张表
        tableEnv.executeSql("CREATE TABLE warehouse_code_mapping (\n" +
                "   WAREHOUSE_CODE VARCHAR,\n" +
                "   SITE_COS_NAME VARCHAR,\n" +
                "   SITE_CODE VARCHAR,\n" +
                "   PRIMARY KEY (SITE_CODE) NOT ENFORCED\n" +
                " ) WITH (\n" +
                "    'connector' = 'sqlserver-cdc',\n" +
                "    'hostname' = 'prod01.public.b8f689b2a39a.database.chinacloudapi.cn',\n" +
                "    'port' = '3342',\n" +
                "    'username' = 'dmp_reader',\n" +
                "    'password' = 'dmp_reader_67*2#_',\n" +
                "    'database-name' = 'DMP_DB',\n" +
                "    'schema-name' = 'dbo',\n" +
                "    'table-name' = 'WAREHOUSE_CODE_MAPPING'\n" +
                ")");
        //两张表join sink到mysql
        tableEnv.executeSql("create table tmp(\n" +
                "  wh_code VARCHAR,\n" +
                "  wms_wh_code VARCHAR,\n" +
                "  wh_name VARCHAR,\n" +
                "  wh_area_code VARCHAR,\n" +
                "  wh_area_name VARCHAR ,\n" +
                "  temperature_zone VARCHAR ,\n" +
                "  area_temperature_info VARCHAR ,\n" +
                "   PRIMARY KEY (wh_code,wms_wh_code,wh_area_code) NOT ENFORCED" +
                "  ) WITH (\n" +
                " 'connector' = 'jdbc',\n" +
                "  'driver'='com.mysql.cj.jdbc.Driver',\n" +
                "  'username' = 'root',  \n" +
                "  'password' = 'root',  \n" +
                "  'table-name' = 'dmp_cdc',\n" +
                "  'url' = 'jdbc:mysql://localhost:3306/test?useSSL=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai'\n" +
                "  )");
        //插入数据
        tableEnv.executeSql("insert into tmp select  \n" +
                "bhi.SITE_CODE as wh_code,\n" +
                "wcm.WAREHOUSE_CODE as wms_wh_code,\n" +
                "wcm.SITE_COS_NAME as wh_name, \n" +
                "bhi.HOUSE_CODE as wh_area_code,\n" +
                "bhi.HOUSE_NAME as wh_area_name,\n" +
                "bhi.TEMPERATE_ZONE as temperature_zone,\n" +
                "bhi.TEMPERATURE as area_temperature_info\n" +
                "from biz_house_info bhi\n" +
                "left join warehouse_code_mapping wcm on bhi.SITE_CODE = wcm.SITE_CODE");
    }
}
