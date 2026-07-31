package com.competition.invoice.service.emergency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 服务客户端（应急实时查询）
 *
 * 通过 MCP JSON-RPC 协议调用 query_nearby_drivers 工具。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpServiceClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${external.mcp.base-url:http://localhost:8081}")
    private String mcpBaseUrl;

    @Value("${external.mcp.query-timeout:15s}")
    private Duration queryTimeout;

    /**
     * 查询指定区域内的在线司机
     * @param polygon 多边形坐标 [[lng,lat],...]
     * @return 司机ID列表及基本状态
     */
    @SuppressWarnings("unchecked")
    public List<NearbyDriver> queryNearbyDrivers(List<double[]> polygon) {
        try {
            // 构建 MCP JSON-RPC 请求
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "method", "tools/call",
                "params", Map.of(
                    "name", "query_nearby_drivers",
                    "arguments", Map.of(
                        "polygon", polygon,
                        "bufferRadius", 500  // 500m 缓冲区
                    )
                )
            );

            Map<String, Object> response = webClient.post()
                    .uri(mcpBaseUrl + "/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(queryTimeout);

            if (response != null && response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (content != null && !content.isEmpty()) {
                    String json = (String) content.get(0).get("text");
                    return objectMapper.readValue(json,
                            objectMapper.getTypeFactory().constructCollectionType(
                                    List.class, NearbyDriver.class));
                }
            }
        } catch (Exception e) {
            log.error("MCP 查询周边司机失败", e);
            throw new RuntimeException("MCP服务查询失败", e);
        }
        return List.of();
    }

    /**
     * 周边司机数据结构
     */
    @lombok.Data
    public static class NearbyDriver {
        private String driverId;
        private String driverName;
        private String phone;
        private Double lng;
        private Double lat;
        private String status;       // ONLINE / BUSY / OFFLINE
        private String h3Index;
        private String featureVector;
    }
}
