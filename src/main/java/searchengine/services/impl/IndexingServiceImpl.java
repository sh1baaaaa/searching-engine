package searchengine.services.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.property.SiteList;
import searchengine.entity.SiteEntity;
import searchengine.entity.Status;
import searchengine.exception.IndexingServiceException;
import searchengine.features.SiteIndexingTask;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.IndexingService;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;

    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    private final SiteList siteList;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final AtomicBoolean isIndexing = new AtomicBoolean(false);

    private final LemmaService lemmaService;

    private final IndexService indexService;

    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();

    @Getter
    private PageService pageService;


    @Autowired
    public IndexingServiceImpl(SiteRepository siteRepository, SiteList siteList
            , LemmaService lemmaService, IndexService indexService) {
        this.siteRepository = siteRepository;
        this.siteList = siteList;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }


    @Override
    public void startIndexing() {
        if (isIndexing.get()) {
            throw new IndexingServiceException("Индексация уже запущена");
        }

        siteRepository.deleteAll();
        isIndexing.set(true);

        executorService.submit(() -> {
            try {

                siteList.getSites().forEach(site -> {
                    Future<?> future = executorService.submit(() -> {
                        try {
                            log.info("Запущена индексация страницы {}", site.getName());
                            SiteEntity siteEntity = new SiteEntity();
                            siteEntity.setStatus(Status.INDEXING);
                            siteEntity.setUrl(site.getUrl());
                            siteEntity.setName(site.getName());
                            siteRepository.save(siteEntity);

                            SiteIndexingTask siteIndexing = new SiteIndexingTask(indexService, pageService, lemmaService, site.getUrl(),
                                    siteRepository.findByUrl(siteEntity.getUrl()), site.getUrl());

                            FORK_JOIN_POOL.invoke(siteIndexing);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    futures.add(future);
                });

                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                log.error("Ошибка при ожидании завершения индексации: {}", e.getMessage());
            } finally {
                isIndexing.set(false);
                log.info("Индексация завершена");
            }
        });
    }

    @Override
    public void stopIndexing() {
        log.warn("Попытка остановить индексацию");
        if (!isIndexing.get()) {
            throw new IndexingServiceException("Индексация не запущена");
        }
        FORK_JOIN_POOL.shutdownNow();
        futures.forEach(future -> future.cancel(true));
        executorService.shutdownNow();
        log.info("Индексация остановлена");
    }

    @Autowired
    public void setPageService(PageService pageService) {
        this.pageService = pageService;
    }

}
