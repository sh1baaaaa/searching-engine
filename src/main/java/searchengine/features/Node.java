package searchengine.features;

import searchengine.entity.PageEntity;

import java.io.IOException;
import java.util.Collection;

public interface Node {

    Collection<PageEntity> getChildren() throws IOException;
    String getValue();


}
