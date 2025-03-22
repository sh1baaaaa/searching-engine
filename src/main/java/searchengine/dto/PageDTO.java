package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;

public class PageDTO {

    @Setter
    @Getter
    private String url;
    @Setter
    @Getter
    private HashSet<PageDTO> childPageDTOS = new HashSet<>();
    private boolean isVisited = false;

    public PageDTO(String url) {
        this.url = url;
    }


    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }

    @Override
    public String toString() {
        return "url='" + url + '\'' + (childPageDTOS.isEmpty() ? "" : " Pages on url:\n " + childPageDTOS);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        PageDTO pageDTO = (PageDTO) obj;
        return this.url.equals(pageDTO.url);
    }

    @Override
    public int hashCode() {
        boolean isRootPage = false;
        return Objects.hash(url, childPageDTOS, isRootPage, isVisited);
    }

}
