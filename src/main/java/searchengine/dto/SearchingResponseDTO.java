package searchengine.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SearchingResponseDTO {

    private String uri;

    private String title;

    private String snippet;

    private Float relevance;

}

