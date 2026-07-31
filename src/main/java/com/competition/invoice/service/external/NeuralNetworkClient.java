package com.competition.invoice.service.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 神经网络推理服务客户端
 *
 * 调用外部 NN 服务，输入特征向量，输出召回潜力分。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeuralNetworkClient {

    private final WebClient webClient;

    @Value("${external.neural-network.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${external.neural-network.api-key:}")
    private String apiKey;

    @Value("${external.neural-network.batch-size:100}")
    private int batchSize;

    @Value("${external.neural-network.max-retries:3}")
    private int maxRetries;

    /**
     * 批量推理
     * @param drivers 司机特征向量列表
     * @param modelVersion NN 模型版本
     * @return 推理结果（driverId -> recallScore）
     */
    public List<InferenceResult> batchInference(List<InferenceInput> drivers, String modelVersion) {
        NNRequest request = new NNRequest();
        request.setModelVersion(modelVersion);
        request.setDrivers(drivers);

        try {
            NNResponse response = webClient.post()
                    .uri(baseUrl + "/api/v1/inference/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (apiKey != null && !apiKey.isEmpty()) {
                            h.set("Authorization", "Bearer " + apiKey);
                        }
                    })
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(NNResponse.class)
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(8)))
                    .block(Duration.ofSeconds(60));

            if (response == null || response.getResults() == null) {
                throw new RuntimeException("NN 服务返回空结果");
            }
            return response.getResults();

        } catch (Exception e) {
            log.error("NN 推理调用失败, driverCount={}", drivers.size(), e);
            throw new RuntimeException("神经网络推理失败", e);
        }
    }

    // ---------- Request / Response DTO ----------

    @Data
    public static class NNRequest {
        private String modelVersion;
        private List<InferenceInput> drivers;
    }

    @Data
    public static class InferenceInput {
        private String driverId;
        private double[] features;
    }

    @Data
    public static class NNResponse {
        private List<InferenceResult> results;
    }

    @Data
    public static class InferenceResult {
        private String driverId;
        private double recallScore;
        private double confidence;
    }
}
