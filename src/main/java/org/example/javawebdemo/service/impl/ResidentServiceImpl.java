package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.mapper.ResidentMapper;
import org.example.javawebdemo.model.Resident;
import org.example.javawebdemo.service.ResidentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResidentServiceImpl implements ResidentService {
    private final ResidentMapper residentMapper;

    public ResidentServiceImpl(ResidentMapper residentMapper) {
        this.residentMapper = residentMapper;
    }

    @Override
    public List<Resident> list(String keyword, String status) {
        return residentMapper.findAll(keyword, status);
    }

    @Override
    public PageResult<Resident> listPaged(String keyword, String status, int page, int pageSize) {
        long total = residentMapper.count(keyword, status);
        int offset = PageResult.calcOffset(page, pageSize);
        List<Resident> items = residentMapper.findAllPaged(keyword, status, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
    }

    @Override
    public Resident findById(Long id) {
        return residentMapper.findById(id);
    }

    @Override
    @Transactional
    public void save(Resident resident) {
        if (resident == null || resident.getUnitId() == null || isBlank(resident.getName())) {
            throw new IllegalArgumentException("请填写房屋和住户姓名。");
        }
        if (isBlank(resident.getStatus())) {
            resident.setStatus("ACTIVE");
        }
        if (isBlank(resident.getResidentType())) {
            resident.setResidentType("OWNER");
        }
        if (resident.getId() == null) {
            residentMapper.insert(resident);
        } else {
            residentMapper.update(resident);
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        residentMapper.deleteById(id);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
