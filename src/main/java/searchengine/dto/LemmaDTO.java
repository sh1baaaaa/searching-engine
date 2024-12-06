package searchengine.dto;

import lombok.Data;
import java.util.List;

@Data
public class LemmaDTO {

    private Integer id;

    private SiteDTO site;

    private Integer frequency;

    private List<IndexDTO> indexes;

}
