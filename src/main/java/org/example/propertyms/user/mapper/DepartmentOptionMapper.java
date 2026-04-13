package org.example.propertyms.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.propertyms.user.model.DepartmentOption;

@Mapper
public interface DepartmentOptionMapper {

    @Select("""
            SELECT code, label, sort_order, enabled
            FROM system_departments
            WHERE enabled = 1
            ORDER BY sort_order ASC, code ASC
            """)
    List<DepartmentOption> findEnabled();

    @Select("""
            SELECT COUNT(*)
            FROM system_departments
            WHERE enabled = 1 AND code = #{code}
            """)
    int countEnabledByCode(@Param("code") String code);
}

