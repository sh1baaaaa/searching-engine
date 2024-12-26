package searchengine.services;

import searchengine.dto.SearchingResponseDTO;


public interface SearchingService {

    SearchingResponseDTO searchingRequest(String query, String offset, String limit, String site);


}
