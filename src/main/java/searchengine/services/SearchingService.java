package searchengine.services;

import searchengine.dto.SearchingResponseDTO;


import java.util.List;

public interface SearchingService {

    List<SearchingResponseDTO> searchingRequest(String query, String offset, String limit);

    List<SearchingResponseDTO> searchingRequest(String query, String offset, String limit, String site);


}
