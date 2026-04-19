package com.nazir.myownai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse {
    private String answer;
    private String model;
    private List<SearchContextResponse> contexts;
    private int docCount;
}