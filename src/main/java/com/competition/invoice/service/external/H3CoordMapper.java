package com.competition.invoice.service.external;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * H3 网格坐标映射
 *
 * 将 mock 数据中的 H3 level-8 索引映射到真实的上海市区经纬度坐标。
 * 每个 H3 格网中心偏移少量随机值，模拟司机分布的热力效果。
 *
 * 生产环境中替换为 uber/h3 库做精确的 h3ToGeo 转换。
 */
@Component
public class H3CoordMapper {

    /**
     * 上海市区核心区域 → H3 level-8 网格中心坐标
     * 覆盖：静安、徐汇、长宁、黄浦、浦东陆家嘴、虹口、杨浦、普陀、宝山、闵行
     */
    private static final Map<String, double[]> GRID = Map.ofEntries(
        // 882a100c73 — 静安寺/南京西路商圈
        Map.entry("882a100c73fffff", new double[]{121.4480, 31.2246}),
        // 882a100c8b — 陆家嘴/浦东CBD
        Map.entry("882a100c8bfffff", new double[]{121.5034, 31.2415}),
        // 882a100c99 — 徐家汇商圈
        Map.entry("882a100c99fffff", new double[]{121.4365, 31.1960}),
        // 882a100c13 — 宝山万达/共康
        Map.entry("882a100c13fffff", new double[]{121.4378, 31.3252}),
        // 882a100c31 — 虹桥枢纽/长宁
        Map.entry("882a100c31fffff", new double[]{121.3380, 31.1962}),
        // 882a100c7b — 五角场/大学城
        Map.entry("882a100c7bfffff", new double[]{121.5156, 31.3034}),
        // 882a100c49 — 中山公园/长宁
        Map.entry("882a100c49fffff", new double[]{121.4147, 31.2215}),
        // 882a100c55 — 大宁/闸北
        Map.entry("882a100c55fffff", new double[]{121.4530, 31.2798}),
        // 882a100c4f — 新天地/淮海路
        Map.entry("882a100c4fffff", new double[]{121.4750, 31.2195}),
        // 882a100c5d — 打浦桥/日月光
        Map.entry("882a100c5dfffff", new double[]{121.4705, 31.2085}),
        // 882a100c89 — 世纪公园/花木
        Map.entry("882a100c89fffff", new double[]{121.5478, 31.2109}),
        // 882a100c69 — 上海火车站/不夜城
        Map.entry("882a100c69fffff", new double[]{121.4565, 31.2500})
    );

    /**
     * 根据 H3 索引获取对应的经纬度
     * 对同一 H3 格网内的司机做微偏移，避免完全重叠
     */
    public double[] getCoord(String h3Index, int offsetSeed) {
        double[] base = GRID.getOrDefault(h3Index, new double[]{121.4700, 31.2300});

        // 微偏移：H3 level-8 格网边长约 650m，在格网内随机偏移 ±300m ≈ ±0.003°
        double jitter = (offsetSeed % 7 - 3) * 0.0009;   // -0.0027 ~ +0.0027
        double jitter2 = ((offsetSeed * 13) % 7 - 3) * 0.0009;

        return new double[]{base[0] + jitter, base[1] + jitter2};
    }
}
