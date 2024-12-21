package searchengine.services.impl;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.IndexEntity;
import searchengine.processor.LemmaFinder;
import searchengine.services.IndexService;
import searchengine.services.SearchingService;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SearchingServiceImpl implements SearchingService {


    private final IndexService indexService;

    private final LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());

    @Autowired
    public SearchingServiceImpl(IndexService indexService) throws IOException {
        this.indexService = indexService;
    }


    @Override
    public List<IndexEntity> searchLemmas(String query, String offset, String limit) {
        Map<String, Integer> queryLemmas = lemmaFinder.collectLemmas(query);

        List<String> actualLemmas = queryLemmas.keySet()
                .stream()
                .filter(lemma -> indexService.findLemmaCount(lemma) < 27)
                .toList();
        actualLemmas.sort(Comparator.comparing(queryLemmas::get));

        AtomicReference<List<IndexEntity>> foundLemmas
                = new AtomicReference<>(indexService.findByLemma(actualLemmas.get(0)));

        actualLemmas
                .forEach(lemma -> foundLemmas.set(foundLemmas
                        .get()
                        .stream()
                        .filter(index -> indexService.findByLemma(lemma).contains(index))
                        .toList()));
        return foundLemmas.get();
    }

    @Override
    public List<IndexEntity> searchLemmasByPage(String query, String offset, String limit, String site) {


        Map<String, Integer> queryLemmas = lemmaFinder.collectLemmas(query);

        List<String> actualLemmas = queryLemmas.keySet()
                .stream()
                .filter(lemma -> indexService.findLemmaCount(lemma, site) < 27)
                .toList();
        actualLemmas.sort(Comparator.comparing(queryLemmas::get));

        AtomicReference<List<IndexEntity>> foundLemmas
                = new AtomicReference<>(indexService.findByLemmaAndSite(actualLemmas.get(0), site));

        actualLemmas
                .forEach(lemma -> foundLemmas.set(foundLemmas
                        .get()
                        .stream()
                        .filter(index -> indexService.findByLemmaAndSite(lemma, site).contains(index))
                        .toList()));
        return foundLemmas.get();
    }

    private void calculateRelevance(List<IndexEntity> pages) {
        Integer totalLemmaCount = pages.stream()
                .map(index -> index.getLemma().getFrequency())
                .reduce(Integer::sum)
                .orElseThrow(() -> new RuntimeException("Empty lemma list"));



    }

}
