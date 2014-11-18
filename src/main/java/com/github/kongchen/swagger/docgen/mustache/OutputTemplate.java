package com.github.kongchen.swagger.docgen.mustache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.lang.StringUtils;

import scala.collection.JavaConversions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.github.kongchen.swagger.docgen.AbstractDocumentSource;
import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSourceInfo;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.google.common.collect.Maps;
import com.sun.media.sound.ModelConnectionBlock;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.model.Model;

/**
 * Created with IntelliJ IDEA.
 * User: kongchen
 * Date: 3/7/13
 */
public class OutputTemplate {
    private String basePath;

    private String apiVersion;
    
    private ApiSourceInfo apiInfo;

    private List<MustacheDocument> apiDocuments = new ArrayList<MustacheDocument>();

    private Set<MustacheDataType> dataTypes = new TreeSet<MustacheDataType>();
    private Map<String, MustacheDataType> dataTypesMap = new HashMap<String, MustacheDataType>();

    public OutputTemplate(AbstractDocumentSource docSource) {
        feedSource(docSource);
    }

    public static String getJsonSchema() {
        ObjectMapper m = new ObjectMapper();
        try {
            JsonSchema js = m.generateJsonSchema(OutputTemplate.class);
            return m.writeValueAsString(js);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
	public Map<String, MustacheDataType> getDataTypesMap() {
    	if (dataTypesMap.isEmpty()) { 
    		for (MustacheDataType dataType : dataTypes) {
    			dataTypesMap.put(dataType.getName(), dataType);
    		}
    	}
    	return LazyMap.decorate(dataTypesMap, new Transformer() {
			@Override
			public Object transform(Object input) {
				if (input != null) {
					String className = input.toString();
					String generic = StringUtils.substringBetween(className, "[", "]");
					if (StringUtils.isNotBlank(generic)) {
						className = generic;
					}
					
					Class<?> clazz = classForName(className);
					if (clazz != null) {
						scala.collection.immutable.List<Model> models = ModelConverters.readAll(clazz);
						
						List<Model> modelsList = JavaConversions.asJavaList(models);
						for (Model model : modelsList) {
							if (model.id().equals(className)) {
								Map<String, Model> modelsMap = Maps.newHashMap();
								modelsMap.put(className, model);
								apiDocuments.get(0).addModels(modelsMap);
								return new MustacheDataType(apiDocuments.get(0), className);
							}
						}
					}
				}
				return null;
			}
    	});
    }

    private Class<?> classForName(String className) {
    	final Package[] packages = Package.getPackages();

        for (final Package p : packages) {
            final String pack = p.getName();
            final String tentative = pack + "." + className;
            try {
                return Class.forName(tentative);
            } catch (final ClassNotFoundException e) {
                continue;
            }
        }
        
        return null;
    }
    
    public Set<MustacheDataType> getDataTypes() {
        return dataTypes;
    }

    public void addDateType(MustacheDocument mustacheDocument, MustacheDataType dataType) {
        if (dataTypes.contains(dataType)) {
            return;
        }
        dataTypes.add(dataType);
        for (MustacheItem item : dataType.getItems()) {
            String trueType = TypeUtils.getTrueType(item.getType());
            if (trueType == null) {
                continue;
            }
            addDateType(mustacheDocument, new MustacheDataType(mustacheDocument, trueType));
        }
    }

    public List<MustacheDocument> getApiDocuments() {
        return apiDocuments;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public ApiSourceInfo getApiInfo() {
        return apiInfo;
    }

    public void setApiInfo(ApiSourceInfo apiInfo) {
        this.apiInfo = apiInfo;
    }

    /**
     * Create mustache document according to a swagger document apilisting
     * @param swaggerDoc
     * @return
     */
    private MustacheDocument createMustacheDocument(ExtendedApiListing swaggerDoc) {
        MustacheDocument mustacheDocument = new MustacheDocument(swaggerDoc);
        
        for (Iterator<ExtendedApiDescription> it = swaggerDoc.apis().iterator(); it.hasNext(); ) {
        	ExtendedApiDescription api = it.next();
            mustacheDocument.setDescription(Utils.getStrInOption(api.description()));

            MustacheApi mustacheApi = new MustacheApi(swaggerDoc.basePath(), api);

            for (Iterator<ExtendedOperation> opIt  = api.operations().iterator(); opIt.hasNext(); ) {
            	ExtendedOperation op = opIt.next();
                MustacheOperation mustacheOperation = null;
                mustacheOperation = new MustacheOperation(mustacheDocument, op);
                mustacheApi.addOperation(mustacheOperation);
                addResponseType(mustacheDocument, mustacheOperation.getResponseClass());
                for (MustacheResponseClass responseClass : mustacheOperation.getResponseClasses()) {
                    addResponseType(mustacheDocument, responseClass);
                }
            }

            mustacheDocument.addApi(mustacheApi);
        }

        for (String requestType : mustacheDocument.getRequestTypes()) {
            MustacheDataType dataType = new MustacheDataType(mustacheDocument, requestType);

            addDateType(mustacheDocument, dataType);
        }

        Set<String> missedTypes = new LinkedHashSet<String>();

        for (String responseType : mustacheDocument.getResponseTypes()) {
            if (!mustacheDocument.getRequestTypes().contains(responseType)) {
                String ttype = TypeUtils.getTrueType(responseType);
                if (ttype != null) {
                    missedTypes.add(ttype);
                }
            }
        }

        for (String type : missedTypes) {
            MustacheDataType dataType = new MustacheDataType(mustacheDocument, type);
            addDateType(mustacheDocument, dataType);
        }
        filterDatatypes(dataTypes);

        return mustacheDocument;
    }

    private void filterDatatypes(Set<MustacheDataType> dataTypes) {
        Iterator<MustacheDataType> it = dataTypes.iterator();
        while (it.hasNext()){
            MustacheDataType type = it.next();

            if (type.getItems() == null || type.getItems().size() == 0) {
                it.remove();
            }
        }
    }

    private void addResponseType(MustacheDocument mustacheDocument, MustacheResponseClass responseClass) {
        mustacheDocument.addResponseType(responseClass);
        if (responseClass.getGenericClasses() != null) {
            for (MustacheResponseClass mrc : responseClass.getGenericClasses()){
                addResponseType(mustacheDocument, mrc);
            }
        }
    }

    private void feedSource(AbstractDocumentSource source) {
        setApiVersion(source.getApiVersion());
        setBasePath(source.getBasePath());
        setApiInfo(source.getApiInfo());
        for (ExtendedApiListing doc : source.getValidDocuments()) {
            if (doc.apis().isEmpty()){
                continue;
            }
            MustacheDocument mustacheDocument = createMustacheDocument(doc);
            addMustacheDocument(mustacheDocument);
        }
        handleAllZeroIndex();
        Collections.sort(apiDocuments, new Comparator<MustacheDocument>() {
            @Override
            public int compare(MustacheDocument o1, MustacheDocument o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
    }

    private void addMustacheDocument(MustacheDocument mustacheDocument) {
        apiDocuments.add(mustacheDocument);
    }

    public void setDataTypes(Set<MustacheDataType> dataTypes) {
        this.dataTypes = dataTypes;
    }

    private void handleAllZeroIndex() {
        if (apiDocuments.size() < 2) {
            // only 1, index doesn't matter
            return;
        }
        if (apiDocuments.get(0).getIndex() != apiDocuments.get(1).getIndex()) {
            // different indexs, no special handling required
            return;
        }
        Collections.sort(apiDocuments);
        int i = 0;
        for (MustacheDocument apiDocument : apiDocuments) {
            apiDocument.setIndex(i); // requires delete of final modifier
            i++;
        }

    }
}
