package searchengine.features;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.entity.IndexEntity;
import searchengine.entity.PageEntity;
import searchengine.entity.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.impl.IndexingServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class SiteIndexingTask extends RecursiveTask<List<PageEntity>> implements Node{

    public SiteIndexingTask(IndexService indexService, PageService pageService, LemmaService lemmaService, String firstNode, SiteRepository siteRepository){
        this.indexService = indexService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.firstNode = firstNode;
        this.siteRepository = siteRepository;
    }
    public SiteIndexingTask(IndexService indexService, PageService pageService, LemmaService lemmaService
            , String firstNode, SiteEntity siteEntity, String mainLink, SiteRepository siteRepository){
        this.indexService = indexService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.siteEntity = siteEntity;
        this.firstNode = firstNode;
        this.mainLink = mainLink;
        this.siteRepository = siteRepository;
    }

    private final LemmaFinder lemmaFinder;

    {
        try {
            lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final IndexService indexService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private SiteEntity siteEntity;
    private String mainLink;
    private final String firstNode;
    private final List<String> seenSites = new ArrayList<>();
    private final SiteRepository siteRepository;


    @SneakyThrows
    @Override
    protected List<PageEntity> compute() {
        List<PageEntity> links = new ArrayList<>();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        getChildren().forEach(child -> {
            SiteIndexingTask task = new SiteIndexingTask(indexService, pageService, lemmaService, child.getPath()
                    , siteRepository);
            task.fork();
            links.add(child);
        });

        siteRepository.updateSiteStatusByUrl(mainLink, "INDEXED");
        log.info("Индексация страницы {} завершена", mainLink);
        return links;
    }

    @Override
    public Collection<PageEntity> getChildren() {
        ArrayList<PageEntity> nodes = new ArrayList<>();
        String node = getValue();
        if(!seenSites.contains(node)) {

            Document doc;
            try {
                doc = Jsoup.connect(node)
                        .ignoreContentType(true)
                        .get();
            } catch (IOException e) {
                siteRepository.updateSiteStatusByUrl(mainLink, "FAILED", e.getMessage());
                throw new RuntimeException(e);
            }

            doc.getElementsByTag("a").forEach(link -> {
                if (!Thread.currentThread().isInterrupted() && IndexingServiceImpl.isIndexing.get()) {
                    String childNode = link.attr("abs:href");
                    PageEntity pageEntity = new PageEntity();

                    if (!seenSites.contains(childNode)
                            && !childNode.isEmpty()
                            && childNode.startsWith(mainLink)
                            && !childNode.equals(mainLink)) {
                        seenSites.add(childNode);

                        try {
                            pageEntity.setContent(Jsoup.connect(childNode)
                                    .ignoreContentType(true)
                                    .get()
                                    .html());
                        } catch (IOException e) {
                            siteRepository.updateSiteStatusByUrl(mainLink, "FAILED", e.getMessage());
                            throw new RuntimeException(e);
                        }

                        pageEntity.setPath("/" + childNode.replace(mainLink, ""));
                        seenSites.add(childNode);
                        pageEntity.setCode(200);
                        pageEntity.setSite(siteEntity);

                    }
                    if (pageEntity.getPath() != null) {
                        savePages(pageEntity);
                        nodes.add(pageEntity);
                    }
                }
            });

            return nodes;
        } else {
            return new ArrayList<>();
        }
    }


    private void savePages(PageEntity pageEntity) {
        pageService.save(pageEntity);
        saveLemmas(pageEntity);
    }


    private void saveLemmas(PageEntity pageEntity) {
        lemmaFinder.deleteTagsAndCollectLemmas(pageEntity.getContent())
                .forEach((key, value) -> {
                    lemmaService.insertOrUpdateLemma(siteEntity.getId(), key);

                    IndexEntity indexEntity = new IndexEntity();
                    indexEntity.setLemma(lemmaService.findByLemma(key));
                    indexEntity.setRank(value);
                    indexEntity.setPage(pageEntity);
                    indexService.save(indexEntity);
        });
    }

    @Override
    public String getValue() {
        return firstNode;
    }
}
