package org.example.propertyms.resident.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.Map;
import org.example.propertyms.resident.mapper.ResidentMapper;
import org.example.propertyms.resident.model.Resident;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@ExtendWith(MockitoExtension.class)
class ResidentServiceImplTest {

    @Mock
    private ResidentMapper residentMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ResidentServiceImpl residentService;

    @Test
    void save_shouldClearMoveOutDateForActiveResident() {
        Resident resident = new Resident();
        resident.setUnitId(8L);
        resident.setName("张三");
        resident.setStatus("ACTIVE");
        resident.setMoveInDate(LocalDate.of(2026, 4, 1));
        resident.setMoveOutDate(LocalDate.of(2026, 4, 5));
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(Map.of("id", 88L));
            return 1;
        }).when(jdbcTemplate).update(any(), any(KeyHolder.class));

        residentService.save(resident);

        assertNull(resident.getMoveOutDate());
        verify(jdbcTemplate).update(any(), any(KeyHolder.class));
        verify(residentMapper).insert(resident);
    }

    @Test
    void save_shouldRejectMoveOutDateEarlierThanMoveInDate() {
        Resident resident = new Resident();
        resident.setUnitId(9L);
        resident.setName("李四");
        resident.setStatus("MOVED_OUT");
        resident.setMoveInDate(LocalDate.of(2026, 4, 10));
        resident.setMoveOutDate(LocalDate.of(2026, 4, 1));

        assertThrows(IllegalArgumentException.class, () -> residentService.save(resident));

        verifyNoInteractions(residentMapper);
    }
}



