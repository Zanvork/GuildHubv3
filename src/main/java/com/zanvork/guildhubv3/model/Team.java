package com.zanvork.guildhubv3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zanvork.guildhubv3.model.enums.Regions;
import com.zanvork.guildhubv3.services.CharacterService;
import com.zanvork.guildhubv3.services.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 * @author zanvork
 */
@Data
@EqualsAndHashCode(exclude="membersMap")
@ToString(exclude="membersMap")
@JsonIgnoreProperties("membersMap")
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"name", "region"}))
public class Team {
    
    @Id
    @GeneratedValue
    private long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('EU', 'US', 'KR', 'TW')")
    private Regions region;
    
    @OneToMany(mappedBy = "team", fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    private List<TeamMember> members    =   new ArrayList<>();
    
    @Transient
    private Map<Long, TeamMember> membersMap  =   new HashMap<>();
    
    
    public void addMember(TeamMember member){
        member.setTeam(this);
        members.add(member);
    }
    
    public boolean hasMember(long id){  if (membersMap.size() != members.size()){
            updateMembersMap();
        }
        return membersMap.containsKey(id);
    }
    
    public TeamMember getMember(long id)
            throws EntityNotFoundException{
        if (membersMap.size() != members.size()){
            updateMembersMap();
        }
        TeamMember  member  =   membersMap.get(id);
        if (member == null){
            EntityNotFoundException e   =   new EntityNotFoundException(
                    "Team '" + name + "' on region '" + region.name() + "' does not contain a member with id '" + id + "'."
            );
            throw e;
        }
        return member;
    }
    
    private void updateMembersMap(){
        members.forEach((member) -> {
            membersMap.put(member.getId(), member);
        });
    }
    
    @ManyToOne
    private User owner;
    
    private boolean ownershipLocked =   false;
    
    private boolean readOnly    =   false;
}
