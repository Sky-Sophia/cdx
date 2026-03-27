package org.example.javawebdemo.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.javawebdemo.mapper.BuildingMapper;
import org.example.javawebdemo.mapper.FeeBillMapper;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.mapper.ResidentMapper;
import org.example.javawebdemo.mapper.WorkOrderMapper;
import org.example.javawebdemo.model.DashboardStats;
import org.example.javawebdemo.model.FeeBill;
import org.example.javawebdemo.model.WorkOrder;
import org.springframework.stereotype.Service;

@Service
public class PropertyDashboardService {
    private final BuildingMapper buildingMapper;
    private final PropertyUnitMapper propertyUnitMapper;
    private final ResidentMapper residentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final FeeBillMapper feeBillMapper;

    public PropertyDashboardService(BuildingMapper buildingMapper,
                                    PropertyUnitMapper propertyUnitMapper,
                                    ResidentMapper residentMapper,
                                    WorkOrderMapper workOrderMapper,
                                    FeeBillMapper feeBillMapper) {
        this.buildingMapper = buildingMapper;
        this.propertyUnitMapper = propertyUnitMapper;
        this.residentMapper = residentMapper;
        this.workOrderMapper = workOrderMapper;
        this.feeBillMapper = feeBillMapper;
    }

    public DashboardStats stats() {
        DashboardStats stats = new DashboardStats();
        stats.setBuildingCount(buildingMapper.countAll());
        stats.setUnitCount(propertyUnitMapper.countAll());
        stats.setResidentCount(residentMapper.countActive());
        stats.setOpenOrderCount(workOrderMapper.countOpen());
        stats.setDueBillCount(feeBillMapper.countDue());
        stats.setTotalReceivable(nullSafe(feeBillMapper.sumReceivable()));
        stats.setTotalReceived(nullSafe(feeBillMapper.sumReceived()));
        return stats;
    }

    public List<WorkOrder> recentOrders(int limit) {
        return workOrderMapper.findRecent(limit);
    }

    public List<FeeBill> dueBills(int limit) {
        return feeBillMapper.findDueSoon(limit);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
