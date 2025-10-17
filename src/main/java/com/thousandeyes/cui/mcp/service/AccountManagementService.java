package com.thousandeyes.cui.mcp.service;
import com.thousandeyes.models.account.v1.GetUserRegionResponseOuterClass.GetUserRegionResponse;

/**
 * Service for Account Management gRPC operations.
 */

public interface AccountManagementService {
    
    GetUserRegionResponse getRegionByUserEmail(String userEmail);
}
