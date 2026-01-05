package ru.practicum.stats.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.model.EndpointHit;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    @Query("select h.app as app, h.uri as uri, count(h.id) as hits " +
            "from EndpointHit h " +
            "where h.timestamp between :start and :end " +
            "and (:urisEmpty = true or h.uri in :uris) " +
            "group by h.app, h.uri " +
            "order by hits desc")
    List<ViewStatsProjection> findStats(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("uris") List<String> uris,
                                        @Param("urisEmpty") boolean urisEmpty);

    @Query("select h.app as app, h.uri as uri, count(distinct h.ip) as hits " +
            "from EndpointHit h " +
            "where h.timestamp between :start and :end " +
            "and (:urisEmpty = true or h.uri in :uris) " +
            "group by h.app, h.uri " +
            "order by hits desc")
    List<ViewStatsProjection> findUniqueStats(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("uris") List<String> uris,
                                              @Param("urisEmpty") boolean urisEmpty);

    interface ViewStatsProjection {
        String getApp();

        String getUri();

        Long getHits();
    }
}