package org.example.propertyms.dashboard.service;

import java.util.List;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.dashboard.dto.DashboardStats;
import org.example.propertyms.resident.service.ResidentService;
import org.example.propertyms.unit.service.PropertyUnitService;
import org.example.propertyms.workorder.model.WorkOrder;
import org.example.propertyms.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

/**
 * 仪表板服务 — 通过 Service 层获取统计数据，消除原版直接注入 Mapper 的层级违规。
 */
@Service
public class PropertyDashboardService {
    private final BuildingService buildingService;
    private final PropertyUnitService propertyUnitService;
    private final ResidentService residentService;
    private final WorkOrderService workOrderService;
    private final FeeBillService feeBillService;

    public PropertyDashboardService(BuildingService buildingService,
                                    PropertyUnitService propertyUnitService,
                                    ResidentService residentService,
                                    WorkOrderService workOrderService,
                                    FeeBillService feeBillService) {
        this.buildingService = buildingService;
        this.propertyUnitService = propertyUnitService;
        this.residentService = residentService;
        this.workOrderService = workOrderService;
        this.feeBillService = feeBillService;
    }

    public DashboardStats stats() {
        DashboardStats stats = new DashboardStats();
        stats.setBuildingCount(buildingService.countAll());
        stats.setUnitCount(propertyUnitService.countAll());
        stats.setOccupiedUnitCount(residentService.countOccupiedUnits());
        stats.setResidentCount(residentService.countActive());
        stats.setOpenOrderCount(workOrderService.countOpen());
        stats.setDueBillCount(feeBillService.countDue());
        stats.setTotalReceivable(feeBillService.sumReceivable());
        stats.setTotalReceived(feeBillService.sumReceived());
        return stats;
    }

    public List<WorkOrder> recentOrders(int limit) {
        return workOrderService.findRecent(limit);
    }

    public List<FeeBill> dueBills(int limit) {
        return feeBillService.findDueSoon(limit);
    }
}


