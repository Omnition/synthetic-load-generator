package io.omnition.loadgenerator.model.trace;

import java.util.UUID;

public class Reference {
    public enum RefType {
        CHILD_OF, FOLLOWS_FROM
    }

    public RefType refType;
    public UUID fromSpanId;
    public UUID toSpanId;

    public Reference(RefType refType, UUID fromSpanId, UUID toSpanId) {
        this.refType = refType;
        this.fromSpanId = fromSpanId;
        this.toSpanId = toSpanId;
    }
}
