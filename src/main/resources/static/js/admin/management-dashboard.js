/**
 * 管理后台 Dashboard 数据可视化模块
 *
 * 职责：从 DOM data-* 属性读取服务器端渲染的数据，初始化 ECharts 实例。
 * 设计原则：
 *   - 零耦合：不依赖任何全局变量或外部状态，仅通过 DOM 容器的 data-* 属性获取数据
 *   - 单一职责：每种图表由独立工厂函数创建，互不影响
 *   - 优雅降级：若容器不存在或 ECharts 未加载，在控制台给出提示后静默跳过
 */
(() => {
    "use strict";

    /* ================================================================
     *  工具函数
     * ================================================================ */

    /** 安全地将字符串转为数值，失败返回 fallback */
    function toNumber(value, fallback = 0) {
        const n = Number(value);
        return Number.isFinite(n) ? n : fallback;
    }

    /** 格式化金额：千位逗号 + 两位小数 */
    function formatMoney(value) {
        return value.toLocaleString("zh-CN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });
    }

    /* ================================================================
     *  配色常量（与 CSS 变量保持一致）
     * ================================================================ */

    const COLORS = Object.freeze({
        primary:  "#409eff",
        soft:     "#d9e4ff",
        positive: "#0f9d58",
        warning:  "#faad14",
        danger:   "#ff4d4f",
        neutral:  "#409eff",
    });

    /* ================================================================
     *  收缴进度 — 环形图
     * ================================================================ */

    function createDonutChart(container) {
        const rate     = toNumber(container.dataset.rate);
        const received = toNumber(container.dataset.received);
        const pending  = toNumber(container.dataset.pending);

        const chart = echarts.init(container);

        chart.setOption({
            tooltip: {
                trigger: "item",
                formatter: ({ name, value }) => `${name}：¥${formatMoney(value)}`,
            },
            legend: { show: false },
            series: [
                {
                    type: "pie",
                    radius: ["58%", "80%"],
                    center: ["50%", "50%"],
                    avoidLabelOverlap: false,
                    itemStyle: {
                        borderRadius: 6,
                        borderColor: "#fff",
                        borderWidth: 2,
                    },
                    label: {
                        show: true,
                        position: "center",
                        formatter: () => `{rate|${rate}%}\n{desc|收缴完成}`,
                        rich: {
                            rate: {
                                fontSize: 26,
                                fontWeight: "bold",
                                color: "#183b8c",
                                lineHeight: 36,
                            },
                            desc: {
                                fontSize: 13,
                                color: "#7184a1",
                                lineHeight: 22,
                            },
                        },
                    },
                    emphasis: {
                        label: { show: true },
                        itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,.12)" },
                    },
                    data: [
                        { value: received, name: "已收缴", itemStyle: { color: COLORS.primary } },
                        { value: pending,  name: "待收缴", itemStyle: { color: COLORS.soft } },
                    ],
                    animationType: "scale",
                    animationEasing: "cubicOut",
                    animationDuration: 800,
                },
            ],
        });

        return chart;
    }

    /* ================================================================
     *  今日运营分布 — 水平条形图
     * ================================================================ */

    function createBarChart(container) {
        const items = [
            { name: "在管房屋",   sub: "户室资源总量", value: toNumber(container.dataset.units),     color: COLORS.neutral },
            { name: "活跃住户",   sub: "当前实际在住", value: toNumber(container.dataset.residents), color: COLORS.positive },
            { name: "待处理工单", sub: "服务响应压力", value: toNumber(container.dataset.orders),    color: COLORS.warning },
            { name: "待缴账单",   sub: "费用收缴压力", value: toNumber(container.dataset.bills),     color: COLORS.danger },
        ];

        // ECharts y 轴顺序从下到上，需反转
        const reversed = [...items].reverse();

        const chart = echarts.init(container);

        chart.setOption({
            tooltip: {
                trigger: "axis",
                axisPointer: { type: "shadow" },
                formatter: (params) => {
                    const p = params[0];
                    const item = reversed[p.dataIndex];
                    return `<strong>${item.name}</strong><br/>${item.sub}：${p.value}`;
                },
            },
            grid: {
                left: 110,
                right: 50,
                top: 12,
                bottom: 12,
                containLabel: false,
            },
            xAxis: {
                type: "value",
                show: false,
            },
            yAxis: {
                type: "category",
                data: reversed.map((i) => i.name),
                axisLine: { show: false },
                axisTick: { show: false },
                axisLabel: {
                    fontSize: 13,
                    color: "#202020",
                    fontWeight: 500,
                },
            },
            series: [
                {
                    type: "bar",
                    barWidth: 14,
                    data: reversed.map((i) => ({
                        value: i.value,
                        itemStyle: {
                            color: i.color,
                            borderRadius: [0, 7, 7, 0],
                        },
                    })),
                    label: {
                        show: true,
                        position: "right",
                        fontSize: 13,
                        fontWeight: 600,
                        color: "#1f3f86",
                    },
                    animationDuration: 900,
                    animationEasing: "cubicOut",
                },
            ],
        });

        return chart;
    }

    /* ================================================================
     *  入口：初始化 + 响应式 + Tab 切换支持
     * ================================================================ */

    function init() {
        if (typeof echarts === "undefined") {
            console.warn("[Dashboard] ECharts 未加载，图表初始化已跳过。请检查 /js/vendor/echarts.min.js 是否可访问。");
            return;
        }

        const charts = [];

        const donutEl = document.getElementById("chart-collection-donut");
        if (donutEl) {
            charts.push(createDonutChart(donutEl));
        }

        const barEl = document.getElementById("chart-distribution-bar");
        if (barEl) {
            charts.push(createBarChart(barEl));
        }

        if (charts.length === 0) {
            return;
        }

        /** 通知所有图表重新计算尺寸（Tab 切换 / 窗口缩放时调用） */
        function resizeAll() {
            charts.forEach((c) => {
                try { c.resize(); } catch (_) { /* 忽略已销毁实例 */ }
            });
        }

        // 窗口大小变化时自适应（防抖）
        let resizeTimer = null;
        window.addEventListener("resize", () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(resizeAll, 120);
        });

        // 暴露 resize 钩子供 Tab 切换模块调用
        // 当 dashboard pane 从 display:none 变为可见时，ECharts 需要重新计算尺寸
        window.__dashboardChartsResize = resizeAll;
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
