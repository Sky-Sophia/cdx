package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.model.PropertyUnit;
import org.example.javawebdemo.service.PropertyUnitService;
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
    public List<PropertyUnit> list(String keyword, Long buildingId, String status) {
        return propertyUnitMapper.findAll(keyword, buildingId, status);
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
        if (unit == null || unit.getBuildingId() == null || isBlank(unit.getUnitNo())) {
            throw new IllegalArgumentException("请填写楼栋和房号。");
        }
        if (isBlank(unit.getOccupancyStatus())) {
            unit.setOccupancyStatus("VACANT");
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
        propertyUnitMapper.deleteById(id);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
