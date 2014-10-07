package com.github.kongchen.swagger.docgen.mustache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.wordnik.swagger.model.ModelProperty;

public class MustacheItem {
    private String name;

    private String type;

    private String linkType;

    private boolean required;

    private String access;

    private String description;

    private String notes;

    private String allowableValue;

    private int position;

    private Map<String, Map<String, String>> annotations = new HashMap<String, Map<String,String>>();
    
    public MustacheItem(String name, ModelProperty documentationSchema, Field field) {

        this.name = name;
        this.type = documentationSchema.type();
        this.linkType = this.type;
        this.description = Utils.getStrInOption(documentationSchema.description());
        this.required = documentationSchema.required();
        this.notes = Utils.getStrInOption(documentationSchema.description());
        this.linkType = TypeUtils.filterBasicTypes(this.linkType);
        this.allowableValue = Utils.allowableValuesToString(documentationSchema.allowableValues());
        this.position = documentationSchema.position();
        if (field != null) {
        	for (Annotation annotation : field.getAnnotations()) {
        		annotations.put(annotation.annotationType().getName(), TypeUtils.annotationToMap(annotation));
        	}
        }
    }

    public Map<String, Map<String, String>> getAnnotations() {
		return annotations;
	}
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAllowableValue() {
        return allowableValue;
    }

    public void setAllowableValue(String allowableValue) {
        this.allowableValue = allowableValue;
    }

    public void setTypeAsArray(String elementType) {
        this.type = TypeUtils.AsArrayType(elementType);
        setLinkType(TypeUtils.filterBasicTypes(elementType));
    }

    public int getPosition() {
        return position;
    }

	@Override
	public String toString() {
		return "MustacheItem [name=" + name + ", type=" + type + ", linkType=" + linkType + ", required=" + required
				+ ", access=" + access + ", description=" + description + ", notes=" + notes + ", allowableValue="
				+ allowableValue + ", position=" + position + ", annotations=" + annotations + "]";
	}


}
