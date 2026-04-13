package org.example.propertyms.user.model;

import lombok.Data;

@Data
public class DepartmentOption {
    private String code;
    private String label;
    private Integer sortOrder;
    private Integer enabled;
}

