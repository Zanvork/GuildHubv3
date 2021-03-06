package com.zanvork.guildhubv3.services;

import com.zanvork.battlenet.model.RestCharacter;
import com.zanvork.battlenet.model.RestCharacterItems;
import com.zanvork.battlenet.model.RestCharacterTalents;
import com.zanvork.battlenet.model.RestItem;
import com.zanvork.battlenet.service.RestObjectNotFoundException;
import com.zanvork.battlenet.service.WarcraftAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.zanvork.guildhubv3.model.WarcraftCharacter;
import com.zanvork.guildhubv3.model.CharacterItem;
import com.zanvork.guildhubv3.model.CharacterSpec;
import com.zanvork.guildhubv3.model.User;
import com.zanvork.guildhubv3.model.WarcraftCharacterVerificationRequest;
import com.zanvork.guildhubv3.model.dao.WarcraftCharacterDAO;
import com.zanvork.guildhubv3.model.dao.WarcraftCharacterVerificationRequestDAO;
import com.zanvork.guildhubv3.model.enums.ItemSlots;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author zanvork
 */
@Service
public class CharacterService extends OwnedEntityBackendService<WarcraftCharacter>{
    //Battlenet Services
    @Autowired
    private WarcraftAPIService apiService;
    
    //Backend Service
    @Autowired
    private DataService dataService;
    
    @Autowired
    private WarcraftCharacterDAO dao; 
    @Autowired
    private WarcraftCharacterVerificationRequestDAO verificationRequestsDAO;
    
    private final Logger log  =   LoggerFactory.getLogger(this.getClass());
   
    private Map<Long, WarcraftCharacterVerificationRequest> verificationRequests =   new HashMap<>();
    
    private final Object verificationRequestsLock  =   new Object();
    
    /**
     * Get a character from a name, realm and region.
     * Tries to load the specified character from the cache, if it does not exist
     * then it will create the character.
     * @param name name of the character
     * @param realm realm name the character is on
     * @param region region the realm is in
     * @return character loaded or created
     */
    public WarcraftCharacter getCharacter(String name, String realm, String region)
            throws EntityNotFoundException {
        
        String key                  =   WarcraftCharacter.characterNameRealmRegionToKey(name, realm, region);
        WarcraftCharacter character =   getEntity(key);
        return character;
    }
    
    /**
     * Create a new character from a name, region and realm.
     * @param name name of the character.
     * @param realm realm the character is on
     * @param region region the realm is in
     * @return the new character
     */
    public WarcraftCharacter createCharacter(String name, String realm, String region)
            throws RestObjectNotFoundException, EntityAlreadyExistsException {
        
        return createCharacter(null, name, realm, region, false);
    }
    /**
     * Create a new character from a name, region and realm.
     * @param user
     * @param name name of the character.
     * @param realm realm the character is on
     * @param region region the realm is in
     * @param updateDetails whether to load data from the rest API to update
     * @return the new character
     */
    public WarcraftCharacter createCharacter(User user, String name, String realm, String region, boolean updateDetails)
            throws RestObjectNotFoundException, EntityAlreadyExistsException {
        
        String key  =   WarcraftCharacter.characterNameRealmRegionToKey(name, realm, region);
        if (entityExists(key)){
            throw new EntityAlreadyExistsException(
                    "Could not create character with key '" + key + "', a character with that key already exists"
            );
        }
        
        WarcraftCharacter character =   new WarcraftCharacter();
        character.setName(name);
        character.setRealm(dataService.getRealm(realm, region));
        if (updateDetails){
            character.setOwner(user);
            RestCharacter characterData =   apiService.getCharacter(region, realm, name);
            updateCharacter(character, characterData);
        }
        
        return character;
    }
    
    /**
     * Updates a character from a name, region and realm.
     * @param userId
     * @param id
     * @return the updated character
     */
    public WarcraftCharacter updateCharacter(long userId, long id)
            throws EntityNotFoundException, RestObjectNotFoundException,ReadOnlyEntityException, NotAuthorizedException{
        
        WarcraftCharacter character =   getEntity(id);
        
        userCanEditEntity(userId, character);
        RestCharacter characterData =   apiService.getCharacter(character.getRealm().getRegionName(), character.getRealm().getName(), character.getName());
        updateCharacter(character, characterData);
        return character;
    }
    
   
    /**
     * Updates character information from a RestCharacter object.
     * @param character WarcraftCharacter to update
     * @param characterData RestCharacter to load information from
     */
    private void updateCharacter(WarcraftCharacter character, RestCharacter characterData){
        character.setName(characterData.getName());
        character.setCharacterClass(dataService.getCharacterClass(characterData.getCharClass()));
        updateCharacterSpec(character, characterData.getTalents());

        character.setAverageItemLevel(characterData.getItems().getAverageItemLevel());
        setCharacterItems(character, characterData.getItems());

        saveEntity(character);
    }
    
