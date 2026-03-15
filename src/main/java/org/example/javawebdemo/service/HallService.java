package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.model.Hall;

public interface HallService {
    List<Hall> listAll();

    Hall getById(Long id);

    void create(Hall hall);

    void update(Hall hall);

    void delete(Long id);
}
