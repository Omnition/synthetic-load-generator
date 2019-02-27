package io.omnition.loadgenerator.model.topology.taggen;

import io.omnition.loadgenerator.model.trace.KeyValue;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Map;
import java.util.Random;

public class MultiTagGenerator implements TagGenerator {

    private Random rand = new Random();
    private TagNameGenerator tagGen = new TagNameGenerator();

    public String tagName;
    public int valLength = 10;
    public int numTags = 0;
    public int numVals = 0;

    @Override
    public void addTagsTo(Map<String, KeyValue> tags) {
        if (numTags != 0 && tagName != null) {
            throw new IllegalArgumentException("numTags and tagName cannot both be set");
        }
        if (tagName == null) {
            for (int genIndex = 0; genIndex < numTags; genIndex++) {
                String val;
                val = RandomStringUtils.random(valLength, 0, 0, true, true, null, new Random(rand.nextInt(numVals)));
                String tagKeyName = tagGen.getForIndex(genIndex);
                tags.put(tagKeyName, KeyValue.ofStringType(tagKeyName, val));
            }
        } else {
            tags.put(tagName, KeyValue.ofStringType(tagName, RandomStringUtils.random(valLength, 0, 0, true, true, null, new Random(rand.nextInt(numVals)))));
        }
    }

}
