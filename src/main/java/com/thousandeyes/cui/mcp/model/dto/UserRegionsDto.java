package com.thousandeyes.cui.mcp.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for user regions response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegionsDto {
    
    private String email;
    private List<Integer> regionIds;
    private int totalRegions;
    private Integer defaultRegionId;
    
    public static UserRegionsDto fromRegionIds(String email, List<Integer> regionIds) {
        return UserRegionsDto.builder()
                .email(email)
                .regionIds(regionIds)
                .totalRegions(regionIds != null ? regionIds.size() : 0)
                .build();
    }
    
    public static UserRegionsDto fromRegionResponse(String email, List<Integer> regionIds, Integer defaultRegionId) {
        return UserRegionsDto.builder()
                .email(email)
                .regionIds(regionIds)
                .totalRegions(regionIds != null ? regionIds.size() : 0)
                .defaultRegionId(defaultRegionId)
                .build();
    }
}
