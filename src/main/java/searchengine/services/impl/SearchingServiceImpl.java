package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.SearchingResponseDTO;
import searchengine.dto.SearchingResponseDataDTO;
import searchengine.entity.IndexEntity;
import searchengine.features.LemmaFinder;
import searchengine.services.IndexService;
import searchengine.services.SearchingService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchingServiceImpl implements SearchingService {


    private final IndexService indexService;

    private final LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());

    @Autowired
    public SearchingServiceImpl(IndexService indexService) throws IOException {
        this.indexService = indexService;
    }


    @Override
    public SearchingResponseDTO searchingRequest(String query, String offset, String limit, String site) {
        return prepareSearchingResponse(query, site, offset, limit);
    }

    private SearchingResponseDTO prepareSearchingResponse(String query, String site, String offset, String limit) {

        final Map<String, List<IndexEntity>> lemmasAndIndexMap = new HashMap<>();

        Map<String, Integer> queryLemmas = lemmaFinder.collectLemmas(query);

        List<String> actualLemmas;
        if (site == null) {
            actualLemmas = queryLemmas.keySet()
                    .stream()
                    .filter(lemma -> indexService.findLemmaCount(lemma) < 70)
                    .collect(Collectors.toList());
            actualLemmas.sort(Comparator.comparing(queryLemmas::get));

            actualLemmas.forEach(lemma1 -> {
                AtomicReference<List<IndexEntity>> foundLemmas
                        = new AtomicReference<>(indexService.findByLemma(actualLemmas.get(0)));

                actualLemmas.forEach(lemma2 -> foundLemmas.set(foundLemmas
                        .get()
                        .stream()
                        .filter(index -> indexService.findByLemma(lemma2).contains(index))
                        .toList()));
                lemmasAndIndexMap.put(lemma1, foundLemmas.get());
            });

        } else {
            actualLemmas = queryLemmas.keySet()
                    .stream()
                    .filter(lemma -> indexService.findLemmaCount(lemma) < 70)
                    .collect(Collectors.toList());
            actualLemmas.sort(Comparator.comparing(queryLemmas::get));

            actualLemmas.forEach(lemma -> {
                AtomicReference<List<IndexEntity>> foundLemmas
                        = new AtomicReference<>(indexService.findByLemmaAndSite(actualLemmas.get(0), site));
                actualLemmas.forEach(lemma1 -> foundLemmas.set(foundLemmas
                        .get()
                        .stream()
                        .filter(index -> indexService.findByLemmaAndSite(lemma, site).contains(index))
                        .toList()));

                lemmasAndIndexMap.put(lemma, foundLemmas.get());
            });
        }


        List<SearchingResponseDataDTO> responseData = prepareSearchingResponseDataDTO(lemmasAndIndexMap
                , actualLemmas, query)
                .stream()
                .limit(Integer.parseInt(limit))
                .skip(Long.parseLong(offset))
                .toList();

        return SearchingResponseDTO.builder()
                .count(responseData.size())
                .data(responseData)
                .result(true)
                .build();
    }

    private List<SearchingResponseDataDTO> prepareSearchingResponseDataDTO(Map<String, List<IndexEntity>> pages
            , List<String> lemmas, String query) {
        List<String> checkedPage = new ArrayList<>();
        List<SearchingResponseDataDTO> response = new ArrayList<>();

        pages.forEach((key, value) -> value.forEach(index1 -> {
            if (!checkedPage.contains(index1.getPage().getPath())) {
                AtomicReference<Float> relevance = new AtomicReference<>(index1.getRank() / 10F);

                pages.forEach((key1, value1) -> {
                    if (!key.equals(key1)) {
                        value1.forEach(index2 -> {
                            if (index1.getPage().getId().equals(index2.getPage().getId())
                                    && !checkedPage.contains(index1.getPage().getPath())) {
                                relevance.updateAndGet(v -> v + index2.getRank() / 10F);
                            }
                        });
                    }
                });

                try {
                    response.add(SearchingResponseDataDTO.builder()
                            .relevance(relevance.get())
                            .uri(index1.getPage().getPath())
                            .title(Jsoup.connect(index1.getPage()
                                    .getSite()
                                    .getUrl()
                                    .substring(0, index1.getPage().getSite().getUrl().length() - 1)
                                    + index1.getPage().getPath()).get().title())
                            .snippet(createSnippet(index1.getPage().getContent(), lemmas, query))
                            .site(index1.getPage().getSite().getUrl())
                            .siteName(index1.getPage().getSite().getName())
                            .build());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                checkedPage.add(index1.getPage().getPath());
            }
        }));
        response.sort(Comparator.comparing(SearchingResponseDataDTO::getRelevance).reversed());
        return response;
    }

    private String createSnippet(String content, List<String> lemmas, String query) {
        String snippet = "";
        Document doc = Jsoup.parse(content);
        String metaDescription = doc.select("meta[name=description]").attr("content");
        if(metaDescription.contains(query) || containsAny(metaDescription, lemmas)){
            if(metaDescription.contains(query)){
                snippet = metaDescription.replaceAll(query, "<b>"+query+"</b>");
                return snippet;
            }
            if(containsAny(metaDescription, lemmas)){
                snippet = metaDescription;
                for(String l : lemmas){
                    snippet = snippet.replaceAll(l, "<b>"+l+"</b>");
                }
                return snippet;
            }
        }
        else{
            // Если в мета-тегах ничего нет, ищем в основном тексте
            String text = lemmaFinder.deleteHTMLTags(content);
            String sentenceRegex = "[^.!?]*[.!?]";
            Pattern pattern = Pattern.compile(sentenceRegex);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String sentence = matcher.group().trim();
                if(sentence.length() > 300) {
                    sentence = sentence.substring(0, 300) + "...";
                }
                if(sentence.contains(query)){
                    return sentence.replace(query, "<b>"+ query +"</b>");
                }
                for(String word : lemmas){
                    if(sentence.contains(word)){
                        snippet = sentence.replaceAll(word, "<b>"+ word +"</b>");
                    }
                }
                if(!snippet.isEmpty()) return snippet;
            }
        }
        return snippet;
    }

    private Boolean containsAny(String text, Collection<String> lemmas) {
        for (String lemma : lemmas) {
            if (text.contains(lemma)) {
                return true; // Возвращаем true, если найдено хотя бы одно совпадение
            }
        }
        return false; // Возвращаем false, если ничего не найдено
    }

}
    