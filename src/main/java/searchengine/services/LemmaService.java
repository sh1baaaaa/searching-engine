package searchengine.services;

import searchengine.entity.LemmaEntity;

import java.util.Map;

public interface LemmaService {

    Map<String, Integer> collectLemmas(String text);

    String removeHTMLTags(String text);

    Boolean isExist(String lemma);

    LemmaEntity findByLemma(String lemma);

    void insertOrUpdateLemma(Integer siteId, String lemma);

    void save(LemmaEntity lemmaEntity);

    LemmaEntity findByLemmaAndSite(String lemma, String site);

}
