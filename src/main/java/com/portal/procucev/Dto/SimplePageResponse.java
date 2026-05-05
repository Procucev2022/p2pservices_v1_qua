package com.portal.procucev.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplePageResponse<T> {

    private long totalRecords;
    private List<T> data;
}