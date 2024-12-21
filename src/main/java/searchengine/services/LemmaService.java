package searchengine.services;

import searchengine.entity.LemmaEntity;

import java.util.Map;

public interface LemmaService {

    Map<String, Integer> collectLemmas(String text);

    Boolean isExist(String lemma);

    LemmaEntity findByLemma(String lemma);

    void insertOrUpdateLemma(Integer siteId, String lemma);

    LemmaEntity findByLemmaAndSite(String lemma, String site);

}
