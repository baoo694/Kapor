package com.kapor.techtalk.dto;

import com.kapor.techtalk.model.RoleplaySession;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoleplayHistoryDto {
    List<RoleplaySession> content;
    int page;
    int size;
    boolean hasMore;
}
