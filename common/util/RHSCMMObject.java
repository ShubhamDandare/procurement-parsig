package com.kpmg.rcm.sourcing.common.util;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import com.kpmg.rcm.sourcing.common.exception.RHSDocumentException;

/**
 * {@code RHSCMMObject} maps keys to values. {@code RHSCMMObject} cannot contain
 * duplicate keys; each key can map to at most one value.
 */	

public class RHSCMMObject implements Serializable {

    private String id;
    private String hashedId;
    private Document content;
    private Timestamp created;
    private Timestamp lastModified;

    /*
    Parent structure field names
     */
    public static final String SYSTEM = "system";
    public static final String FILES = "files";
    public static final String SUB_GRANULES_LIST = "subGranulesList";
    public static final String SUB_GRANULES = "subGranules";
    public static final String SOURCE = "source";
    public static final String ENRICHMENTS = "enrichments";
  //Addint Meta data header in json
    public static final String METADATA = "metaData";

    /*
    System specific fields
     */
    public static final String ID = SYSTEM + ".id";
    public static final String JURISDICTION = SYSTEM + ".jurisdiction";
    public static final String LOGICAL_SOURCE = SYSTEM + ".logicalSource";
    public static final String PROCESSING_SOURCE = SYSTEM + ".processingSource";
    public static final String TYPE = SYSTEM + ".type";
    public static final String COMMONID = SYSTEM +".commonId";
    public static final String VERSION = SYSTEM +".version";
    //Adding new field for JSON
    public static final String SOURCE_URL = SYSTEM+".sourceUrl";

    /*
    Non-indexable fields for content control
     */
    public static final String _HASHED_ID = "_id";
    public static final String _LAST_MODIFIED = "_lastModified";
    public static final String _CREATED = "_created";
    public static final String _KEY = "key";

    /*
    Source specific fields
     */
    public static final String ISLEAF = SOURCE + ".isLeaf";
    public static final String ISCURRENT = SOURCE + ".isCurrent";
    public static final String DATES = SOURCE +".dates";
    
    public static final String CHANGED = "changed";

    /**
     * Constructs an empty <tt>RHSCMMObject</tt> with the specified initial
     * capacity and load factor.
     */
    public RHSCMMObject() {
    	/**** Adding ISO date format***/
    	String isoDatePattern = "yyyy-MM-dd'T'HH:mm:ss:SSS'Z'";
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(isoDatePattern);

        content = new Document();
        created = new Timestamp(System.currentTimeMillis());
        
        String dateString = simpleDateFormat.format(new Date(created.getTime()));
        
        //content.put(_CREATED, new Date(created.getTime()));
        //content.put(_LAST_MODIFIED, new Date(created.getTime()));
        content.put(_CREATED, dateString);
        content.put(_LAST_MODIFIED, dateString);
    }

    /**
     * Constructs a <tt>RHSCMMObject</tt> with the specified id.
     *
     * @param id the id.
     *           Initializes the hashed id which will be used in Mongo for storing.
     */
    public RHSCMMObject(String id) {
        this();
        this.id = (id == null ? null : id.trim());
        if (this.id != null) {
            hashedId = SecurityUtils.SHA256Hash(this.id);
        }
    }

    /**
     * Constructs a <tt>RHSCMMObject</tt> with the specified content.
     *
     * @param content the content of the new <tt>RHSCMMObject</tt>.
     */
    public RHSCMMObject(Document content) {
        this();
        for (String key : content.keySet()) {
            switch (key) {
                case _KEY:
                    id = content.getString(key);
                    break;
                case _CREATED:
                    created = new Timestamp(content.getDate(key).getTime());
                    break;
                case _LAST_MODIFIED:
                    lastModified = new Timestamp(content.getDate(key).getTime());
                    break;
                case _HASHED_ID:
                    hashedId = content.getString(key);
                    break;
            }
        }
        id = (id == null) ? (String) get(ID) : id;
        hashedId = (hashedId == null && id != null) ? SecurityUtils.SHA256Hash(id) : hashedId;
        created = (created == null) ? new Timestamp(System.currentTimeMillis()) : created;
        lastModified = (lastModified == null) ? new Timestamp(System.currentTimeMillis()) : lastModified;
        content.put(_KEY, id);
        content.put(_HASHED_ID, hashedId);
        content.put(_CREATED, new Date(created.getTime()));
        content.put(_LAST_MODIFIED, new Date(created.getTime()));
        this.content = content;
    }

    /**
     * Returns the id.
     *
     * @return A string representing the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the hashed id.
     *
     * @return A string representing the hashed id.
     */
    public String getHashedId() {
        return hashedId;
    }

    /**
     * Returns the json representation.
     *
     * @return A string with the json representation.
     */
    public String toJson() {
        JsonWriterSettings writerSettings = new JsonWriterSettings(JsonMode.SHELL, true);
        String res;
        try {
            res = content.toJson(writerSettings);
            content.clear();
        } catch (CodecConfigurationException e) {
            res = String.format("{\"id\": \"%s\"}", getId());
        }
        return res;
    }

