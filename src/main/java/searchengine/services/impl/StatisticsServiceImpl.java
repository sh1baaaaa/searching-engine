package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.entity.SiteEntity;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteService siteService;

    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        sitesList.forEach(site -> {
            SiteEntity siteEntity = siteService.findByUrl(site.getUrl());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            item.setPages(siteEntity.getPages().size());
            item.setLemmas(siteEntity.getLemmas().size());
            item.setStatus(siteEntity.getStatus().toString());
            item.setError(siteEntity.getLastError() == null ? "-" : siteEntity.getLastError());
            item.setStatusTime(siteEntity.getStatusTime().toInstant(ZoneOffset.UTC).toEpochMilli());
            total.setPages(total.getPages() + siteEntity.getPages().size());
            total.setLemmas(total.getLemmas() + siteEntity.getLemmas().size());
            detailed.add(item);
        });

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
