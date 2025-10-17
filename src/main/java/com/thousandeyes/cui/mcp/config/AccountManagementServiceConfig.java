package com.thousandeyes.cui.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import com.thousandeyes.ams.api.v1.account.RegionApiGrpc;

@Configuration
@RequiredArgsConstructor
public class AccountManagementServiceConfig {
    
    private final AccountManagementServiceProperties accountManagementServiceProperties;

    @Bean
    public RegionApiGrpc.RegionApiBlockingStub getRegionApiBlockingStub(ManagedChannel channel) {
        return RegionApiGrpc.newBlockingStub(channel);
    }

    @Bean
    public RegionApiGrpc.RegionApiStub getRegionApiAsyncStub(ManagedChannel channel) {
        return RegionApiGrpc.newStub(channel);
    }

    @Bean
    public ManagedChannel getManagedChannel() {
        String endpoint = accountManagementServiceProperties.getGrpcEndpoint();
        boolean useTls = accountManagementServiceProperties.isGrpcUseTls();
        
        ManagedChannelBuilder<?> channelBuilder;
        
        if (useTls) {
            // For ingress endpoints with TLS (standard port 443)
            channelBuilder = ManagedChannelBuilder.forAddress(endpoint, 443)
                    .useTransportSecurity();
        } else {
            // For plain text connections (standard port 80)
            channelBuilder = ManagedChannelBuilder.forAddress(endpoint, 80)
                    .usePlaintext();
        }
        
        return channelBuilder
                .keepAliveTime(30, java.util.concurrent.TimeUnit.SECONDS)
                .keepAliveTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
                .build();
    }
}
