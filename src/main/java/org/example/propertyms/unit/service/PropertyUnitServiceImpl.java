package org.example.propertyms.unit.service;

import java.util.List;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.unit.mapper.PropertyUnitMapper;
import org.example.propertyms.unit.model.OccupancyStatus;
import org.example.propertyms.unit.model.PropertyUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyUnitServiceImpl implements PropertyUnitService {
    private final PropertyUnitMapper propertyUnitMapper;

    public PropertyUnitServiceImpl(PropertyUnitMapper propertyUnitMapper) {
        this.propertyUnitMapper = propertyUnitMapper;
    }

    @Override
    public List<PropertyUnit> listSimple() {
        return propertyUnitMapper.findAllSimple();
    }

    @Override
    public PropertyUnit findById(Long id) {
        return propertyUnitMapper.findById(id);
    }

    @Override
    @Transactional
    public void save(PropertyUnit unit) {
        if (unit == null || unit.getBuildingId() == null || StringHelper.isBlank(unit.getUnitNo())) {
            throw new IllegalArgumentException("请填写楼栋和房号。");
        }
        if (StringHelper.isBlank(unit.getOccupancyStatus())) {
            unit.setOccupancyStatus(OccupancyStatus.VACANT.name());
        }
        try {
            if (unit.getId() == null) {
                propertyUnitMapper.insert(unit);
            } else {
                propertyUnitMapper.update(unit);
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("同一楼栋下房号重复，请检查后重试。");
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        try {
            propertyUnitMapper.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("该房屋仍有关联账单、住户或工单，暂时无法删除，请先清理关联数据后再试。");
        }
    }

    @Override
    public PageResult<PropertyUnit> listPaged(String keyword, Long buildingId, String status, int page, int pageSize) {
        long total = propertyUnitMapper.count(keyword, buildingId, status);
        int offset = PageResult.calcOffset(page, pageSize);
        List<PropertyUnit> items = propertyUnitMapper.findAllPaged(keyword, buildingId, status, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    public long countAll() {
        return propertyUnitMapper.countAll();
    }
}

