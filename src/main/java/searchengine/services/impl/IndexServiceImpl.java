package searchengine.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.IndexEntity;
import searchengine.repository.IndexRepository;
import searchengine.services.IndexService;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {

    private final IndexRepository indexRepository;

    @Autowired
    public IndexServiceImpl(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    @Override
    public Boolean isExistByLemmaLemmaAndPagePath(String lemma, String pagePath) {
        return indexRepository.existsByPagePathAndLemmaLemma(pagePath, lemma);
    }

    @Override
    public void deleteByLemmaLemmaAndPagePath(String lemma, String pagePath) {
        indexRepository.deleteByPagePathAndLemmaLemma(pagePath, lemma);
    }

    @Override
    public void saveAll(List<IndexEntity> indexEntities) {
        indexRepository.saveAll(indexEntities);
    }

    @Override
    public void save(IndexEntity indexEntity) {
        indexRepository.save(indexEntity);
    }
}
