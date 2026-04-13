package org.example.propertyms.common.constant;

/**
 * 管理后台重定向 URL 常量，消除硬编码字符串。
 */
public final class RedirectUrls {

    public static final String MANAGEMENT_DASHBOARD = "redirect:/admin/management?tab=dashboard";
    public static final String MANAGEMENT_UNITS     = "redirect:/admin/management?tab=units";
    public static final String MANAGEMENT_RESIDENTS  = "redirect:/admin/management?tab=residents";
    public static final String MANAGEMENT_WORK_ORDERS = "redirect:/admin/management?tab=work-orders";
    public static final String MANAGEMENT_BILLS     = "redirect:/admin/management?tab=bills";
    public static final String MANAGEMENT_USERS     = "redirect:/admin/management?tab=users";
    public static final String LOGIN                = "redirect:/login";

    private RedirectUrls() {}
}


