package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.model.Resident;

public interface ResidentService {
    List<Resident> list(String keyword, String status);

    Resident findById(Long id);

    void save(Resident resident);

    void deleteById(Long id);
}
