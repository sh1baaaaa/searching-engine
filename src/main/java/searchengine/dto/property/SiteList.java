package searchengine.dto.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Setter
@Getter
@Component
@ConfigurationProperties("indexing-settings")
public class SiteList {

    private ConcurrentLinkedQueue<Site> sites;

}
