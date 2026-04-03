package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.model.PropertyUnit;

public interface PropertyUnitService {
    List<PropertyUnit> list(String keyword, Long buildingId, String status);

    List<PropertyUnit> listSimple();

    PropertyUnit findById(Long id);

    void save(PropertyUnit unit);

    void deleteById(Long id);
}
