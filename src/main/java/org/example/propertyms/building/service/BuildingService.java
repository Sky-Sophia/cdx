package org.example.propertyms.building.service;

import java.util.List;
import org.example.propertyms.building.model.Building;

public interface BuildingService {
    List<Building> listAll();

    long countAll();
}


