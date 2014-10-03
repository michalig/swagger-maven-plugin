package com.github.kongchen.swagger.docgen.mustache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import scala.collection.Iterator;
import scala.collection.TraversableOnce;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.model.Authorization;
import com.wordnik.swagger.model.Operation;
import com.wordnik.swagger.model.Parameter;
import com.wordnik.swagger.model.ResponseMessage;

import edu.emory.mathcs.backport.java.util.Collections;

public class ExtendedOperation {

	private Operation operation;
	private List<String> tags;
	private List<ExtendedParameter> parameters = new ArrayList<ExtendedParameter>();
	
	private static final List<String> ordered = Lists.newArrayList("query", "body", "response_header", "path", "header");

	public ExtendedOperation(Operation operation, Method method) {
		this.operation = operation;
		ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
		if (apiOperation != null) {
			this.tags = Lists.newArrayList(Splitter.onPattern(",").omitEmptyStrings().trimResults().split(apiOperation.tags()));
		}
		Iterator<Parameter> iterator = operation.parameters().iterator();
		while (iterator.hasNext()) {
			Parameter parameter = iterator.next();
			parameters.add(new ExtendedParameter(parameter, getParameter(method, parameter.name())));
		}
//		parameters = Lists.newArrayList(JavaConversions.asJavaList(operation.parameters()));
		Collections.sort(parameters, new Comparator<ExtendedParameter>() {
			@Override
			public int compare(ExtendedParameter o1, ExtendedParameter o2) {
				if (!ordered.contains(o1.paramType())) {
					return -1;
				} else if (!ordered.contains(o2.paramType())) {
					return 1;
				}
				return ordered.indexOf(o1.paramType()) - ordered.indexOf(o2.paramType());
			}
		});
	}
	
	private java.lang.reflect.Parameter getParameter(Method m, String name) {
		for(java.lang.reflect.Parameter param : m.getParameters()){
			if (param.getName().equals(name)) {
				return param;
			}
		}
		return null;
	}

	public List<String> tags() {
		return tags;
	}

	public int position() {
		return operation.position();
	}

	public String method() {
		return operation.method();
	}

	public String summary() {
		return operation.summary();
	}

	public String nickname() {
		return operation.nickname();
	}

	public String responseClass() {
		return operation.responseClass();
	}

	public TraversableOnce<Authorization> authorizations() {
		return operation.authorizations();
	}

	public String notes() {
		return operation.notes();
	}

	public List<ExtendedParameter> parameters() {
		return parameters;
	}

	public TraversableOnce<ResponseMessage> responseMessages() {
		return operation.responseMessages();
	}

}
