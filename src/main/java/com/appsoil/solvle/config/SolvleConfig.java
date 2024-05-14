package com.appsoil.solvle.config;

import com.appsoil.solvle.data.Dictionary;
import com.appsoil.solvle.data.Word;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Configuration
@Log4j2
public class SolvleConfig {

    @Bean(name = "bigDictionary")
    Dictionary getBigDictionary() {
        return readResourceToDictionary("/dict2/enable1.txt");
    }

    @Bean(name = "simpleDictionary")
    Dictionary getSimpleDictionary() {
        return readResourceToDictionary("/dict2/simple-solutions.txt");
    }

    @Bean(name = "extendedDictionary")
    Dictionary getExtendedDictionary() {
        return readResourceToDictionary("/dict2/extended-solutions.txt");
    }

    @Bean(name = "reducedDictionary")
    Dictionary getReducedDictionary() {
        return readResourceToDictionary("/dict2/remaining-solutions.txt");
    }

    @Bean(name = "icelandicDictionary")
    Dictionary getIcelandicDictionary() {
        return readResourceToDictionary("/dict2/iceland.txt");
    }

    @Bean(name = "icelandicCommonDictionary")
    Dictionary getIcelandicCommonDictionary() {
        return readResourceToDictionary("/dict2/iceland-common.txt");
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
