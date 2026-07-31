package com.competition.invoice.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 神经网络推理服务
 *
 * 架构说明：
 * ─────────────────────────────────────────────────────────
 *  生产环境：替换为独立的模型服务（PyTorch/TensorFlow Serving / ONNX Runtime），
 *  通过 REST/gRPC 调用。本项目用 NeuralNetworkClient 对接，API 契约不变。
 *
 *  当前实现：一个轻量级多层感知机（MLP）模拟器 ——
 *    输入层(10) → 隐藏层1(16, ReLU) → Dropout → 隐藏层2(8, ReLU) → 输出层(1, Sigmoid) → ×100
 *
 *  为什么不是线性加权：
 *    - 线性模型无法捕获特征之间的交互效应。例如：
 *      "高活跃度 + 低取消率"的组合价值 远大于 两个特征独立相加。
 *    - 非线性激活(ReLU)引入了阈值和拐点，模拟真实司机行为的"临界触发"模式：
 *      活跃度超过某个阈值后，召回概率跃升。
 *    - Dropout 层在推理时提供天然的置信度估计基础。
 * ─────────────────────────────────────────────────────────
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inference")
public class NeuralNetworkController {

    // ═══════════════════════════════════════════════════════
    // MLP 模型参数（模拟训练好的权重）
    // 注：生产环境这些参数来自 model checkpoint，而不是硬编码
    // ═══════════════════════════════════════════════════════

    /** 隐藏层1: 10 → 16, 权重矩阵 W1[16][10] + 偏置 b1[16] */
    private static final double[][] W1 = {
        { 0.42, 0.15, 0.08, 0.31,-0.22,-0.18, 0.35, 0.12, 0.28, 0.05},
        { 0.18, 0.55, 0.03, 0.22,-0.12,-0.08, 0.20, 0.48, 0.15,-0.10},
        {-0.08, 0.10, 0.62, 0.05,-0.05,-0.02, 0.08, 0.12, 0.55, 0.08},
        { 0.35, 0.20, 0.12,-0.30,-0.20,-0.15, 0.25, 0.18, 0.10, 0.38},
        { 0.20, 0.08, 0.05, 0.45,-0.10,-0.08, 0.15, 0.10, 0.05, 0.42},
        { 0.30, 0.35, 0.20, 0.25,-0.18,-0.20, 0.28, 0.30, 0.22, 0.15},
        {-0.15,-0.10, 0.05,-0.08,-0.42,-0.35,-0.12,-0.05, 0.02,-0.28},
        { 0.22, 0.12, 0.05, 0.30, 0.05, 0.02, 0.10, 0.15, 0.08, 0.45},
        { 0.28, 0.25, 0.18, 0.15,-0.10,-0.08, 0.20, 0.28, 0.18, 0.20},
        {-0.05, 0.08, 0.10,-0.05,-0.08,-0.28, 0.02, 0.05, 0.12, 0.30},
        { 0.15, 0.42, 0.05, 0.18,-0.08,-0.05, 0.35, 0.38, 0.10, 0.05},
        { 0.32, 0.18, 0.08, 0.22,-0.15,-0.12, 0.25, 0.20, 0.15, 0.35},
        { 0.25, 0.10, 0.55, 0.08, 0.02,-0.05, 0.12, 0.15, 0.50, 0.12},
        {-0.30,-0.15,-0.05, 0.10,-0.20,-0.55, 0.08,-0.10,-0.02,-0.35},
        { 0.20, 0.08, 0.05, 0.35, 0.05, 0.08,-0.10, 0.10, 0.05, 0.55},
        { 0.38, 0.20, 0.10, 0.28,-0.22,-0.18, 0.30, 0.22, 0.18, 0.20},
    };
    private static final double[] b1 = {0.12,0.08,0.05,0.15,0.10,0.14,0.06,0.18,0.13,0.02,0.09,0.16,0.07,0.04,0.20,0.11};

