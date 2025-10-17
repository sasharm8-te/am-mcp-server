package com.thousandeyes.cui.mcp.service;

import com.thousandeyes.models.account.v1.GetUserRegionRequestOuterClass.GetUserRegionRequest;
import com.thousandeyes.models.account.v1.GetUserRegionResponseOuterClass.GetUserRegionResponse;
import com.thousandeyes.ams.api.v1.account.RegionApiGrpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import com.thousandeyes.cui.mcp.model.dto.UserRegionsDto;

/**
 * Service for Account Management gRPC operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountManagementGrpcService implements AccountManagementService {
    
    private final RegionApiGrpc.RegionApiBlockingStub regionApiBlockingStub;

    

    @Override
    public GetUserRegionResponse getRegionByUserEmail(String userEmail) {
        GetUserRegionRequest request = GetUserRegionRequest.newBuilder().setUserEmail(userEmail).build();
        return regionApiBlockingStub.getUserRegion(request);
    }

    
    private String getStringArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required argument '" + key + "' is missing");
        }
        return value.toString();
    }

    /**
     * Get user regions by email address.
     * 
     * Calls the Account Management Service gRPC API:
     * - Uses GetUserRegionRequest with userEmail field
     * - Returns GetUserRegionResponse with regionIdList and defaultRegionId
     */
    public UserRegionsDto getUserRegions(Map<String, Object> arguments) {
        String email = getStringArgument(arguments, "email");
        
        log.info("Getting user regions for email: {}", email);
        
        try {
            
            GetUserRegionResponse response = getRegionByUserEmail(email);
            
            List<Integer> regionIds = response.getRegionIdList();
            log.info("Successfully retrieved {} regions for user: {}, default region: {}", 
                    regionIds.size(), email, response.getDefaultRegionId());
            
            return UserRegionsDto.fromRegionResponse(email, regionIds, response.getDefaultRegionId());
            
        } catch (Exception e) {
            log.error("Error getting user regions for email: {}", email, e);
            throw new RuntimeException("Failed to get user regions: " + e.getMessage());
        }
    }
}