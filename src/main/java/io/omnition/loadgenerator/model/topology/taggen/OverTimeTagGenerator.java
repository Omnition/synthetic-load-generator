package io.omnition.loadgenerator.model.topology.taggen;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class OverTimeTagGenerator implements TagGenerator {

    private Random random = new Random();
    private AtomicReference<Double> currVal = new AtomicReference<>();
    private AtomicLong lastStep = new AtomicLong(Instant.now().getEpochSecond());

    public int step;
    public int period;
    public int amplitude;
    public int jitter;
    public int offset;
    public String name;

    @Override
    public Map<String, Object> generateTags() {
        HashMap<String, Object> tag = new HashMap<>();
        tag.put(name, genVal());
        return tag;
    }

    private Double genVal() {
        long t = Instant.now().getEpochSecond();
        // this might get updated more than once per step, and that is ok.
        if (t > lastStep.get() + step || currVal.get() == null) {
            currVal.set(amplitude * Math.sin(((double) t) / period * 2 * Math.PI) + offset);
            lastStep.set(t);
        }

        return currVal.get() + random.nextInt(jitter) - (jitter/2);
    }
}
