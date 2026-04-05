package org.example.propertyms.resident.service;

import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.resident.model.Resident;

import java.util.List;

public interface ResidentService {
    PageResult<Resident> listPaged(String keyword, String status, int page, int pageSize);

    Resident findById(Long id);

    void save(Resident resident);

    void deleteById(Long id);

    long countActive();

    long countOccupiedUnits();
}

