package searchengine.entity;

import javax.persistence.*;

import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(name = "site")
@Entity
public class SiteEntity {

    @Column(name = "id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SiteStatus status;

    @UpdateTimestamp
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "siteId", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    private List<PageEntity> pages;

    @OneToMany(mappedBy = "site", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    private List<LemmaEntity> lemmas;

}
