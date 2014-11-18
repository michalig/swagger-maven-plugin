package com.github.kongchen.swagger.docgen.mustache;

import java.util.ArrayList;
import java.util.List;

import scala.Option;
import scala.collection.Iterator;
import scala.collection.immutable.Map;

import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.ApiDescription;
import com.wordnik.swagger.model.ApiListing;
import com.wordnik.swagger.model.Model;

public class ExtendedApiListing {

	private ApiListing apiListing;
	private final List<ExtendedApiDescription> apis = new ArrayList<ExtendedApiDescription>();
	private String basePath;
//	private Option<Map<String, Model>> models;

	public ExtendedApiListing(ApiListing apiListing, Class<?> c) {
		this.apiListing =  apiListing;
		this.basePath = apiListing.basePath();
		Iterator<ApiDescription> iterator = apiListing.apis().iterator();
		while (iterator.hasNext()) {
			apis.add(new ExtendedApiDescription(iterator.next(), c));
		}
		//TODO models
//		apiListing.models().iterator()
	}
	
	public String resourcePath() {
		return apiListing.resourcePath();
	}

	public List<ExtendedApiDescription> apis() {
		return apis;
	}

	public Option<Map<String,Model>> models() {
		return apiListing.models();
	}

	public int position() {
		return apiListing.position();
	}

	public String apiVersion() {
		return apiListing.apiVersion();
	}

	public String basePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

}