    /** 隐藏层2: 16 → 8 */
    private static final double[][] W2 = {
        { 0.35, 0.15, 0.05,-0.25, 0.18, 0.20,-0.10, 0.22, 0.12, 0.05, 0.15, 0.25,-0.08,-0.20, 0.30, 0.10},
        { 0.20, 0.38, 0.08, 0.10, 0.25, 0.22,-0.05, 0.18, 0.32, 0.12, 0.08, 0.15, 0.10,-0.12, 0.20, 0.18},
        {-0.12,0.05, 0.45, 0.08,-0.05, 0.10,-0.08, 0.05, 0.48, 0.10, 0.42, 0.08,-0.05,-0.15, 0.08, 0.12},
        { 0.25, 0.12, 0.10,-0.35, 0.15, 0.18,-0.08, 0.10, 0.08, 0.05,-0.05, 0.12,-0.25,-0.18, 0.15, 0.35},
        { 0.15, 0.08, 0.05, 0.28, 0.22, 0.12,-0.05, 0.15, 0.10, 0.42, 0.18, 0.08, 0.05,-0.05, 0.12, 0.40},
        { 0.18, 0.22, 0.15, 0.12, 0.28, 0.25,-0.08, 0.20, 0.25, 0.15, 0.10, 0.18, 0.08,-0.08, 0.22, 0.15},
        {-0.10,-0.05, 0.05, 0.08,-0.35,-0.30,-0.40, 0.02,-0.05, 0.08, 0.05,-0.10,-0.20,-0.45,-0.15, 0.05},
        { 0.22, 0.15, 0.10, 0.25, 0.10, 0.15, 0.05, 0.12, 0.18, 0.22, 0.15, 0.10, 0.05, 0.08, 0.20, 0.32},
    };
    private static final double[] b2 = {0.10,0.12,0.06,0.14,0.16,0.13,0.05,0.11};

    /** 输出层: 8 → 1 */
    private static final double[] W3 = {0.38, 0.22, 0.15, -0.35, 0.28, 0.20, -0.42, 0.18};
    private static final double b3 = -0.30;  // 偏置控制基础分，让分布集中在 40-80 区间

    /**
     * 批量推理
     */
    @PostMapping("/batch")
    public NNResponse batchInference(@RequestBody NNRequest request) {
        log.info("NN 推理: modelVersion={}, count={}", request.getModelVersion(), request.getDrivers().size());

        List<InferenceResult> results = new ArrayList<>();
        for (InferenceInput driver : request.getDrivers()) {
            double raw = forward(driver.getFeatures());
            double score = Math.round(raw * 100 * 10) / 10.0;    // 0-100
            double confidence = computeConfidence(driver.getFeatures());
            results.add(new InferenceResult(driver.getDriverId(), score, confidence));
        }

        NNResponse resp = new NNResponse();
        resp.setResults(results);
        return resp;
    }

    /**
     * MLP 前向传播
     *
     * input[10] → hidden1[16] → ReLU → hidden2[8] → ReLU → output[1] → sigmoid → [0,1]
     */
    private double forward(double[] features) {
        if (features == null || features.length == 0) return 0.15; // 无特征给低分

        // 确保至少 10 维，不足补 0
        double[] x = new double[10];
        for (int i = 0; i < Math.min(features.length, 10); i++) {
            x[i] = clamp(features[i]);
        }

        // Hidden Layer 1: 10 → 16, ReLU
        double[] h1 = new double[16];
        for (int i = 0; i < 16; i++) {
            double sum = b1[i];
            for (int j = 0; j < 10; j++) sum += W1[i][j] * x[j];
            h1[i] = relu(sum);
        }

        // Hidden Layer 2: 16 → 8, ReLU
        double[] h2 = new double[8];
        for (int i = 0; i < 8; i++) {
            double sum = b2[i];
            for (int j = 0; j < 16; j++) sum += W2[i][j] * h1[j];
            h2[i] = relu(sum);
        }

        // Output: 8 → 1, Sigmoid → [0, 1]
        double out = b3;
        for (int i = 0; i < 8; i++) out += W3[i] * h2[i];
        return sigmoid(out);
    }

    private double relu(double x)    { return Math.max(0, x); }
    private double sigmoid(double x) { return 1.0 / (1.0 + Math.exp(-x)); }
    private double clamp(double v)   { return Math.max(0, Math.min(1, v)); }

    /**
     * 置信度：特征向量的 L2 范数越大（信息越丰富），置信度越高
     */
    private double computeConfidence(double[] features) {
        double sum = 0;
        int n = Math.min(features.length, 10);
        for (int i = 0; i < n; i++) sum += features[i] * features[i];
        return Math.round(Math.min(1.0, Math.sqrt(sum / n) * 1.5) * 100) / 100.0;
    }

    // ---------- Request / Response ----------

    @Data public static class NNRequest { private String modelVersion; private List<InferenceInput> drivers; }
    @Data public static class InferenceInput { private String driverId; private double[] features; }
    @Data public static class NNResponse { private List<InferenceResult> results; }
    @Data public static class InferenceResult {
        private String driverId;
        @JsonProperty("recallScore") private double recallScore;
        private double confidence;
        public InferenceResult() {}
        public InferenceResult(String driverId, double recallScore, double confidence) {
            this.driverId = driverId; this.recallScore = recallScore; this.confidence = confidence;
        }
    }
}
