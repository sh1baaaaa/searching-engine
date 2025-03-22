package searchengine.services;

import searchengine.entity.SiteEntity;

public interface SiteService {

    SiteEntity findByUrl(String url);


}
