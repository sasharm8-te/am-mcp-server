// package com.thousandeyes.cui.mcp.service;

// import com.thousandeyes.cui.mcp.model.dto.UserRegionsDto;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.TestPropertySource;

// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;

// /**
//  * Test class for AccountManagementService.
//  * Note: This test will fail in CI/CD without a running gRPC server.
//  * It's mainly for local development and integration testing.
//  */
// @SpringBootTest
// @TestPropertySource(properties = {
//     "external-services.account-management.grpc-host=localhost",
//     "external-services.account-management.grpc-port=9999"
// })
// class AccountManagementServiceTest {

//     @Test
//     void testGetUserRegions_withValidEmail() {
//         // This test is mainly to verify the service can be instantiated
//         // and the method signature is correct
//         AccountManagementGrpcService service = new AccountManagementGrpcService();
        
//         // Test the argument validation
//         Map<String, Object> arguments = Map.of("email", "test@example.com");
        
//         // This will fail without a running gRPC server, but that's expected
//         assertThrows(RuntimeException.class, () -> {
//             service.getUserRegions(arguments);
//         });
//     }
    
//     @Test
//     void testGetUserRegions_withMissingEmail() {
//         AccountManagementGrpcService service = new AccountManagementGrpcService();
        
//         Map<String, Object> arguments = Map.of();
        
//         assertThrows(IllegalArgumentException.class, () -> {
//             service.getUserRegions(arguments);
//         });
//     }
    
//     @Test
//     void testUserRegionsDto() {
//         String email = "test@example.com";
//         java.util.List<Integer> regionIds = java.util.List.of(1, 2, 3);
        
//         UserRegionsDto dto = UserRegionsDto.fromRegionIds(email, regionIds);
        
//         assertEquals(email, dto.getEmail());
//         assertEquals(regionIds, dto.getRegionIds());
//         assertEquals(3, dto.getTotalRegions());
//     }
// }
