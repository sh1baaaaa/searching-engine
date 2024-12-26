package searchengine.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class SearchingResponseDTO {

   private Boolean result;

   private Integer count;

   private List<SearchingResponseDataDTO> data;

}

