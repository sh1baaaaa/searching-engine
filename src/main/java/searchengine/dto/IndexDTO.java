package searchengine.dto;

import lombok.Data;

@Data
public class IndexDTO {

    private Integer id;

    private PageDTO page;

    private LemmaDTO lemma;

    private Integer rank;

}