    /**
     * Sets the character's specs in the character object provided.
     * @param character character object to set the specs on
     * @param talentsData talent data to read
     */
    private void updateCharacterSpec(WarcraftCharacter character, List<RestCharacterTalents> talentsData){
        long classId    =   character.getCharacterClass().getId();
        String[] specNames  =   new String[2];
        int mainSpecId  =   0;
        specNames[0]    =   talentsData.get(0).getSpec().getName();
        if (talentsData.size() > 1 && talentsData.get(1).getSpec() != null){
            specNames[1]    =   talentsData.get(1).getSpec().getName();
            if (talentsData.get(1).isSelected()){
                mainSpecId  =   1;
            } 
            CharacterSpec offSpec   =   dataService.getCharacterSpec(classId, specNames[1 - mainSpecId]);
            character.setOffSpec(offSpec);
        } else {
            
        }
        CharacterSpec mainSpec  =   dataService.getCharacterSpec(classId, specNames[mainSpecId]);
        character.setMainSpec(mainSpec);
    }
    
    /**
     * Set the character's item in the character object provided.
     * @param character character object to set the items in
     * @param itemsData item data to read
     */
    private void setCharacterItems(WarcraftCharacter character, RestCharacterItems itemsData){
        Map<ItemSlots, CharacterItem> characterItems    =   new HashMap<>();
        List<CharacterItem> newItems                    =   new ArrayList<>();
        if (character.getItems() != null){
            character.getItems().stream().forEach((item) -> {
                characterItems.put(item.getSlot(), item);
            });
        }
        itemsData.getItems().entrySet().stream()
                .filter(entry -> 
                        entry.getValue() != null
                )
                .map(entry -> {
                    ItemSlots itemSlot  =   ItemSlots.valueOf(entry.getKey().toUpperCase());
                    RestItem itemData   =   entry.getValue();
                    CharacterItem item  =   new CharacterItem();
                    if (characterItems.containsKey(itemSlot)){
                        item    =   characterItems.remove(itemSlot);
                    }
                    item.setSlot(itemSlot);
                    item.setOwner(character);
                    item.setBlizzardID(itemData.getId());
                    item.setItemLevel(itemData.getItemLevel());
                    return item;
                })
                .forEach(item -> {
                    newItems.add(item);
                });
        character.setItems(newItems);
    }
    
    
    public WarcraftCharacterVerificationRequest takeOwnershipViaVerfication(long userId, long characterId){
        
        User user       =   userService.getUser(userId);
        Random random   =   new Random();
        WarcraftCharacterVerificationRequest verificationRequest   =   new WarcraftCharacterVerificationRequest();
        WarcraftCharacter character =   getEntity(characterId);
        
        try {
            canChangeEntityOwner(userId, character);
        } catch (NotAuthorizedException e){
            //We don't care about not authorized exceptions, but other exceptions still matter
        }
        
        verificationRequest.setDateCreated(new Date());
        verificationRequest.setRequesterId(user.getId());
        verificationRequest.setSubjectId(character.getId());
        verificationRequest.setSlot(character.getItems().get(random.nextInt(character.getItems().size())).getSlot());
        saveVerificationRequest(verificationRequest);
        
        return verificationRequest;
    }
    
