package ru.practicum.stats;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

public class StatsClient {
    private final RestClient restClient;

    public StatsClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void addHit(EndpointHitDto endpointHitDto) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder b = uriBuilder.path("/stats")
                            .queryParam("start", start)
                            .queryParam("end", end);
                    if (unique) {
                        b.queryParam("unique", true);
                    }

                    if (uris != null && !uris.isEmpty()) {
                        b.queryParam("uris", uris);
                    }

                    return b.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {});
    }
}