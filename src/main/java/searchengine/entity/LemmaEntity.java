package searchengine.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Setter
@Getter
@Table(name = "`lemma`")
@Entity
public class LemmaEntity {

    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private SiteEntity site;

    @Column(name = "lemma", nullable = false, unique = true)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", cascade = {CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    private List<IndexEntity> indexes;
}
