package com.vk.cdc;

import com.ververica.cdc.debezium.DebeziumSourceFunction;
import com.vk.schema.JsonDebeziumDeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class FlinkSSCDC6 {
    public static void main(String[] args) throws Exception {
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
        DebeziumSourceFunction build = SqlServerSource.<String>builder()
                .hostname("prod01.public.b8f689b2a39a.database.chinacloudapi.cn")
                .port(3342)
                .database("DMP_DB")
                .tableList("dbo.BIZ_HOUSE_INFO")
                .username("dmp_reader")
                .password("dmp_reader_67*2#_")
                .deserializer(new JsonDebeziumDeserializationSchema())
                .build();

        env.addSource(build).print().setParallelism(1);
        env.execute("1");
    }
}
