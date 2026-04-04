package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.model.Resident;

public interface ResidentService {
    List<Resident> list(String keyword, String status);

    PageResult<Resident> listPaged(String keyword, String status, int page, int pageSize);

    Resident findById(Long id);

    void save(Resident resident);

    void deleteById(Long id);
}
