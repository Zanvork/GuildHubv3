/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zanvork.guildhubv3.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

/**
 *
 * @author jgreeman
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestClasses {
    private List<RestClass> classes;
}

