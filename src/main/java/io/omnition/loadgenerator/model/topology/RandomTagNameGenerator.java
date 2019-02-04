package io.omnition.loadgenerator.model.topology;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

class RandomTagNameGenerator {

    // Gives about 16M different options, so .02% chance to collide once on 100 tags
    // and ~2% chance to collide once on 1000 tags
    private List<String> pokemon;
    private List<String> natures;
    private List<String> adjectives;

    RandomTagNameGenerator() {
        pokemon = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/pokemon.txt")))
            .lines()
            .collect(Collectors.toList());
        natures = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/natures.txt")))
            .lines()
            .collect(Collectors.toList());
        adjectives = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/adjectives.txt")))
            .lines()
            .collect(Collectors.toList());
    }

    String random(Random rand) {
        return adjectives.get(rand.nextInt(adjectives.size()))
                + "-" + natures.get(rand.nextInt(natures.size()))
                + "-" + pokemon.get(rand.nextInt(pokemon.size()));
    }
}
