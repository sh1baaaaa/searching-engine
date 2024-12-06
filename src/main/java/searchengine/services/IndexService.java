package searchengine.services;

import searchengine.entity.IndexEntity;

import java.util.List;

public interface IndexService {

    Boolean isExistByLemmaLemmaAndPagePath(String lemma, String pagePath);

    void deleteByLemmaLemmaAndPagePath(String lemma, String pagePath);

    void saveAll(List<IndexEntity> indexEntities);

    void save(IndexEntity indexEntity);
}
