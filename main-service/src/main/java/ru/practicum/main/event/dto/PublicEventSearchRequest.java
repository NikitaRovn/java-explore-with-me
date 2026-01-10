package ru.practicum.main.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.event.enums.EventSort;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicEventSearchRequest {
    private String text;
    private List<Long> categories;
    private Boolean paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable;
    private EventSort sort;
    private int from;
    private int size;
}