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
    public List<IndexEntity> findByLemma(String lemma) {
        return indexRepository.findByLemmaLemma(lemma);
    }

    @Override
    public List<IndexEntity> findByLemmaAndSite(String lemma, String site) {
        return indexRepository.findByLemmaAndSite(lemma, site);
    }

    @Override
    public void save(IndexEntity indexEntity) {
        indexRepository.save(indexEntity);
    }

    @Override
    public Integer findLemmaCount(String lemma) {
        return indexRepository.lemmaCount(lemma);
    }

}
