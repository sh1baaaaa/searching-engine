package searchengine.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.services.SiteService;

@Service
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;

    @Autowired
    public SiteServiceImpl(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public SiteEntity findByUrl(String url) {
        return siteRepository.findSiteByUrl(url);
    }

}
