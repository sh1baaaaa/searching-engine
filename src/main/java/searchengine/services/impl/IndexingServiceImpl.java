package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupSession;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.IndexingResponse;
import searchengine.dto.PageDTO;
import searchengine.entity.IndexEntity;
import searchengine.entity.PageEntity;
import searchengine.entity.SiteEntity;
import searchengine.entity.SiteStatus;
import searchengine.features.LemmaFinder;
import searchengine.features.IndexingTask;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepo;

    private final PageRepository pageRepo;

    private final IndexRepository indexRepo;

    private final LemmaRepository lemmaRepo;

    private final SitesList sites;

    private final JsoupSession connectionFab;

    private final LemmaFinder lemmaFinder;

    public static AtomicBoolean isIndexing = new AtomicBoolean(false);

    static final String[] errors = {
            "Данная страница находится за пределами сайтов, указанных в конфигурационном файле",
            "Индексация уже запущена",
            "Индексация не запущена"};

    private final Map<String, Boolean> statusOfIndexing = new ConcurrentHashMap<>();
    private static final ForkJoinPool forkJoinPool = new ForkJoinPool(24);
    public static final ExecutorService mainExecutor = Executors.newFixedThreadPool(6);

    public IndexingServiceImpl(SiteRepository siteRepo, PageRepository pageRepo, IndexRepository indexRepo
            , LemmaRepository lemmaRepo, SitesList sites, JsoupSession connectionFab, LemmaFinder lemmaFinder) {
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
        this.indexRepo = indexRepo;
        this.lemmaRepo = lemmaRepo;
        this.sites = sites;
        this.connectionFab = connectionFab;
        this.lemmaFinder = lemmaFinder;
    }

    @Override
    public IndexingResponse startIndexing() {
        if(isIndexing.get()){
            return IndexingResponse.builder().result(false).error(errors[1]).build();
        }
        deleteAllSitesData();
        log.info("Starting indexing sites");
        isIndexing.set(true);
        updateSitesInfo();
        sites.getSites().forEach(site -> mainExecutor.execute(() -> {
            String url = site.getUrl();
            statusOfIndexing.put(url, true);
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setUrl(url);
            siteEntity.setName(site.getName());
            siteEntity.setStatus(SiteStatus.INDEXING);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepo.save(siteEntity);
            siteRepo.flush();

            try {
                Set<String> set = ConcurrentHashMap.newKeySet();
                IndexingTask task = new IndexingTask(pageRepo, indexRepo, lemmaRepo, new PageDTO(url), set, url
                        , siteRepo.findSiteByUrl(url), connectionFab, lemmaFinder);
                forkJoinPool.invoke(task);
            } catch (Exception exception){
                log.error("Error in IndexingService - startIndexing method: {}", exception.getMessage());
            }

            log.info("Pages from {} saved in DB", url);
            statusOfIndexing.put(url, false);
        }));

        return IndexingResponse.builder().result(true).build();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if(!isIndexing.get()){
            return IndexingResponse.builder().result(false).error(errors[2]).build();
        }
        isIndexing.set(false);
        try {
            forkJoinPool.shutdownNow();
            IndexingTask.executor.shutdown();
            if(IndexingTask.executor.awaitTermination(10000, TimeUnit.MILLISECONDS)){
                IndexingTask.executor.shutdownNow();
            }
        } catch (Exception exception){
            log.error("Error in IndexingService - stopIndexing method: {}", exception.getMessage());
        }
        sites.getSites().forEach(site -> {
            String url = site.getUrl();
            siteRepo.updateIndexingTimeByUrl(LocalDateTime.now(), url);
            siteRepo.updateStatusByUrl(SiteStatus.INDEXED.toString(), url);
        });
        mainExecutor.shutdownNow();
        log.info("Indexing is stopped!!!");
        return IndexingResponse.builder().result(true).build();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        List<String> sitesUrls = sites.getSites().stream().map(Site::getUrl).toList();
        boolean valid = sitesUrls.stream().anyMatch(url::contains);
        if (url.contains("#") || url.contains(".sql") || url.contains(".zip")
                || url.contains(".yaml") || url.contains(".jpg") || url.contains(".pdf")){
            return IndexingResponse.builder().result(false).error("Provided url have incorrect type").build();
        } else if(valid){
            mainExecutor.execute(() -> {
                SiteEntity siteModel = Optional
                        .ofNullable(siteRepo.findSiteByUrl(sites.getSites()
                                .stream()
                                .map(Site::getUrl)
                                .filter(url::contains)
                                .collect(Collectors.joining())))
                        .orElse(
                            SiteEntity.builder()
                                    .url(sites.getSites()
                                            .stream()
                                            .map(Site::getUrl)
                                            .filter(url::contains)
                                            .collect(Collectors.joining()))
                                    .name(sites.getSites()
                                            .stream()
                                            .filter(s -> url.contains(s.getUrl()))
                                            .map(Site::getName)
                                            .collect(Collectors.joining()))
                                    .status(SiteStatus.INDEXING)
                                    .statusTime(LocalDateTime.now())
                                    .build());
                siteRepo.save(siteModel);
                siteRepo.updateStatusByUrl(String.valueOf(SiteStatus.INDEXING), url);
                connectAndParsePage(url, siteModel);
                log.info("Indexing page: {} - is done!", url);
                siteRepo.updateStatusByUrl(String.valueOf(SiteStatus.INDEXED), siteModel.getUrl());
            });
            return IndexingResponse.builder().result(true).build();
        }
        else{
            return IndexingResponse.builder().result(false).error("""
                    Данная страница находится за пределами сайтов,\s
                    указанных в конфигурационном файле
                    """).build();
        }
    }

    @Override
    public void deleteAllSitesData() {
        pageRepo.deleteAll();
        siteRepo.deleteAll();
        indexRepo.deleteAll();
        lemmaRepo.deleteAll();
    }

    private void updateSitesInfo(){
        mainExecutor.execute(()->{
            while(isIndexing.get()){
                try {
                    Thread.sleep(10000);
                    updateAllSitesStatuses(SiteStatus.INDEXING);
                } catch (InterruptedException e) {
                    updateAllSitesStatuses(SiteStatus.FAILED);
                    log.info(e.getMessage());
                }
                if(!statusOfIndexing.containsValue(true)){
                    isIndexing.set(false);
                    updateAllSitesStatuses(SiteStatus.INDEXED);
                    log.info("Circle while is done!");
                    break;
                }
            }
            Thread.currentThread().interrupt();
        });
    }

    private void updateAllSitesStatuses(SiteStatus status){
        sites.getSites().forEach(site -> {
            String url = site.getUrl();
            siteRepo.updateIndexingTimeByUrl(LocalDateTime.now(), url);
            siteRepo.updateStatusByUrl(String.valueOf(status), url);
            log.info("StatusTime is updated...");
        });
    }

    private void connectAndParsePage(String url, SiteEntity siteModel){
        try {
            Connection.Response response = connectionFab
                    .JsoupConnection()
                    .newRequest()
                    .url(url)
                    .execute();
            Document doc = response.parse();
            PageEntity pageModel = PageEntity.builder()
                    .path(url.replace(siteModel.getUrl(), ""))
                    .content(doc.toString())
                    .httpStatusCode(response.statusCode())
                    .siteId(siteModel)
                    .build();
            pageRepo.save(pageModel);
            Map<String, Integer> lemmas = lemmaFinder.deleteTagsAndCollect(doc.toString());
            lemmas.forEach((k, v)->{
                lemmaRepo.updateOrInsertLemma(siteModel.getId(), k);
                IndexEntity index = IndexEntity.builder()
                        .lemma(lemmaRepo.findByLemma(k)) // лемма
                        .rank(v) // кол-во данной леммы на странице
                        .pageId(pageModel) // страница
                        .build();
                indexRepo.save(index);
            });
            lemmaRepo.flush();
            indexRepo.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
