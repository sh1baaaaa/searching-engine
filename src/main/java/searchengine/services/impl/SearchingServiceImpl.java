package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.SearchingResponseDTO;
import searchengine.dto.SearchingResponseDataDTO;
import searchengine.entity.IndexEntity;
import searchengine.entity.LemmaEntity;
import searchengine.entity.PageEntity;
import searchengine.entity.SiteEntity;
import searchengine.features.LemmaFinder;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchingService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchingServiceImpl implements SearchingService {


    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private final IndexRepository indexRepository;

    private final LemmaFinder lemmatizator;

    public SearchingServiceImpl(LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository, IndexRepository indexRepository, LemmaFinder lemmatizator) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.lemmatizator = lemmatizator;
    }

    @Override
    public SearchingResponseDTO search(String query, String siteUrl, Integer offset, Integer limit) {
        Set<String> lemmas = lemmatizator.getLemmaSet(query);
        log.info("Lemmas: {}", lemmas);
        List<Integer> lemmasIds = lemmaRepository.findLemmasInSet(lemmas)
                .stream()
                .map(LemmaEntity::getId)
                .collect(Collectors.toCollection(ArrayList::new));
        SiteEntity siteModel = siteUrl == null ? null : siteRepository.findSiteByUrl(siteUrl);
        List<PageEntity> pagesWithLemmas = new ArrayList<>(
                pageRepository.findPagesWithLemmasAndSite(
                        lemmas,
                        siteModel == null ? 0 : siteModel.getId(),
                        query.split("\\s+").length)
        );

        return makeSearchingResponse(pagesWithLemmas, lemmas, lemmasIds, siteModel, query, limit, offset);
    }

    public SearchingResponseDTO makeSearchingResponse(List<PageEntity> pages, Set<String> lemmas
            , List<Integer> lemmasIds, SiteEntity siteModel, String query, Integer limit, Integer offset){
        List<SearchingResponseDataDTO> dataList = new ArrayList<>();
        Map<PageEntity, Integer> pageWithAbsRelevance = new HashMap<>();
        AtomicInteger maxRankSum = new AtomicInteger(0);
        pages.forEach(p->{
            List<IndexEntity> indexes = indexRepository.findIndexByPageIdAndLemmas(p.getId(), lemmasIds);
            int sumOfRanks = indexes.stream().mapToInt(IndexEntity::getRank).sum();
            pageWithAbsRelevance.put(p, sumOfRanks);
            if(sumOfRanks > maxRankSum.get()){
                maxRankSum.set(sumOfRanks);
            }
        });
        pageWithAbsRelevance.forEach((page, pageRank) -> {
            log.info("Page: {}", page.getPath());
            String content = page.getContent();

            SearchingResponseDataDTO searchingData = SearchingResponseDataDTO.builder()
                    .site(siteModel != null ? siteModel.getUrl()
                            : siteRepository.findById(page.getSiteId().getId()).map(SiteEntity::getUrl).get())
                    .siteName(siteModel != null ? siteModel.getName()
                            : siteRepository.findById(page.getSiteId().getId()).map(SiteEntity::getName).get())
                    .uri(page.getPath())
                    .snippet(createSnippet(content, lemmas, query))
                    .relevance((float) (pageRank.doubleValue() / maxRankSum.doubleValue()))
                    .build();

            try {
                searchingData.setTitle(content.split("<title>")[1].split("</title>")[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                searchingData.setTitle("Playback");
                log.error("{} does not contain title", page.getPath());
            }

            dataList.add(searchingData);
        });

        return SearchingResponseDTO.builder()
                .result(true)
                .data(dataList
                        .stream()
                        .distinct()
                        .sorted(Comparator.comparingDouble(SearchingResponseDataDTO::getRelevance).reversed())
                        .skip(offset)
                        .limit(limit)
                        .toList())
                .count(dataList.size())
                .build();
    }

    private String createSnippet(String content, Set<String> lemmas, String query) {
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
            String text = lemmatizator.deleteAllHtmlTags(content);
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

    public static boolean containsAny(String text, Collection<String> lemmas) {
        for (String lemma : lemmas) {
            if (text.contains(lemma)) {
                return true;
            }
        }
        return false;
    }


}
    