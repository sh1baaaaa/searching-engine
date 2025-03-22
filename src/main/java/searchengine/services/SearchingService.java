package searchengine.services;

import searchengine.dto.SearchingResponseDTO;


public interface SearchingService {

    SearchingResponseDTO search(String query, String siteUrl, Integer offset, Integer limit);


}
