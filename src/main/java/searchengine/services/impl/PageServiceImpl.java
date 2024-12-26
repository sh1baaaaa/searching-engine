package searchengine.services.impl;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.IndexEntity;
import searchengine.entity.LemmaEntity;
import searchengine.entity.PageEntity;
import searchengine.exception.UnknownUrlPathException;
import searchengine.repository.PageRepository;
import searchengine.services.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class PageServiceImpl implements PageService {

    private final PageRepository pageRepository;

    private final LemmaService lemmaService;

    private final SiteService siteService;

    private final IndexService indexService;

    @Autowired
    public PageServiceImpl(PageRepository pageRepository, LemmaService lemmasService, SiteService siteService, IndexService indexService) {
        this.pageRepository = pageRepository;
        this.lemmaService = lemmasService;
        this.siteService = siteService;
        this.indexService = indexService;
    }

    @Override
    public void indexByUrl(String url) {
        log.info("Старт индексации страницы {}", url);

        String[] siteUrl = url.split("/");
        String path = url.replace(siteUrl[0].concat("//").concat(siteUrl[2])
                , "");

        if (!siteService.existByUrl(siteUrl[0]
                .concat("//")
                .concat(siteUrl[2])
                .concat("/"))){
            throw new UnknownUrlPathException("Данная страница находится за пределами сайтов," +
                    " указанных в конфигурационном файле");
        }

        if (pageRepository.existsByPath(path)){
            pageRepository.deleteById(pageRepository.findByPathAndSiteUrl(path
                    , siteUrl[0].concat("//")
                            .concat(siteUrl[2])
                            .concat("/")).getId());
        }

        pageRepository.save(processPage(url));
        log.info("Страница индексирована");

    }

    @Override
    public PageEntity processPage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .get();

            String[] siteUrl = url.split("/");
            String path = url.replace(siteUrl[0].concat("//").concat(siteUrl[2])
                    , "");

            PageEntity pageEntity = new PageEntity();


            pageEntity.setContent(doc.html());
            pageEntity.setSite(siteService
                    .findByUrl(siteUrl[0].concat("//").concat(siteUrl[2]).concat("/")));
            pageEntity.setCode(200);
            pageEntity.setPath(path);

            List<IndexEntity> indexList = new ArrayList<>();
            log.info("Получение лемм страницы");

            lemmaService.collectLemmas(doc.html())
                    .forEach((key, value) -> {
                        if (!indexService.isExistByLemmaLemmaAndPagePath(key, path)) {
                            IndexEntity indexEntity = new IndexEntity();
                            indexEntity.setPage(pageEntity);

                            LemmaEntity lemma;
                            if (!lemmaService.isExist(key)) {
                                lemma = new LemmaEntity();
                                lemma.setSite(siteService.findByUrl(siteUrl[0]
                                        .concat("//")
                                        .concat(siteUrl[2])
                                        .concat("/")));
                                lemma.setLemma(key);
                                lemma.setIndexes(List.of(indexEntity));
                                lemma.setFrequency(1);
                            } else {
                                lemma = lemmaService.findByLemma(key);
                                if (lemma.getIndexes() == null) {
                                    lemma.setIndexes(Arrays.asList(indexEntity));
                                } else {
                                    List<IndexEntity> indexes = new ArrayList<>(lemma.getIndexes());
                                    indexes.add(indexEntity);
                                    lemma.setIndexes(indexes);
                                }
                                lemma.setFrequency(lemma.getFrequency() + 1);
                            }

                            indexEntity.setLemma(lemma);
                            indexEntity.setRank(value);
                            indexList.add(indexEntity);
                        }
                    });

            pageEntity.setIndexes(indexList);
            return pageEntity;
        } catch (IOException e) {
            log.error("Ошибка индексации страницы");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(PageEntity pageEntity) {
        pageRepository.save(pageEntity);
    }

    @Override
    public void processPage(PageEntity pageEntity) {

    }

    @Override
    public PageEntity findByPath(String path) {
        return pageRepository.findByPath(path);
    }


}
