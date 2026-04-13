package org.example.propertyms.user.service;

import java.util.List;
import org.example.propertyms.user.model.DepartmentOption;

public interface DepartmentService {
    List<DepartmentOption> listEnabled();

    boolean isEnabledCode(String code);
}

