package com.vk.cdc;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * flink sqlserver cdc -> mysql
 * 下一步实现sink到sqlserver
 */
public class FlinkSSCDC6 {
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
        tableEnv.executeSql("CREATE TABLE t1 (\n" +
                "   ID INT,\n" +
                "   USER_NAME VARCHAR,\n" +
                "   SEX VARCHAR,\n" +
                "   PRIMARY KEY (ID) NOT ENFORCED\n" +
                " ) WITH (\n" +
                "    'connector' = 'sqlserver-cdc',\n" +
                "    'hostname' = 'noprod01.public.56fbe2297a1d.database.chinacloudapi.cn',\n" +
                "    'port' = '3342',\n" +
                "    'username' = 'iot_test_admin',\n" +
                "    'password' = '123456',\n" +
                "    'database-name' = 'IOT_DM_DB_TEST',\n" +
                "    'schema-name' = 'dbo',\n" +
                "    'table-name' = 'test1'\n" +
                ")");
//        tableEnv.executeSql("select * from biz_house_info").print();
        //browse subversion repository
        //建立第二张表
        tableEnv.executeSql("CREATE TABLE t2 (\n" +
                "   ID INT,\n" +
                "   PHONE VARCHAR,\n" +
                "   PRIMARY KEY (ID) NOT ENFORCED\n" +
                " ) WITH (\n" +
                "    'connector' = 'sqlserver-cdc',\n" +
                "    'hostname' = 'noprod01.public.56fbe2297a1d.database.chinacloudapi.cn',\n" +
                "    'port' = '3342',\n" +
                "    'username' = 'iot_test_admin',\n" +
                "    'password' = '123456',\n" +
                "    'database-name' = 'IOT_DM_DB_TEST',\n" +
                "    'schema-name' = 'dbo',\n" +
                "    'table-name' = 'test2'\n" +
                ")");
        //两张表join sink到mysql
        tableEnv.executeSql("create table tmp(\n" +
                "  id INT,\n" +
                "  user_name VARCHAR,\n" +
                "  sex VARCHAR,\n" +
                "  phone VARCHAR,\n" +
//                "   PRIMARY KEY (wh_code,wms_wh_code,wh_area_code) NOT ENFORCED" +
                "   PRIMARY KEY (id) NOT ENFORCED" +
                "  ) WITH (\n" +
                " 'connector' = 'jdbc',\n" +
                "  'driver'='com.mysql.cj.jdbc.Driver',\n" +
                "  'username' = 'root',  \n" +
                "  'password' = 'root',  \n" +
                "  'table-name' = 'sqlserver_test',\n" +
                "  'url' = 'jdbc:mysql://localhost:3306/test?useSSL=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai'\n" +
                "  )");
        //插入数据
        tableEnv.executeSql("insert into tmp\n" +
                "select\n" +
                "t1.ID as id,\n" +
                "USER_NAME as user_name,\n" +
                "SEX as sex,\n" +
                "PHONE as phone\n" +
                "from\n" +
                "t1\n" +
                "left join t2 on t1.ID = t2.ID");
//        tableEnv.executeSql("insert into tmp \n" +
//                " select *,111 from test1");
    }
}
