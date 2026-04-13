package org.example.propertyms.user.service;

import java.util.List;
import java.util.Locale;
import org.example.propertyms.user.mapper.DepartmentOptionMapper;
import org.example.propertyms.user.model.DepartmentOption;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentOptionMapper departmentOptionMapper;

    public DepartmentServiceImpl(DepartmentOptionMapper departmentOptionMapper) {
        this.departmentOptionMapper = departmentOptionMapper;
    }

    @Override
    public List<DepartmentOption> listEnabled() {
        return departmentOptionMapper.findEnabled();
    }

    @Override
    public boolean isEnabledCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return departmentOptionMapper.countEnabledByCode(code.trim().toUpperCase(Locale.ROOT)) > 0;
    }
}

