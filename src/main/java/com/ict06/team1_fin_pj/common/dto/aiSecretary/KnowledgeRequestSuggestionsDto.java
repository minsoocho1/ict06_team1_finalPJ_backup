package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KnowledgeRequestSuggestionsDto {

    private final List<String> requestTypes;

    private final List<String> categories;
}
