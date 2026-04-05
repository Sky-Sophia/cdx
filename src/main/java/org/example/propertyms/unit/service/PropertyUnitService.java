package org.example.propertyms.unit.service;

import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.unit.model.PropertyUnit;

import java.util.List;

public interface PropertyUnitService {
    PageResult<PropertyUnit> listPaged(String keyword, Long buildingId, String status, int page, int pageSize);

    List<PropertyUnit> listSimple();

    PropertyUnit findById(Long id);

    void save(PropertyUnit unit);

    void deleteById(Long id);

    long countAll();
}

