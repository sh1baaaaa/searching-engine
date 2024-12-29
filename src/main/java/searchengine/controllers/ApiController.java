package searchengine.controllers;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.IndexingResponse;
import searchengine.dto.SearchingResponseDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.PageService;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    @Getter
    private IndexingService siteService;

    @Getter
    private PageService pageService;

    private final SearchingService searchingService;

    @Autowired
    public ApiController(StatisticsService statisticsService, SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        siteService.startIndexing();
        return new ResponseEntity<>(IndexingResponse.builder()
                .result(true)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        siteService.stopIndexing();
        return new ResponseEntity<>(IndexingResponse.builder().result(true).build(), HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        pageService.indexByUrl(url);
        return new ResponseEntity<>(IndexingResponse.builder()
                .result(true)
                .build(), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponseDTO> search(@RequestParam String query,
                                                             @RequestParam(required = false) String site,
                                                             @RequestParam(required = false, defaultValue = "0") String offset,
                                                             @RequestParam(defaultValue = "20"
                                                             , required = false) String limit) {
        return new ResponseEntity<>(searchingService.searchingRequest(query, offset, limit, site), HttpStatus.OK);
    }

    @Autowired
    public void setSiteService(IndexingService siteService) {
        this.siteService = siteService;
    }

    @Autowired
    public void setPageService(PageService pageService) {
        this.pageService = pageService;
    }
}
