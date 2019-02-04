package io.omnition.loadgenerator.model.topology;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

// Can generate about 16M different unique combinations
class TagNameGenerator {

    private static List<String> pokemon = new BufferedReader(
        new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/pokemon.txt"))
    ).lines().collect(Collectors.toList());

    private static List<String> natures = new BufferedReader(
        new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/natures.txt"))
    ).lines().collect(Collectors.toList());

    private static List<String> adjectives = new BufferedReader(
        new InputStreamReader(TagNameGenerator.class.getResourceAsStream("/adjectives.txt"))
    ).lines().collect(Collectors.toList());

    String getForIndex(int index) {
        return adjectives.get((adjectives.size()-1) % (index+1) )
                + "-" + natures.get((natures.size()-1) % (index+1))
                + "-" + pokemon.get((pokemon.size()-1) % (index+1));
    }
}
