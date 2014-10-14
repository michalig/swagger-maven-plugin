package com.github.kongchen.swagger.docgen.mustache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

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
	
	private java.lang.reflect.Parameter getParameter(Method method, String name) {
		for(java.lang.reflect.Parameter param : method.getParameters()){
			if (param.getAnnotation(QueryParam.class) != null && name.equals(param.getAnnotation(QueryParam.class).value())) {
				return param;
			} else if (param.getAnnotation(PathParam.class) != null && name.equals(param.getAnnotation(PathParam.class).value())) {
				return param;
			} else if (param.getAnnotation(CookieParam.class) != null && name.equals(param.getAnnotation(CookieParam.class).value())) {
				return param;
			} else if (param.getAnnotation(HeaderParam.class) != null && name.equals(param.getAnnotation(HeaderParam.class).value())) {
				return param;
			} else if ("body".equals(name)) {
				return param;
			}
		}
		System.out.println("Couldn't find param [" + name + "] in method [" + method.getName() + "]");
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
