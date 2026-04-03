package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.BuildingMapper;
import org.example.javawebdemo.model.Building;
import org.example.javawebdemo.service.BuildingService;
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
}
