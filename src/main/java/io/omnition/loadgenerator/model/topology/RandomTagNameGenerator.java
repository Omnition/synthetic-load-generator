package io.omnition.loadgenerator.model.topology;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

class RandomTagNameGenerator {

    // Gives about 16M different options
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

    String getForIndex(int index) {
        return adjectives.get((adjectives.size()-1) % (index+1) )
                + "-" + natures.get((natures.size()-1) % (index+1))
                + "-" + pokemon.get((pokemon.size()-1) % (index+1));
    }
}
