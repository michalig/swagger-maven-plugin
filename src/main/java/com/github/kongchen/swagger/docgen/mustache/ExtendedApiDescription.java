package com.github.kongchen.swagger.docgen.mustache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import scala.Option;
import scala.collection.Iterator;

import com.wordnik.swagger.model.ApiDescription;
import com.wordnik.swagger.model.Operation;

public class ExtendedApiDescription {

	private ApiDescription apiDescription;
	private final List<ExtendedOperation> operations = new ArrayList<ExtendedOperation>();

	public ExtendedApiDescription(ApiDescription apiDescription, Class<?> c) {
		this.apiDescription = apiDescription;
		Iterator<Operation> iterator = apiDescription.operations().iterator();
		while (iterator.hasNext()) {
			Operation operation = iterator.next();
			operations.add(new ExtendedOperation(operation, getMethod(c, operation.nickname())));
		}
	}
	
	private Method getMethod(Class<?> c, String name) {
		for(Method method : c.getMethods()){
			if (method.getName().equals(name)) {
				//current method
				return method;
			}
		}
		return null;
	}

	public Option<String> description() {
		return apiDescription.description();
	}

	public String path() {
		return apiDescription.path();
	}

	public List<ExtendedOperation> operations() {
		return operations;
	}
	
}
