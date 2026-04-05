package org.example.propertyms.resident.service;

import java.util.List;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.resident.mapper.ResidentMapper;
import org.example.propertyms.resident.model.Resident;
import org.example.propertyms.resident.model.ResidentStatus;
import org.example.propertyms.resident.model.ResidentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResidentServiceImpl implements ResidentService {
    private final ResidentMapper residentMapper;

    public ResidentServiceImpl(ResidentMapper residentMapper) {
        this.residentMapper = residentMapper;
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
        if (resident == null || resident.getUnitId() == null || StringHelper.isBlank(resident.getName())) {
            throw new IllegalArgumentException("请填写房屋和住户姓名。");
        }
        if (StringHelper.isBlank(resident.getStatus())) {
            resident.setStatus(ResidentStatus.ACTIVE.name());
        }
        if (StringHelper.isBlank(resident.getResidentType())) {
            resident.setResidentType(ResidentType.OWNER.name());
        }
        if (resident.getMoveInDate() != null
                && resident.getMoveOutDate() != null
                && resident.getMoveOutDate().isBefore(resident.getMoveInDate())) {
            throw new IllegalArgumentException("迁出日期不能早于入住日期。");
        }
        if (ResidentStatus.ACTIVE.name().equals(resident.getStatus())) {
            resident.setMoveOutDate(null);
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

    @Override
    public long countActive() {
        return residentMapper.countActive();
    }

    @Override
    public long countOccupiedUnits() {
        return residentMapper.countOccupiedUnits();
    }
}

