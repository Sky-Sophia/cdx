package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.HallMapper;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.service.HallService;
import org.springframework.stereotype.Service;

@Service
public class HallServiceImpl implements HallService {
    private final HallMapper hallMapper;

    public HallServiceImpl(HallMapper hallMapper) {
        this.hallMapper = hallMapper;
    }

    @Override
    public List<Hall> listAll() {
        return hallMapper.findAll();
    }

    @Override
    public Hall getById(Long id) {
        return hallMapper.findById(id);
    }

    @Override
    public void create(Hall hall) {
        hallMapper.insert(hall);
    }

    @Override
    public void update(Hall hall) {
        hallMapper.update(hall);
    }

    @Override
    public void delete(Long id) {
        hallMapper.deleteById(id);
    }
}
