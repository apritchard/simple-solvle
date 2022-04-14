package com.appsoil.solvle.config;

import com.appsoil.solvle.wordler.Dictionary;
import com.appsoil.solvle.wordler.Word;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@Log4j2
public class SolvleConfig {

    @Bean(name="defaultDictionary")
    Dictionary getDictionary() {
        return readResourceToDictionary("/dict2/wlist_match8.txt");
    }

    @Bean(name="bigDictionary")
    Dictionary getBigDictionary() {
        return readResourceToDictionary("/dict2/enable1.txt");
    }

    @Bean(name="hugeDictionary")
    Dictionary getHugeDictionary() {
        return readResourceToDictionary("/dict2/big-dict-energy.txt");
    }

    @Bean(name="wordleDictionary")
    Dictionary getWordleDictionary() {
        return readResourceToDictionary("/dict2/wordle-solutions.txt");
    }

    private Dictionary readResourceToDictionary(String path) {
        log.info("Current path: " + System.getProperty("user.dir"));
        InputStream is = this.getClass().getResourceAsStream(path);
        Map<Integer, Set<Word>> dict = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int count = 0;

        try {
            String word = br.readLine();
            while (word != null) {
                if(!dict.containsKey(word.length())) {
                    dict.put(word.length(), new HashSet<>());
                }
                dict.get(word.length()).add(new Word(word));
                word = br.readLine();
                if (count++ % 10000 == 0) {
                    log.info(count - 1 + " read...");
                }
            }
        } catch (IOException ioe) {
            log.info("Error parsing dictionary");
            throw new RuntimeException(ioe);
        }

        log.info("Read in " + count + " words");
        return new Dictionary(dict);
    }
}
