package searchengine.features;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.JsoupSession;
import searchengine.entity.IndexEntity;
import searchengine.entity.PageEntity;
import searchengine.entity.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import searchengine.services.impl.IndexingServiceImpl;
import searchengine.dto.PageDTO;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class IndexingTask extends RecursiveTask<Set<PageEntity>>{

    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static JsoupSession connector;
    private static PageRepository pageRepository;
    private static LemmaFinder lematizator;
    public static AtomicBoolean isIndexing = IndexingServiceImpl.isIndexing;
    private final String domain;
    private final SiteEntity model;
    public final static ExecutorService executor = Executors.newFixedThreadPool(20);
    public PageDTO pageDTO;
    public Set<String> visited;
    public Set<PageEntity> pageModels = ConcurrentHashMap.newKeySet();

    public IndexingTask(PageRepository pageRepo, IndexRepository indexRepository, LemmaRepository lemmaRepository,
                        PageDTO pageDTO, Set<String> visited, String domain, SiteEntity siteModel, JsoupSession connector, LemmaFinder lemmaFinder) {
        IndexingTask.lemmaRepository = lemmaRepository;
        IndexingTask.indexRepository = indexRepository;
        IndexingTask.pageRepository = pageRepo;
        IndexingTask.connector = connector;
        IndexingTask.lematizator = lemmaFinder;
        this.pageDTO = pageDTO;
        this.visited = visited;
        this.domain = domain;
        this.model = siteModel;
    }

    private IndexingTask(String domain, SiteEntity model, PageDTO pageDTO, Set<String> visited) {
        this.domain = domain;
        this.model = model;
        this.pageDTO = pageDTO;
        this.visited = visited;
    }

    public LinkedHashSet<PageDTO> getPagesOnPage(PageDTO pageDTO) throws IOException {
        try {
            Thread.sleep(150);
        }catch (InterruptedException e){
            log.info("ForkJoin thread is interrupted!");
        }
        LinkedHashSet<PageDTO> pageDTOS = new LinkedHashSet<>();
        Elements elements = saveAllPageInfo(pageDTO, domain, model);
        pageDTO.setVisited(true);
        elements.forEach(e -> {
            String url = e.absUrl("href").replace("/$", " ");
            boolean validUrl = url.contains(domain) && !url.contains("#")
                    && !url.equals(pageDTO.getUrl()) && !visited.contains(url) && !url.contains(".sql") &&
                    !url.contains(".zip") && !url.contains(".yaml") && !url.contains(".jpg") && !url.contains(".pdf");
            if (validUrl) pageDTOS.add(new PageDTO(url));
        });
        pageDTO.setChildPageDTOS(pageDTOS);
        return pageDTOS;
    }

    @SneakyThrows
    @Override
    protected Set<PageEntity> compute() {
        if(isIndexing.get()) {
            Set<IndexingTask> taskListForPage = ConcurrentHashMap.newKeySet();
            visited.add(pageDTO.getUrl());

            getPagesOnPage(pageDTO).forEach(child -> {

                if ((!child.isVisited() || !visited.contains(child.getUrl())) && isIndexing.get()) {
                    IndexingTask subTask = new IndexingTask(domain, model, child, visited);
                    visited.add(child.getUrl());
                    subTask.fork();
                    taskListForPage.add(subTask);
                }
            });

            if (!taskListForPage.isEmpty()) {
                pageDTO.getChildPageDTOS()
                        .forEach(p -> taskListForPage.forEach(m -> {
                            if (p.getUrl().equals(m.pageDTO.getUrl()) && isIndexing.get()) {
                                try {
                                    Thread.sleep(150);
                                    m.join();
                                } catch (InterruptedException ignored){}
                            }
                    }));
                }
            }
        return pageModels;
    }

    public static Elements saveAllPageInfo(PageDTO pageDTO, String domain, SiteEntity model) throws IOException {
        Connection.Response response = connector
                .JsoupConnection()
                .newRequest()
                .url(pageDTO.getUrl())
                .execute();
        Document doc = response.parse();
        Elements elements = doc.select("a");
        if (!isIndexing.get()) executor.shutdownNow();

        PageEntity pageEntity = new PageEntity();
        pageEntity.setContent(doc.toString());
        pageEntity.setHttpStatusCode(response.statusCode());
        pageEntity.setPath(pageDTO.getUrl().replace(domain, ""));
        pageEntity.setSiteId(model);
        pageRepository.save(pageEntity);
        saveLemmas(doc, model, pageEntity);
        return elements;
    }

    public static void saveLemmas(Document doc, SiteEntity model, PageEntity pageModel){
        executor.execute(()->{
            Map<String, Integer> lemmas = lematizator.deleteTagsAndCollect(doc.toString());
            lemmas.forEach((k, v)->{
                lemmaRepository.updateOrInsertLemma(model.getId(), k);

                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(lemmaRepository.findByLemma(k));
                indexEntity.setRank(v);
                indexEntity.setPageId(pageModel);
                indexRepository.save(indexEntity);

            });
            lemmaRepository.flush();
            indexRepository.flush();
        });
        log.info("saving is finished");
    }
}
