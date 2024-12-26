package searchengine.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchingResponseDataDTO {

    private String site;

    private String siteName;

    private String uri;

    private String title;

    private String snippet;

    private Float relevance;

}
