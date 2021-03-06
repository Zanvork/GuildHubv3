package com.zanvork.guildhubv3.model;

import com.zanvork.guildhubv3.model.enums.Regions;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

/**
 *
 * @author zanvork
 */

@Data
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"slug", "region"}))
public class Realm implements Serializable {
    
    @Id 
    @GeneratedValue 
    private long id; 
    
    @Column(nullable = false)
    private String name; 
    
    @Column(nullable = false)
    private String slug;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('EU', 'US', 'KR', 'TW')")
    private Regions region;
    
    
    public String getRegionName(){
        return region.name();
    }
    
    public String getKey(){
        return realmNameRegionToKey(name, region.name());
    }
    
    public static String realmNameRegionToKey(String realmName, String region){
        String key  =   "null";
        if (realmName != null && region != null){
            key =   realmName.toLowerCase() + "_" + region.toLowerCase();
        } 
        return key;        
    }
    
}
