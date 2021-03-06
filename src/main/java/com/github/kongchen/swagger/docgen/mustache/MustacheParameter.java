package com.github.kongchen.swagger.docgen.mustache;


import static com.github.kongchen.swagger.docgen.TypeUtils.getTrueType;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kongchen.swagger.docgen.util.Utils;

public class MustacheParameter {
    private final String allowableValue;

    private final String access;

    private final String defaultValue;

    private String name;

    private final boolean required;

    private final String description;

    private final String type;

    private final String linkType;

	private Map<String, Map<String, Object>> annotations;

    public MustacheParameter(ExtendedParameter para) {
        this.name = para.name();
        if (para.allowMultiple()) { //TODO
        	this.linkType = "List[" + getTrueType(para.dataType()) + "]";
        } else {
        	this.linkType = getTrueType(para.dataType());
        }
        this.required = para.required();
        this.description = para.description();
        this.type = para.dataType();
        this.defaultValue = Utils.getStrInOption(para.defaultValue());
        this.allowableValue = Utils.allowableValuesToString(para.allowableValues());
        this.access = Utils.getStrInOption(para.paramAccess());
        this.annotations = para.getAnnotations();
    }

    public Map<String, Map<String, Object>> getAnnotations() {
		return annotations;
	}
    
    public String getDefaultValue() {
        return defaultValue;
    }

    public String getAllowableValue() {
        return allowableValue;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getAccess() {
        return access;
    }

    @Override
    public String toString() {
        ObjectMapper om = new ObjectMapper();
        try {
           return  om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }

    }

    public void setName(String name) {
        this.name = name;
    }
}
