package io.omnition.loadgenerator.model.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TagGenerator {

    private static final String defaultTagName = "tag-name-";
    private static final String valName = "val-name-";

    private Random rand = new Random();

    public String name = defaultTagName;
    public int numTags;
    public int numVals;

    public Map<String, Object> generateTags() {
        Map<String, Object> retVal = new HashMap<>();
        for (int genIndex = 0; genIndex < numTags; genIndex++) {
            retVal.put(name + genIndex, valName + rand.nextInt(numVals));
        }

        return retVal;
    }
}
