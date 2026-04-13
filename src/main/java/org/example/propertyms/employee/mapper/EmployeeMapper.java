package org.example.propertyms.employee.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    @Select("""
            SELECT id
            FROM employees
            WHERE account_id = #{accountId}
              AND status = 'ACTIVE'
            LIMIT 1
            """)
    Long findActiveEmployeeIdByAccountId(@Param("accountId") Long accountId);
}
