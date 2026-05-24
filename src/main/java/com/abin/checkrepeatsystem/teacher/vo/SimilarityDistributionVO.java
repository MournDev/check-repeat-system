package com.abin.checkrepeatsystem.teacher.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityDistributionVO {
    private String range;
    private Integer paperCount;
    private BigDecimal percentage;
}
