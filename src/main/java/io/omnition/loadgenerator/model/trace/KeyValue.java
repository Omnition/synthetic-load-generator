package io.omnition.loadgenerator.model.trace;

import java.util.List;
import java.util.Random;

public class KeyValue {
    private static final Random random = new Random();

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

    public static KeyValue FromObject(String key, Object val) {
        if (val instanceof String) {
            return KeyValue.ofStringType(key, (String) val);
        }
        if (val instanceof Double) {
            return KeyValue.ofLongType(key, ((Double) val).longValue());
        }
        if (val instanceof Boolean) {
            return KeyValue.ofBooleanType(key, (Boolean) val);
        }
        if (val instanceof List) {
            return FromObject(key, ((List) val).get(random.nextInt(((List) val).size())));
        }
        return null;
    }
}