    public WarcraftCharacter checkVerificationRequest(long userId, long characterId, long verificationRequestId){
        
        User user       =   userService.getUser(userId);
        WarcraftCharacterVerificationRequest verificationRequest    =   getVerificationRequest(verificationRequestId);
        WarcraftCharacter character =   getEntity(verificationRequest.getSubjectId());
        if (character.getId() != characterId){
            throw new UnexpectedEntityException(
                    "Character id expected ('" + characterId + 
                    "') does not match the id in the verification request ('" + 
                    character.getId() + "')."
            );
        }
        if (userId != verificationRequest.getRequesterId()){
            throw new NotAuthorizedException(
                    "Requesting user ('" + user.getId() + 
                    "') does not match the id in the verification request ('" + 
                    verificationRequest.getRequesterId() + "')."
            );
            
        }
        RestCharacter characterData =   apiService.getCharacter(character.getRealm().getRegionName(), character.getRealm().getName(), character.getName());
        Map<String, RestItem> items =   characterData.getItems().getItems();
        String slotName             =   verificationRequest.getSlot().name().toLowerCase();
        if (items.containsKey(slotName) && items.get(slotName) != null){
            throw new NotAuthorizedException(
                    "Verification failed, character with id '" + characterId + 
                    "' could not be verified, itemSlot '" +
                    verificationRequest.getSlot().name().toLowerCase() + "' still equipped."
            );
        }
        character.setOwner(user);
        saveEntity(character);
        deleteVerificationRequest(verificationRequest);
        
        return character;
        
    }
    
    /**
     * Takes a character and generates a unique string key.
     * @param character
     * @return a unique key
     */
    @Override
    public String entityToKey(WarcraftCharacter character){
        String key  =   "null";
        if (character != null){
            key = character.getKey();
        }
        return key;
    }
    
    
    public WarcraftCharacterVerificationRequest getVerificationRequest(long id)
            throws EntityNotFoundException{
        
        WarcraftCharacterVerificationRequest verificationRequest;
        synchronized(verificationRequestsLock){
            verificationRequest = verificationRequests.get(id);
        }
        if (verificationRequest == null){
            throw new EntityNotFoundException(
                    "Could not load WarcraftCharacterVerificationRequest with id '" + id + "'."
            );
        }
        return verificationRequest;
    }
    
    
    /**
     * Store all objects currently cached in service.
     */
    @Scheduled(fixedDelay=TIME_15_SECOND)
    @Override
    public void updateToBackend(){
        
    }
    /**
     * Loads object from the backend database into memory.
     */
    @Scheduled(fixedDelay=TIME_15_SECOND)
    @Override
    public void updateFromBackend(){
        super.updateFromBackend();
        loadCharacterVerificationRequestsFromBackend();
    }

    
    public void saveVerificationRequest(WarcraftCharacterVerificationRequest verificationRequest){
        verificationRequestsDAO.save(verificationRequest);
       
        synchronized(verificationRequestsLock){
            verificationRequests.put(verificationRequest.getId(), verificationRequest);
        }
    }
    
    public void deleteVerificationRequest(WarcraftCharacterVerificationRequest verificationRequest){
        verificationRequestsDAO.delete(verificationRequest);
       
        synchronized(verificationRequestsLock){
            verificationRequests.remove(verificationRequest.getId());
        }
    }
    
    @Override
    protected void saveEntity(WarcraftCharacter entity) throws HibernateException{
        dao.save(entity);
       
        synchronized(entitiesLock){
            entities.put(entity.getId(), entity);
        }
        synchronized(entitiesByNameLock){
            entitiesByName.put(entityToKey(entity), entity);
        }
    }

    @Override
    protected void loadEntitiesFromBackend() {
        Map<Long, WarcraftCharacter> newEntities          =   new HashMap<>();
        Map<String, WarcraftCharacter> newEntitiesByName  =   new HashMap<>();
        dao.findAll().forEach(entity -> {
            newEntities.put(entity.getId(), entity);
            newEntitiesByName.put(entityToKey(entity), entity);
        });
        synchronized (entitiesLock){
            entities    =   newEntities;
        }
        synchronized (entitiesByNameLock){
            entitiesByName    =   newEntitiesByName;
        }
    }
    
    protected void loadCharacterVerificationRequestsFromBackend() {
        Map<Long, WarcraftCharacterVerificationRequest> newVerificationRequesrs          =   new HashMap<>();
        
        Calendar cal    =   Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);
        Date yesterday  =   cal.getTime();
        verificationRequestsDAO.findAll().forEach(verificationRequest -> {
            if (verificationRequest.getDateCreated().after(yesterday)){
                newVerificationRequesrs.put(verificationRequest.getId(), verificationRequest);
            } else {
                deleteVerificationRequest(verificationRequest);
            }
        });
        synchronized (verificationRequestsLock){
            verificationRequests    =   newVerificationRequesrs;
        }
    }
    
    

}
