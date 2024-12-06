package searchengine.processor;

import lombok.SneakyThrows;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.entity.IndexEntity;
import searchengine.entity.PageEntity;
import searchengine.entity.SiteEntity;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;


public class SiteIndexingTask extends RecursiveTask<List<PageEntity>> implements Node{

    public SiteIndexingTask(IndexService indexService, PageService pageService, LemmaService lemmaService, String firstNode){
        this.indexService = indexService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.firstNode = firstNode;
    }
    public SiteIndexingTask(IndexService indexService, PageService pageService, LemmaService lemmaService
            , String firstNode, SiteEntity siteEntity, String mainLink){
        this.indexService = indexService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.siteEntity = siteEntity;
        this.firstNode = firstNode;
        this.mainLink = mainLink;
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



    @SneakyThrows
    @Override
    protected List<PageEntity> compute() {

        List<PageEntity> links = new ArrayList<>();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        getChildren().forEach(child -> {
            SiteIndexingTask task = new SiteIndexingTask(indexService, pageService, lemmaService, child.getPath());
            task.fork();
            links.add(child);
        });

        return links;
    }

    @Override
    public Collection<PageEntity> getChildren() throws IOException {
        ArrayList<PageEntity> nodes = new ArrayList<>();
        String node = getValue();
        if(!seenSites.contains(node)) {

            Document doc = Jsoup.connect(node)
                    .ignoreContentType(true)
                    .get();

            doc.getElementsByTag("a").forEach(link -> {
                String childNode = link.attr("abs:href");
                PageEntity pageEntity = new PageEntity();

                if(!seenSites.contains(childNode)
                        && !childNode.isEmpty()
                        && childNode.startsWith(mainLink)
                        && !childNode.equals(mainLink)) {
                    seenSites.add(childNode);
                    try {
                        pageEntity.setContent(Jsoup.connect(childNode)
                                .ignoreContentType(true)
                                .get()
                                .html());

                        pageEntity.setPath("/" + childNode.replace(mainLink, ""));
                        seenSites.add(childNode);
                        pageEntity.setCode(200);
                        pageEntity.setSite(siteEntity);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(pageEntity.getPath() != null){
                    savePages(pageEntity);
                    nodes.add(pageEntity);
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
