package com.appsoil.solvle.config;

import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@Log4j2
@EnableAsync
public class SolvleConfig {

    @Bean
    public Map<DictionaryType, Dictionary> allDictionaries() {
        return Arrays.stream(DictionaryType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> readResourceToDictionary(type.getPath())
                ));
    }

    private Dictionary readResourceToDictionary(String path) {
        InputStream is = this.getClass().getResourceAsStream(path);
        Map<Integer, Set<Word>> dict = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int count = 0;

        try {
            String word = br.readLine();
            while (word != null) {
                if (!dict.containsKey(word.length())) {
                    dict.put(word.length(), new TreeSet<>()); //alphabetized
                }
                dict.get(word.length()).add(new Word(word));
                word = br.readLine();
                if (count++ % 10000 == 0) {
                    log.info(count - 1 + " read...");
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error parsing dictionary", ioe);
        }

        log.info("Read " + count + " words from " + path);
        return new Dictionary(dict);
    }
}
