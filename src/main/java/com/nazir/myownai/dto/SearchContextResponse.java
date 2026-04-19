package com.nazir.myownai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchContextResponse {
    private int id;
    private String title;
    private String text;
    private float distance;
}