    /**
     * Returns a Document representation of the <tt>RHSCMMObject</tt>'s content.
     * @return A Document with the representation of the <tt>RHSCMMObject</tt>'s content.
     */
    public Document getContent() {
        return content;
    }

    /**
     * Returns the object specified with the fully qualified key stored in the <tt>RHSCMMObject</tt>.
     * @param key A string representing the fully qualified key to retrieve.
     * @return An object stored in the <tt>RHSCMMObject</tt> under the fully qualified key. Returns null if not present.
     */
    public Object get(String key) {
        return getAux(key.split("\\."), 0, null);
    }

    private Object getAux(String[] fullKey, int i, Object value) {
        if (i >= fullKey.length)
            return value;
        if (value != null && value instanceof Document) {
            if (((Document) value).get(fullKey[i]) != null) {
                return getAux(fullKey, i + 1, ((Document) value).get(fullKey[i]));
            }
        }
        if (content.get(fullKey[i]) != null)
            return getAux(fullKey, i + 1, content.get(fullKey[i]));
        else {
            return content.get(fullKey[i]);
        }
    }

    /**
     * Sets the id for the <tt>RHSCMMObject</tt>.
     * @param id A string representing the id of the <tt>RHSCMMObject</tt>.
     * @throws RHSDocumentException
     */
    public void setId(String id) throws RHSDocumentException {
        if (id == null && id.isEmpty()) {
            throw new RHSDocumentException("Id is null", "");
        }
        this.id = id;
        hashedId = SecurityUtils.SHA256Hash(id);
        lastModified = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Returns the number of fields in the <tt>RHSCMMObject</tt>'s content.
     * @return An integer representing the number of fields in the <tt>RHSCMMObject</tt>'s content.
     */
    public int size() {
        return content.size();
    }

    /**
     * Returns
     * @return
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public void wasModified() {
        lastModified = new Timestamp(System.currentTimeMillis());
        content.put(_LAST_MODIFIED, new Date(lastModified.getTime()));
    }

    public Timestamp getLastModified() {
        if (lastModified == null) {
            lastModified = (Timestamp) content.get(_LAST_MODIFIED);
        }
        return lastModified;
    }

    public Object get(Object key) {
        return get((String) key);
    }

    public Object put(String key, Object value) {
        if (value instanceof Timestamp) {
            value = new Date(((Timestamp) value).getTime());
        }
        if (key.equalsIgnoreCase(ID)) {
            id = (String) value;
            hashedId = SecurityUtils.SHA256Hash(id);
            content.put(_KEY, id);
            content.put(_HASHED_ID, hashedId);
        }
        String[] keys = key.split("\\.");
        if (keys.length == 1)
            putAux(null, keys, 0, value);
        else
            putAux(key.substring(0, key.lastIndexOf(".")), keys, keys.length - 1, value);
        return value;
    }

    public void clear() {
        content.clear();
    }

    public Set<String> keySet() {
        return content.keySet();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return content.entrySet();
    }

    private void putAux(String key, String[] keys, int i, Object value) {
        if (key == null) {
            content.put(keys[0], value);
            return;
        }
        Object obj = get(key);
        if (obj != null) {
            if (obj instanceof Document) {
                ((Document) obj).put(keys[i], value);
                return;
            }
        }
        Document temp = new Document();
        temp.putIfAbsent(keys[i], value);
        String keyAux = key.lastIndexOf(".") > 0 ? key.substring(0, key.lastIndexOf(".")) : null;
        putAux(keyAux, keys, i - 1, temp);
    }

    public Document getSystem() {
        return (Document) get(SYSTEM);
    }
    
    
    public Document getSource() {
        return (Document) get(SOURCE);
    }

    public void setSystem(Document system) {
        put(SYSTEM, system);
    }
    
    public void setSource(Document source) {
        put(SOURCE, source);
    }

    public Document getFiles() {
        return (Document) get(FILES);
    }

    public void setFiles(Document files) {
        put(FILES, files);
    }

    public Document getSubGranulesList() {
        return (Document) get(SUB_GRANULES_LIST);
    }

    public void setSubGranulesList(Document subGranulesList) {
        put(SUB_GRANULES_LIST, subGranulesList);
    }

    public Document getSubGranules() {
        return (Document) get(SUB_GRANULES);
    }

    public void setSubGranules(Document subGranules) {
        put(SUB_GRANULES, subGranules);
    }

    public Document getSubGranule(String id) {
        return (Document) get(SUB_GRANULES +"." + id);
    }

    public void addSubGranule(String id, Document subGranule) {
        put(SUB_GRANULES + "." + id, subGranule);
    }

    public Document getEnrichments() {
        return (Document) get(ENRICHMENTS);
    }

    public void setEnrichments(Document enrichments) {
        put(ENRICHMENTS, enrichments);
    }

    public Document getEnrichment(String id) {
        return (Document) get(ENRICHMENTS + "." + id);
    }

    public void addEnrichment(String id, Document enrichment) {
        put(ENRICHMENTS + "." + id, enrichment);
    }

    @Override
    public String toString() {
        return toJson();
    }

}
