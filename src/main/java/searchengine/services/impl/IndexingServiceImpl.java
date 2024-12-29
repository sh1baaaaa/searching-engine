package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.IndexingResponse;
import searchengine.dto.property.SiteList;
import searchengine.entity.SiteEntity;
import searchengine.entity.SiteStatus;
import searchengine.features.SiteIndexingTask;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final IndexRepository indexRepository;

    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private static ForkJoinPool FORK_JOIN_POOL;

    private final SiteList siteList;

    private static ExecutorService executorService;

    public static final AtomicBoolean isIndexing = new AtomicBoolean(false);

    private final LemmaService lemmaService;

    private final IndexService indexService;

    private static ExecutorService mainExecutor;

    private final PageService pageService;


    @Autowired
    public IndexingServiceImpl(IndexRepository indexRepository, LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository, SiteList siteList
            , LemmaService lemmaService, IndexService indexService, PageService pageService) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteList = siteList;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.pageService = pageService;
    }


    @Override
    public IndexingResponse startIndexing() {
        FORK_JOIN_POOL = new ForkJoinPool(24);
        mainExecutor = Executors.newFixedThreadPool(6);
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        log.info("Попытка запуска индексации");
        if (isIndexing.get()) {
            log.error("Индексация уже запущена");
            return IndexingResponse.builder()
                    .result(false)
                    .error("Индексация уже запущена")
                    .build();
        }

        deleteAllSitesData();
        isIndexing.set(true);

        executorService.execute(() -> siteList
                .getSites()
                .forEach(site -> mainExecutor.execute(() -> {
                    log.info("Запущена индексация страницы {}", site.getUrl());

                    SiteEntity siteEntity = new SiteEntity();
                    siteEntity.setStatus(SiteStatus.INDEXING);
                    siteEntity.setUrl(site.getUrl());
                    siteEntity.setName(site.getName());
                    siteRepository.save(siteEntity);

                    SiteIndexingTask siteIndexing = new SiteIndexingTask(indexService, pageService, lemmaService, site.getUrl(),
                            siteRepository.findByUrl(siteEntity.getUrl()), site.getUrl(), siteRepository);

                    FORK_JOIN_POOL.invoke(siteIndexing);
                }
                ))
        );

        return IndexingResponse.builder()
                .result(true)
                .build();
    }


    @Override
    public IndexingResponse stopIndexing() {
        log.warn("Попытка остановить индексацию");
        if (!isIndexing.get()) {
            return IndexingResponse.builder()
                    .result(false)
                    .error("Индексация не запущена")
                    .build();
        }

        FORK_JOIN_POOL.shutdown();
        try {
            if (FORK_JOIN_POOL.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                FORK_JOIN_POOL.shutdownNow();
                if (FORK_JOIN_POOL.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                    log.error("ForkJoinPool did not terminate!");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        mainExecutor.shutdown();
        try {
            if (mainExecutor.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                mainExecutor.shutdownNow();
                if (mainExecutor.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                    log.error("MainExecutor did not terminate!");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                if (executorService.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                    log.error("ExecutorService did not terminate!");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        isIndexing.set(false);
        log.info("Индексация остановлена");
        siteRepository.updateAllSitesStatus("INDEXED");

        return IndexingResponse
                .builder()
                .result(true)
                .build();
    }

    public void deleteAllSitesData() {
        indexRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        siteRepository.deleteAll();
    }

}
