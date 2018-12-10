package io.omnition.loadgenerator.model.trace;

public class KeyValue {
    public static final String STRING_VALUE_TYPE = "string";
    public static final String LONG_VALUE_TYPE = "long";
    public static final String BOOLEAN_VALUE_TYPE = "bool";
    public String key;
    public String valueType;

    // TODO there are more types: double, binary, not needed at the moment
    public String valueString;
    public Boolean valueBool;
    public Long valueLong;

    public static KeyValue ofStringType(String key, String value) {
        KeyValue kv = new KeyValue();
        kv.key = key;
        kv.valueType = STRING_VALUE_TYPE;
        kv.valueString = value;
        return kv;
    }

    public static KeyValue ofLongType(String key, Long value) {
        KeyValue kv = new KeyValue();
        kv.key = key;
        kv.valueType = LONG_VALUE_TYPE;
        kv.valueLong = value;
        return kv;
    }

    public static KeyValue ofBooleanType(String key, Boolean value) {
        KeyValue kv = new KeyValue();
        kv.key = key;
        kv.valueType = BOOLEAN_VALUE_TYPE;
        kv.valueBool = value;
        return kv;
    }
}
