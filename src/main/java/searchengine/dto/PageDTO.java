package searchengine.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageDTO {

    private Integer id;

    private SiteDTO site;

    private String path;

    private Integer code;

    private String content;

    private List<IndexDTO> indexes;

}
