package searchengine.services.impl;


import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.LemmaEntity;
import searchengine.features.LemmaFinder;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.Map;


@Service
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    @Autowired
    public LemmaServiceImpl(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public Map<String, Integer> collectLemmas(String text) {
        try {
            LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
            return lemmaFinder.collectLemmas(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Boolean isExist(String lemma) {
        return lemmaRepository.existsByLemma(lemma);
    }

    @Override
    public LemmaEntity findByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    @Override
    public void insertOrUpdateLemma(Integer siteId, String lemma) {
        lemmaRepository.insertOrUpdateLemma(siteId, lemma);
    }

    @Override
    public LemmaEntity findByLemmaAndSite(String lemma, String site) {
        return lemmaRepository.findByLemmaAndSiteUrl(lemma, site);
    }

}
