package com.github.kongchen.swagger.docgen.mustache;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import scala.Option;

import com.github.kongchen.swagger.docgen.TypeUtils;
import com.wordnik.swagger.model.AllowableValues;
import com.wordnik.swagger.model.Parameter;

public class ExtendedParameter {

	private Parameter parameter;
	private Map<String, Map<String, Object>> annotations = new HashMap<String, Map<String, Object>>();

	public ExtendedParameter(Parameter parameter, java.lang.reflect.Parameter param) {
		this.parameter = parameter;
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
		return parameter.dataType();
	}

	public boolean allowMultiple() {
		return parameter.allowMultiple();
	}

	public boolean required() {
		return parameter.required();
	}

	public Option<String> description() {
		return parameter.description();
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
