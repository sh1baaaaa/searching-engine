package searchengine.services;

import searchengine.entity.PageEntity;

public interface PageService {

    void indexByUrl(String url);

    PageEntity processPage(String url);

    void save(PageEntity pageEntity);

    void processPage(PageEntity pageEntity);

    PageEntity findByPath(String path);
}
