package com.github.kongchen.swagger.docgen.mustache;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import scala.Option;

import com.github.kongchen.swagger.docgen.TypeUtils;
import com.github.kongchen.swagger.docgen.util.Utils;
import com.wordnik.swagger.model.AllowableValues;
import com.wordnik.swagger.model.Parameter;

public class ExtendedParameter {

	private Parameter parameter;
	private Map<String, Map<String, Object>> annotations = new HashMap<String, Map<String, Object>>();
	private String dataType;
	private String description;

	public ExtendedParameter(Parameter parameter, java.lang.reflect.Parameter param) {
		this.parameter = parameter;
		dataType = parameter.dataType();
		description = Utils.getStrInOption(parameter.description());
		if (StringUtils.isNotBlank(description) && description.matches(".*/\\*.*\\*/.*")) {
			dataType = StringUtils.substringBetween(description, "/*", "*/");
			description = StringUtils.substringBefore(description, "/*")
					+ StringUtils.substringAfter(description, "*/");
		}
		if (param != null) {
			for (Annotation annotation : param.getAnnotations()) {
				annotations.put(annotation.annotationType().getName(), TypeUtils.annotationToMap(annotation));
			}
		}
	}

	public Map<String, Map<String, Object>> getAnnotations() {
		return annotations;
	}

	public String paramType() {
		return parameter.paramType();
	}

	public String name() {
		return parameter.name();
	}

	public String dataType() {
		return dataType;
	}

	public boolean allowMultiple() {
		return parameter.allowMultiple();
	}

	public boolean required() {
		return parameter.required();
	}

	public String description() {
		return description;
	}

	public Option<String> defaultValue() {
		return parameter.defaultValue();
	}

	public AllowableValues allowableValues() {
		return parameter.allowableValues();
	}

	public Option<String> paramAccess() {
		return parameter.paramAccess();
	}
}
