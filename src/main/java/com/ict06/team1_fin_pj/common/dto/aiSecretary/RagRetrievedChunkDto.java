package com.ict06.team1_fin_pj.common.dto.aiSecretary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagRetrievedChunkDto {

    private Integer documentId;

    private String documentTitle;

    private Integer chunkId;

    private Integer chunkNo;

    private String sectionTitle;

    private String content;

    private String filePath;

    private Double similarityScore;
}
