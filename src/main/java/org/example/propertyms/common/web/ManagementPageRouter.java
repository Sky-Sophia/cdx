package org.example.propertyms.common.web;

import java.util.function.Consumer;
import org.example.propertyms.common.util.StringHelper;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 管理后台页面跳转与分页链接构造工具，避免在多个控制器中重复拼接查询参数。
 */
public final class ManagementPageRouter {
    private static final String MANAGEMENT_PATH = "/admin/management";

    private ManagementPageRouter() {}

    public static String redirectToTab(String tab) {
        return "redirect:" + buildTabUrl(tab);
    }

    public static String redirectToTab(String tab, Consumer<UriComponentsBuilder> customizer) {
        return "redirect:" + buildTabUrl(tab, customizer);
    }

    public static String buildTabUrl(String tab) {
        return buildTabUrl(tab, null);
    }

    public static String buildTabUrl(String tab, Consumer<UriComponentsBuilder> customizer) {
        UriComponentsBuilder builder = baseTabBuilder(tab);
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.build().encode().toUriString();
    }

    public static UriComponentsBuilder baseTabBuilder(String tab) {
        return UriComponentsBuilder.fromPath(MANAGEMENT_PATH)
                .queryParam("tab", tab);
    }

    public static void addTrimmedParam(UriComponentsBuilder builder, String name, String value) {
        String normalizedValue = StringHelper.trimToNull(value);
        if (normalizedValue != null) {
            builder.queryParam(name, normalizedValue);
        }
    }

    public static void addParam(UriComponentsBuilder builder, String name, Object value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }
}
