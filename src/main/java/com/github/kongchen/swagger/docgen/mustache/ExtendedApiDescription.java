package com.github.kongchen.swagger.docgen.mustache;

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
			operations.add(new ExtendedOperation(iterator.next(), c));
		}
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
