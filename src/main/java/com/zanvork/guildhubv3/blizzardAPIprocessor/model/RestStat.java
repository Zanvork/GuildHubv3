package com.zanvork.guildhubv3.blizzardAPIprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestStat {
    private int stat;
    private int amount;
}