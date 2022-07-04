//package com.vk.func;
//
//import org.apache.flink.configuration.ConfigOption;
//import org.apache.flink.configuration.Configuration;
//import org.apache.flink.configuration.ReadableConfig;
//import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
//import org.apache.flink.streaming.api.functions.sink.SinkFunction;
//import org.apache.flink.table.connector.ChangelogMode;
//import org.apache.flink.table.connector.sink.DynamicTableSink;
//import org.apache.flink.table.connector.sink.SinkFunctionProvider;
//import org.apache.flink.table.data.RowData;
//import org.apache.flink.table.factories.DynamicTableFactory;
//import org.apache.flink.table.factories.DynamicTableSinkFactory;
//import org.apache.flink.table.factories.FactoryUtil;
//import org.apache.flink.table.types.DataType;
//import org.apache.flink.table.types.logical.RowType;
//import org.apache.flink.types.Row;
//import org.apache.flink.types.RowKind;
//
//import java.util.*;
//
//import static org.apache.flink.configuration.ConfigOptions.key;
//
//public class SqlServerTableSinkFactory implements DynamicTableSinkFactory {
//    //连接器名称
//    public static final String IDENTIFIER = "sqlserver";
//    //定义host
//    public static final ConfigOption<String> HOST_NAME = key("hostname")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer host,");
//    //定义port
//    public static final ConfigOption<String> PORT = key("hostPort")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer port,");
//
//    public static final ConfigOption<String> PASSWORD = key("password")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer password");
//
//    public static final ConfigOption<String> USERNAME = key("username")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer username");
//
//    public static final ConfigOption<String> DATABASE_NAME = key("database_name")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer database_name");
//
//    public static final ConfigOption<String> SCHEMA_NAME = key("schema-name")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer schema-name ");
//
//    public static final ConfigOption<String> TABLE_NAME = key("table-name")
//            .stringType()
//            .noDefaultValue()
//            .withDescription("sqlServer table-name ");
//
//
//    @Override
//    // 当 connector 与 IDENTIFIER 一致才会找到 SqlServerSink 通过
//    public String factoryIdentifier() {
//        return IDENTIFIER;
//    }
//
//    @Override
//    public Set<ConfigOption<?>> requiredOptions() {
//        return new HashSet<>();
//    }
//
//    @Override
//    //我们自己定义的所有选项 (with 后面的 ) 都会在这里获取
//    public Set<ConfigOption<?>> optionalOptions() {
//        Set<ConfigOption<?>> options = new HashSet<>();
//        options.add(HOST_NAME);
//        options.add(PORT);
//        options.add(PASSWORD);
//        options.add(USERNAME);
//        options.add(DATABASE_NAME);
//        options.add(SCHEMA_NAME);
//        options.add(TABLE_NAME);
//        return options;
//    }
//
//    @Override
//    public DynamicTableSink createDynamicTableSink(DynamicTableFactory.Context context) {
//        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
//        helper.validate();
//        ReadableConfig options = helper.getOptions();
//        return new SqlServerTableSinkFactory.SqlServerSink(
//                context.getCatalogTable().getSchema().toPhysicalRowDataType(), options);
//    }
//
//
//    private static class SqlServerSink implements DynamicTableSink {
//
//        private final DataType type;
//        private final ReadableConfig options;
//
//        private SqlServerSink(DataType type, ReadableConfig options) {
//            this.type = type;
//            this.options = options;
//        }
//
//        @Override
//        //ChangelogMode
//        public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
//            return requestedMode;
//        }
//
//        @Override
//        //具体运行的地方，真正开始调用用户自己定义的 streaming sink ，建立 sql 与 streaming 的联系
//        public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
//            DataStructureConverter converter = context.createDataStructureConverter(type);
//            return SinkFunctionProvider.of(new SqlServerTableSinkFactory.RowDataPrintFunction(converter, options, type));
//        }
//
//        @Override
//        // sink 可以不用实现，主要用来 source 的谓词下推
//        public DynamicTableSink copy() {
//            return new SqlServerTableSinkFactory.SqlServerSink(type, options);
//        }
//
//        @Override
//        public String asSummaryString() {
//            return "=sqlserver";
//        }
//    }
//
//    /**
//     同 flink streaming 自定义 sink ，只不过我们这次处理的是 RowData，不细说
//     */
//    private static class RowDataPrintFunction extends RichSinkFunction<RowData> {
//
//        private static final long serialVersionUID = 1L;
//
//        private final DynamicTableSink.DataStructureConverter converter;
//        private final ReadableConfig options;
//        private final DataType type;
//        private RowType logicalType;
//        private HashMap<String, Integer> fields;
////        private JedisCluster jedisCluster;
//
//        private RowDataPrintFunction(
//                DynamicTableSink.DataStructureConverter converter, ReadableConfig options, DataType type) {
//            this.converter = converter;
//            this.options = options;
//            this.type = type;
//        }
//
//        @Override
//        public void open(Configuration parameters) throws Exception {
//            super.open(parameters);
//            logicalType = (RowType) type.getLogicalType();
//            fields = new HashMap<>();
//            List<RowType.RowField> rowFields = logicalType.getFields();
//            int size = rowFields.size();
//            for (int i = 0; i < size; i++) {
//                fields.put(rowFields.get(i).getName(), i);
//            }
//
//            jedisCluster = RedisUtil.getJedisCluster(options.get(HOST_PORT));
//        }
//
//        @Override
//        public void close() throws Exception {
//            RedisUtil.closeConn(jedisCluster);
//        }
//
//        @Override
//        /*
//        2> +I(1,30017323,1101)
//        2> -U(1,30017323,1101)
//        2> +U(2,30017323,1101)
//        2> -U(2,30017323,1101)
//        2> +U(3,30017323,1101)
//        2> -U(3,30017323,1101)
//        2> +U(4,30017323,1101)
//        3> -U(3,980897,3208)
//        3> +U(4,980897,3208)
//         */
//        public void invoke(RowData rowData, Context context) {
//            //注释开始
//            RowKind rowKind = rowData.getRowKind();
//            Row data = (Row) converter.toExternal(rowData);
//            if (rowKind.equals(RowKind.UPDATE_AFTER) || rowKind.equals(RowKind.INSERT)) {
//
//                String keyTemplate = options.get(KEY_TEMPLATE);
//                if (Objects.isNull(keyTemplate) || keyTemplate.trim().length() == 0) {
//                    throw new NullPointerException(" keyTemplate is null or keyTemplate is empty");
//                }
//
//                if (keyTemplate.contains("${")) {
//                    String[] split = keyTemplate.split("\\$\\{");
//                    keyTemplate = "";
//                    for (String s : split) {
//                        if (s.contains("}")) {
//                            String filedName = s.substring(0, s.length() - 1);
//                            int index = fields.get(filedName);
//                            keyTemplate = keyTemplate + data.getField(index).toString();
//                        } else {
//                            keyTemplate = keyTemplate + s;
//                        }
//                    }
//                }
//
//                String keyType = options.get(KEY_TYPE);
//                String valueNames = options.get(VALUE_NAMES);
//                // type=hash must need fieldTemplate
//                if ("hash".equalsIgnoreCase(keyType)) {
//                    String fieldTemplate = options.get(FIELD_TEMPLATE);
//                    if (fieldTemplate.contains("${")) {
//                        String[] split = fieldTemplate.split("\\$\\{");
//                        fieldTemplate = "";
//                        for (String s : split) {
//                            if (s.contains("}")) {
//                                String fieldName = s.substring(0, s.length() - 1);
//                                int index = fields.get(fieldName);
//                                fieldTemplate = fieldTemplate + data.getField(index).toString();
//                            } else {
//                                fieldTemplate = fieldTemplate + s;
//                            }
//                        }
//                    }
//
//                    //fieldName = fieldTemplate-valueName
//                    if (valueNames.contains(",")) {
//                        HashMap<String, String> map = new HashMap<>();
//                        String[] fieldNames = valueNames.split(",");
//                        for (String fieldName : fieldNames) {
//                            String value = data.getField(fields.get(fieldName)).toString();
//                            map.put(fieldTemplate + "_" + fieldName, value);
//                        }
//                        jedisCluster.hset(keyTemplate, map);
//                    } else {
//                        jedisCluster.hset(keyTemplate, fieldTemplate + "_" + valueNames, data.getField(fields.get(valueNames)).toString());
//                    }
//
//                } else if ("set".equalsIgnoreCase(keyType)) {
//                    jedisCluster.set(keyTemplate, data.getField(fields.get(valueNames)).toString());
//
//                } else if ("sadd".equalsIgnoreCase(keyType)) {
//                    jedisCluster.sadd(keyTemplate, data.getField(fields.get(valueNames)).toString());
//                } else if ("zadd".equalsIgnoreCase(keyType)) {
//                    jedisCluster.sadd(keyTemplate, data.getField(fields.get(valueNames)).toString());
//                } else {
//                    throw new IllegalArgumentException(" not find this keyType:" + keyType);
//                }
//
//                if (Objects.nonNull(options.get(EXPIRE_TIME))) {
//                    jedisCluster.expire(keyTemplate, options.get(EXPIRE_TIME));
//                }
//            }
//
//            //注释结束
//        }
//
//    }
//}
