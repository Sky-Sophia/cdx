package org.example.propertyms.building.service;

import java.util.List;
import org.example.propertyms.building.mapper.BuildingMapper;
import org.example.propertyms.building.model.Building;
import org.springframework.stereotype.Service;

@Service
public class BuildingServiceImpl implements BuildingService {
    private final BuildingMapper buildingMapper;

    public BuildingServiceImpl(BuildingMapper buildingMapper) {
        this.buildingMapper = buildingMapper;
    }

    @Override
    public List<Building> listAll() {
        return buildingMapper.findAll();
    }

    @Override
    public long countAll() {
        return buildingMapper.countAll();
    }
}

