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

    function createChart(container) {
        const existing = echarts.getInstanceByDom(container);
        if (existing) {
            existing.dispose();
        }
        return echarts.init(container);
    }

    /* ================================================================
     *  配色常量（与 CSS 变量保持一致）
     * ================================================================ */

    const COLORS = Object.freeze({
        primary:  "#2d7ff9",
        soft:     "#e8f3ff",
        positive: "#00b42a",
        warning:  "#ff7d00",
        danger:   "#f53f3f",
        teal:     "#36cfc9",
        neutral:  "#2d7ff9",
        text:     "#1d2129",
        text2:    "#4e5969",
        text3:    "#86909c",
        border:   "#e5e6eb",
    });

    /* ================================================================
     *  收缴进度 — 环形图
     * ================================================================ */

    function createDonutChart(container) {
        const rate     = toNumber(container.dataset.rate);
        const rateLabel = Math.round(rate);
        const received = toNumber(container.dataset.received);
        const pending  = toNumber(container.dataset.pending);

        const chart = createChart(container);

        chart.setOption({
            tooltip: {
                trigger: "item",
                formatter: ({ name, value }) => `${name}：¥${formatMoney(value)}`,
            },
            legend: { show: false },
            series: [
                {
                    type: "pie",
                    radius: ["66%", "82%"],
                    center: ["50%", "50%"],
                    avoidLabelOverlap: false,
                    itemStyle: {
                        borderRadius: 10,
                        borderColor: "#fff",
                        borderWidth: 2,
                    },
                    label: {
                        show: true,
                        position: "center",
                        formatter: () => `{rate|${rateLabel}%}\n{desc|收缴完成}`,
                        rich: {
                            rate: {
                                fontSize: 34,
                                fontWeight: "bold",
                                color: COLORS.text,
                                lineHeight: 42,
                            },
                            desc: {
                                fontSize: 13,
                                color: COLORS.text3,
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
                        { value: pending,  name: "待收缴", itemStyle: { color: "#f2f3f5" } },
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
     *  今日运营分布 — HTML 进度条
     * ================================================================ */

    function initDistributionBars(container) {
        if (!container) {
            return;
        }

        const values = {
            units: toNumber(container.dataset.units),
            residents: toNumber(container.dataset.residents),
            orders: toNumber(container.dataset.orders),
            bills: toNumber(container.dataset.bills),
        };

        const max = Math.max(values.units, values.residents, values.orders, values.bills, 1);

        container.querySelectorAll("[data-stat-key]").forEach((item) => {
            const key = item.dataset.statKey;
            const fill = item.querySelector("[data-stat-fill]");
            if (!fill || !Object.prototype.hasOwnProperty.call(values, key)) {
                return;
            }
            const value = values[key];
            const width = value <= 0 ? 0 : (value / max) * 100;
            fill.style.width = `${width}%`;
        });
    }

    /* ================================================================
     *  近 7 日投诉趋势 — 双折线图（静态占位数据）
     * ================================================================ */

    function createTrendChart(container) {
        const chart = createChart(container);
        const days = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];
        const createdSeries = [16, 18, 14, 19, 17, 20, 18];
        const resolvedSeries = [11, 12, 10, 13, 14, 16, 15];

        chart.setOption({
            animationDuration: 900,
            color: [COLORS.primary, COLORS.teal],
            tooltip: {
                trigger: "axis",
                backgroundColor: "rgba(29, 33, 41, 0.92)",
                borderWidth: 0,
                textStyle: {
                    color: "#fff",
                },
            },
            legend: {
                show: false,
            },
            grid: {
                left: 28,
                right: 18,
                top: 10,
                bottom: 20,
                containLabel: true,
            },
            xAxis: {
                type: "category",
                boundaryGap: false,
                data: days,
                axisLine: {
                    lineStyle: {
                        color: "#d1d5db",
                    },
                },
                axisTick: {
                    show: false,
                },
                axisLabel: {
                    color: COLORS.text3,
                    fontSize: 11,
                    margin: 14,
                },
            },
            yAxis: {
                type: "value",
                min: 0,
                max: 25,
                interval: 5,
                axisLine: {
                    show: false,
                },
                axisTick: {
                    show: false,
                },
                axisLabel: {
                    color: COLORS.text3,
                    fontSize: 11,
                },
                splitLine: {
                    lineStyle: {
                        color: COLORS.border,
                    },
                },
            },
            series: [
                {
                    name: "新增投诉",
                    type: "line",
                    smooth: true,
                    symbol: "circle",
                    symbolSize: 8,
                    lineStyle: {
                        width: 3,
                        color: COLORS.primary,
                    },
                    itemStyle: {
                        color: "#ffffff",
                        borderColor: COLORS.primary,
                        borderWidth: 2,
                    },
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: "rgba(45, 127, 249, 0.12)" },
                            { offset: 1, color: "rgba(45, 127, 249, 0.01)" },
                        ]),
                    },
                    data: createdSeries,
                },
                {
                    name: "已处理完结",
                    type: "line",
                    smooth: true,
                    symbol: "circle",
                    symbolSize: 8,
                    lineStyle: {
                        width: 3,
                        color: COLORS.teal,
                    },
                    itemStyle: {
                        color: "#ffffff",
                        borderColor: COLORS.teal,
                        borderWidth: 2,
                    },
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: "rgba(54, 207, 201, 0.12)" },
                            { offset: 1, color: "rgba(54, 207, 201, 0.01)" },
                        ]),
                    },
                    data: resolvedSeries,
                },
            ],
        });

        return chart;
    }

    /* ================================================================
     *  入口：初始化 + 响应式 + Tab 切换支持
     * ================================================================ */

    function init() {
        const distributionEl = document.querySelector(".dashboard-distribution[data-units]");
        if (distributionEl) {
            initDistributionBars(distributionEl);
        }

        if (typeof echarts === "undefined") {
            console.warn("[Dashboard] ECharts 未加载，图表初始化已跳过。请检查 /js/vendor/echarts.min.js 是否可访问。");
            return;
        }

        const charts = [];


        const donutEl = document.getElementById("chart-collection-donut");
        if (donutEl) {
            charts.push(createDonutChart(donutEl));
        }

        const trendEl = document.getElementById("chart-dashboard-trend");
        if (trendEl) {
            charts.push(createTrendChart(trendEl));
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
