package com.appsoil.solvle.experimental;

import com.appsoil.solvle.config.DictionaryType;
import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class SolvescapeServiceTest {

    @Test
    void contains_respectsMissingLettersAndDuplicateCounts() {
        SolvescapeService service = new SolvescapeService(Map.of(DictionaryType.BIG, dictionary()));

        Assertions.assertTrue(service.contains(new Word("stone"), new Word("tone")));
        Assertions.assertFalse(service.contains(new Word("stone"), new Word("snoot")));
        Assertions.assertFalse(service.contains(new Word("stone"), new Word("table")));
    }

    @Test
    void getAnagrams_returnsContainedWordsByDescendingLength() {
        SolvescapeService service = new SolvescapeService(Map.of(DictionaryType.BIG, dictionary()));

        Map<Integer, List<String>> anagrams = service.getAnagrams("stone");

        Assertions.assertEquals(List.of(5, 4, 3), List.copyOf(anagrams.keySet()));
        Assertions.assertEquals(List.of("notes", "stone", "tones"), anagrams.get(5));
        Assertions.assertEquals(List.of("note", "tone"), anagrams.get(4));
        Assertions.assertEquals(List.of("ten"), anagrams.get(3));
    }

    private static Dictionary dictionary() {
        return new Dictionary(Map.of(
                3, Set.of(new Word("ten"), new Word("too")),
                4, Set.of(new Word("tone"), new Word("note"), new Word("soon")),
                5, Set.of(new Word("stone"), new Word("tones"), new Word("notes"), new Word("snoot"))
        ));
    }
}
