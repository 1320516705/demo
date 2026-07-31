package com.competition.invoice.service.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 高德地图 API 客户端
 *
 * 用于 H3 索引转区域名称、POI 查询等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AMapGeoClient {

    private final WebClient webClient;

    @Value("${external.amap.api-key:}")
    private String apiKey;

    @Value("${external.amap.base-url:https://restapi.amap.com}")
    private String baseUrl;

    /**
     * 逆地理编码：坐标 → 地址描述
     */
    @SuppressWarnings("unchecked")
    public String reverseGeocode(double lng, double lat) {
        try {
            String location = lng + "," + lat;
            Map<String, Object> resp = webClient.get()
                    .uri(baseUrl + "/v3/geocode/regeo?key={key}&location={loc}", apiKey, location)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(5));

            if (resp != null && "1".equals(String.valueOf(resp.get("status")))) {
                Map<String, Object> regeo = (Map<String, Object>) resp.get("regeocode");
                if (regeo != null) {
                    return (String) regeo.get("formatted_address");
                }
            }
        } catch (Exception e) {
            log.warn("高德逆地理编码失败, lng={}, lat={}", lng, lat, e);
        }
        return "未知区域";
    }

    /**
     * POI 搜索：查询区域周边地标
     */
    @SuppressWarnings("unchecked")
    public String searchPoi(double lng, double lat, String keywords) {
        try {
            String location = lng + "," + lat;
            Map<String, Object> resp = webClient.get()
                    .uri(baseUrl + "/v3/place/around?key={key}&location={loc}&keywords={kw}&radius=1000",
                            apiKey, location, keywords)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(5));

            if (resp != null && "1".equals(String.valueOf(resp.get("status")))) {
                java.util.List<Map<String, Object>> pois =
                        (java.util.List<Map<String, Object>>) resp.get("pois");
                if (pois != null && !pois.isEmpty()) {
                    return (String) pois.get(0).get("name");
                }
            }
        } catch (Exception e) {
            log.warn("高德POI搜索失败", e);
        }
        return null;
    }
}
