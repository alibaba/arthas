package com.taobao.arthas.grpc.server.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/17 下午10:02
 */

import com.google.protobuf.WireFormat;

/**
 * @author: FengYe protobuf字段类型映射
 * @date: 2024/7/17 下午10:02
 * @description: ProtoBufFieldType
 */
public enum ProtobufFieldTypeEnum {
    /**
     * types defined in .proto file.
     */
    DOUBLE  ("Double", "double"     , "WIRETYPE_FIXED64", ".doubleValue()", WireFormat.FieldType.DOUBLE         ,"0d"),
    FLOAT   ("Float", "float", "WIRETYPE_FIXED32"    ,    ".floatValue()",  WireFormat.FieldType.FLOAT ,"0f"),
    INT64   ("Long", "int64"      , "WIRETYPE_VARINT"    ,    ".longValue()",  WireFormat.FieldType.INT64  ,"0L"    ),
    UINT64  ("Long", "uInt64"      , "WIRETYPE_VARINT"   ,    ".longValue()",  WireFormat.FieldType.UINT64    ,"0L"  ),
    INT32   ("Integer", "int32"   , "WIRETYPE_VARINT"    ,    ".intValue()" ,  WireFormat.FieldType.INT32   ,"0" ),
    FIXED64 ("Long", "fixed64"       , "WIRETYPE_FIXED64"    ,    ".longValue()" ,  WireFormat.FieldType.FIXED64   ,"0L" ),
    FIXED32 ("Integer", "fixed32"        , "WIRETYPE_FIXED32"   ,    ".intValue()",  WireFormat.FieldType.FIXED32  ,"0"  ),
    BOOL    ("Boolean", "bool"     , "WIRETYPE_VARINT"      , ".booleanValue()",  WireFormat.FieldType.BOOL   ,"false" ),
    STRING  ("String", "string"     , "WIRETYPE_LENGTH_DELIMITED", "",  WireFormat.FieldType.STRING ,"\"\""),
    BYTES   ("byte[]", "bytes", "WIRETYPE_LENGTH_DELIMITED", "",  WireFormat.FieldType.BYTES,  "new byte[0]"),
    UINT32  ("Integer", "uInt32"        , "WIRETYPE_VARINT"    ,    ".intValue()" ,  WireFormat.FieldType.UINT32 ,"0"    ),
    SFIXED32("Integer", "sFixed32"       , "WIRETYPE_FIXED32"   ,    ".intValue()" ,  WireFormat.FieldType.SFIXED32  ,"0"  ),
    SFIXED64("Long", "sFixed64"      , "WIRETYPE_FIXED64"   ,    ".longValue()" ,  WireFormat.FieldType.SFIXED64    ,"0L" ),
    SINT32  ("Integer", "sInt32"        , "WIRETYPE_VARINT"     ,    ".intValue()" ,  WireFormat.FieldType.SINT32  ,"0"  ),
    SINT64  ("Long", "sInt64"       , "WIRETYPE_VARINT"    ,    ".longValue()" ,  WireFormat.FieldType.SINT64   ,"0L"  ),
    OBJECT  ("Object", "object"       , "WIRETYPE_LENGTH_DELIMITED"    ,    ""    ,  WireFormat.FieldType.MESSAGE ,null ),
    ENUM ("Enum", "enum"       , "WIRETYPE_VARINT"    ,    ".ordinal()"    ,  WireFormat.FieldType.ENUM , null ),
    MAP ("Map", "map"       , "WIRETYPE_VARINT"    ,    ""    ,  WireFormat.FieldType.MESSAGE , null ),
    DATE ("Date", "int64"       , "WIRETYPE_VARINT"    ,    ".getTime()"    ,  WireFormat.FieldType.INT64 , null ),
    BIGDECIMAL("java.math.BigDecimal", "string", "WIRETYPE_LENGTH_DELIMITED", ".toString()",
            WireFormat.FieldType.STRING, null),
    BIGINTEGER("java.math.BigInteger", "string", "WIRETYPE_LENGTH_DELIMITED", ".toString()",
            WireFormat.FieldType.STRING, null),
    DEFAULT("", ""       , ""    ,    ""    ,  WireFormat.FieldType.MESSAGE , null );

    /**
     * java original type
     */
    private final String javaType;
    /**
     * protobuf type
     */
    private final String type;
    /**
     * protobuf wire format type
     */
    private final String wireFormat;

    /**
     * to primitive type
     */
    private final String toPrimitiveType;

    /**
     * internal field type
     */
    private WireFormat.FieldType internalFieldType;

    /**
     * default value
     */
    private String defaultValue;

    /**
     * get the defaultValue
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * set defaultValue value to defaultValue
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * get the internalFieldType
     * @return the internalFieldType
     */
    public WireFormat.FieldType getInternalFieldType() {
        return internalFieldType;
    }

    /**
     * set internalFieldType value to internalFieldType
     * @param internalFieldType the internalFieldType to set
     */
    public void setInternalFieldType(WireFormat.FieldType internalFieldType) {
        this.internalFieldType = internalFieldType;
    }

    /**
     * get primitive type in string
     * @return primitive type in string
     */
    public String getToPrimitiveType() {
        return toPrimitiveType;
    }

    /**
     * get protobuf wire format type
     * @return protobuf wire format type
     */
    public String getWireFormat() {
        return wireFormat;
    }

    /**
     * get protobuf type
     * @return protobuf type
     */
    public String getType() {
        return type;
    }

    /**
     * get java original type
     * @return java original type
     */
    public String getJavaType() {
        if (this == ProtobufFieldTypeEnum.ENUM) {
            return Enum.class.getName();
        }
        return javaType;
    }

    /**
     * Constructor method
     *
     * @param javaType java original type
     * @param type protobuf type
     * @param wireFormat protobuf wire format type
     */
    ProtobufFieldTypeEnum(String javaType, String type, String wireFormat,
                          String toPrimitiveType, WireFormat.FieldType internalFieldType, String defaultValue) {
        this.javaType = javaType;
        this.type = type;
        this.wireFormat = wireFormat;
        this.toPrimitiveType = toPrimitiveType;
        this.internalFieldType = internalFieldType;
        this.defaultValue = defaultValue;
    }

    /**
     * Checks if is primitive.
     *
     * @return true, if is primitive
     */
    public boolean isPrimitive() {
        if (this == INT32 || this == DOUBLE || this == FIXED32
                || this == FIXED64 || this == FLOAT
                || this == INT64 || this == SFIXED32
                || this == SFIXED64 || this == SINT32
                || this == SINT64 || this == BOOL || this == ProtobufFieldTypeEnum.UINT32 || this == ProtobufFieldTypeEnum.UINT64) {
            return true;
        }

        return false;
    }

    /**
     * Checks if is enum.
     *
     * @return true, if is enum
     */
    public boolean isEnum() {
        return this == ENUM;
    }
}
