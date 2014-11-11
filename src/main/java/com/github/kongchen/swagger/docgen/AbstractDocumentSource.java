package com.github.kongchen.swagger.docgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import scala.collection.Iterator;
import scala.collection.JavaConversions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kongchen.swagger.docgen.mavenplugin.ApiSourceInfo;
import com.github.kongchen.swagger.docgen.mustache.ExtendedApiListing;
import com.github.kongchen.swagger.docgen.mustache.OutputTemplate;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.converter.OverrideConverter;
import com.wordnik.swagger.core.util.JsonSerializer;
import com.wordnik.swagger.core.util.JsonUtil;
import com.wordnik.swagger.model.ApiListingReference;
import com.wordnik.swagger.model.ResourceListing;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: chekong 05/13/2013
 */
public abstract class AbstractDocumentSource {

	protected final LogAdapter LOG;

	private final String outputPath;

	private final String templatePath;

	private final String mustacheFileRoot;

	private final String swaggerPath;

	protected ResourceListing serviceDocument;

	List<ExtendedApiListing> validDocuments = new ArrayList<ExtendedApiListing>();

	private String basePath;

	private String apiVersion;

	private ApiSourceInfo apiInfo;

	private ObjectMapper mapper = new ObjectMapper();

	private OutputTemplate outputTemplate;

	private boolean useOutputFlatStructure;

	private String overridingModels;

	private Object exampleProvider;
	
	public AbstractDocumentSource(LogAdapter logAdapter, String outputPath,
			String outputTpl, String swaggerOutput, String mustacheFileRoot,
			boolean useOutputFlatStructure1, String overridingModels,
			String exampleProviderClassName) {
		LOG = logAdapter;
		this.outputPath = outputPath;
		this.templatePath = outputTpl;
		this.mustacheFileRoot = mustacheFileRoot;
		this.useOutputFlatStructure = useOutputFlatStructure1;
		this.swaggerPath = swaggerOutput;
		this.overridingModels = overridingModels;
		if (exampleProviderClassName != null) {
			try {
				this.exampleProvider = Class.forName(exampleProviderClassName).newInstance();
			} catch (Exception e) {
				LOG.error("Error setting exampleProvider for class " + exampleProviderClassName + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public abstract void loadDocuments() throws Exception, GenerateException;

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

	public OutputTemplate getOutputTemplate() {
		return outputTemplate;
	}

	public ApiSourceInfo getApiInfo() {
		return apiInfo;
	}

	public void setApiInfo(ApiSourceInfo apiInfo) {
		this.apiInfo = apiInfo;
	}

	protected void acceptDocument(ExtendedApiListing doc) {
		String basePath;
		// will append api's basePath. However, apiReader does not read it
		// correctly by now
		if (doc.basePath() != null) {
			basePath = this.basePath + doc.basePath();
		} else {
			basePath = this.basePath;
		}
		doc.setBasePath(basePath);
		validDocuments.add(doc);
	}

	public List<ExtendedApiListing> getValidDocuments() {
		return validDocuments;
	}

	public void toSwaggerDocuments(String swaggerUIDocBasePath)
			throws GenerateException {
		if (swaggerPath == null) {
			return;
		}
		File dir = new File(swaggerPath);
		if (dir.isFile()) {
			throw new GenerateException(String.format(
					"Swagger-outputDirectory[%s] must be a directory!",
					swaggerPath));
		}

		if (!dir.exists()) {
			try {
				FileUtils.forceMkdir(dir);
			} catch (IOException e) {
				throw new GenerateException(String.format(
						"Create Swagger-outputDirectory[%s] failed.",
						swaggerPath));
			}
		}
		cleanupOlds(dir);

		prepareServiceDocument();
		// rewrite basePath in swagger-ui output file using the value in
		// configuration file.
		writeInDirectory(dir, serviceDocument, swaggerUIDocBasePath);
		for (ExtendedApiListing doc : validDocuments) {
			writeInDirectory(dir, doc, basePath);
		}
	}

	public void loadOverridingModels() throws GenerateException {
		if (overridingModels != null) {
			try {
				JsonNode readTree = mapper.readTree(this.getClass()
						.getResourceAsStream(overridingModels));
				for (JsonNode jsonNode : readTree) {
					JsonNode classNameNode = jsonNode.get("className");
					String className = classNameNode.asText();
					JsonNode jsonStringNode = jsonNode.get("jsonString");
					String jsonString = jsonStringNode.asText();
					OverrideConverter converter = new OverrideConverter();
					converter.add(className, jsonString);
					ModelConverters.addConverter(converter, true);
				}
			} catch (JsonProcessingException e) {
				throw new GenerateException(
						String.format(
								"Swagger-overridingModels[%s] must be a valid JSON file!",
								overridingModels), e);
			} catch (IOException e) {
				throw new GenerateException(String.format(
						"Swagger-overridingModels[%s] not found!",
						overridingModels), e);
			}
		}
	}

	private void cleanupOlds(File dir) {
		if (dir.listFiles() != null) {
			for (File f : dir.listFiles()) {
				if (f.getName().endsWith("json")) {
					f.delete();
				}
			}
		}
	}

	private void prepareServiceDocument() {
		List<ApiListingReference> apiListingReferences = new ArrayList<ApiListingReference>();
		for (Iterator<ApiListingReference> iterator = serviceDocument.apis()
				.iterator(); iterator.hasNext();) {
			ApiListingReference apiListingReference = iterator.next();
			String newPath = apiListingReference.path();
			if (useOutputFlatStructure) {
				newPath = newPath.replaceAll("/", "_");
				if (newPath.startsWith("_")) {
					newPath = "/" + newPath.substring(1);
				}
			}
			newPath += ".{format}";
			apiListingReferences.add(new ApiListingReference(newPath,
					apiListingReference.description(), apiListingReference
							.position()));
		}
		// there's no setter of path for ApiListingReference, we need to create
		// a new ResourceListing for new path
		serviceDocument = new ResourceListing(serviceDocument.apiVersion(),
				serviceDocument.swaggerVersion(),
				scala.collection.immutable.List.fromIterator(JavaConversions
						.asScalaIterator(apiListingReferences.iterator())),
				serviceDocument.authorizations(), serviceDocument.info());
	}

	protected String resourcePathToFilename(String resourcePath) {
		if (resourcePath == null) {
			return "service.json";
		}
		String name = resourcePath;
		if (name.startsWith("/")) {
			name = name.substring(1);
		}
		if (name.endsWith("/")) {
			name = name.substring(0, name.length() - 1);
		}

		if (useOutputFlatStructure) {
			name = name.replaceAll("/", "_");
		}

		return name + ".json";
	}

	private void writeInDirectory(File dir, ExtendedApiListing apiListing,
			String basePath) throws GenerateException {
		String filename = resourcePathToFilename(apiListing.resourcePath());
		try {
			File serviceFile = createFile(dir, filename);
			String json = JsonSerializer.asJson(apiListing);
			JsonNode tree = mapper.readTree(json);
			if (basePath != null) {
				((ObjectNode) tree).put("basePath", basePath);
			}
			JsonUtil.mapper().writerWithDefaultPrettyPrinter()
					.writeValue(serviceFile, tree);
		} catch (IOException e) {
			throw new GenerateException(e);
		}
	}

	private void writeInDirectory(File dir, ResourceListing resourceListing,
			String basePath) throws GenerateException {
		String filename = resourcePathToFilename(null);
		try {
			File serviceFile = createFile(dir, filename);
			String json = JsonSerializer.asJson(resourceListing);
			JsonNode tree = mapper.readTree(json);
			if (basePath != null) {
				((ObjectNode) tree).put("basePath", basePath);
			}

			JsonUtil.mapper().writerWithDefaultPrettyPrinter()
					.writeValue(serviceFile, tree);
		} catch (IOException e) {
			throw new GenerateException(e);
		}
	}

	protected File createFile(File dir, String outputResourcePath)
			throws IOException {
		File serviceFile;
		int i = outputResourcePath.lastIndexOf("/");
		if (i != -1) {
			String fileName = outputResourcePath.substring(i + 1);
			String subDir = outputResourcePath.substring(0, i);
			File finalDirectory = new File(dir, subDir);
			finalDirectory.mkdirs();
			serviceFile = new File(finalDirectory, fileName);
		} else {
			serviceFile = new File(dir, outputResourcePath);
		}
		while (!serviceFile.createNewFile()) {
			serviceFile.delete();
		}
		LOG.info("Creating file " + serviceFile.getAbsolutePath());
		return serviceFile;
	}

	public OutputTemplate prepareMustacheTemplate() throws GenerateException {
		this.outputTemplate = new OutputTemplate(this);
		return outputTemplate;
	}

	public void toDocuments() throws GenerateException, IOException {
		if (outputTemplate == null) {
			prepareMustacheTemplate();
		}
		if (outputTemplate.getApiDocuments().isEmpty()) {
			LOG.warn("nothing to write.");
			return;
		}
		LOG.info("Writing doc to " + outputPath + "...");

		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(outputPath);
		} catch (FileNotFoundException e) {
			throw new GenerateException(e);
		}
		OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream,
				Charset.forName("UTF-8"));

		if (templatePath.endsWith("vm")) {
			// use Velocity template - hell yeah!!!
			VelocityEngine ve = new VelocityEngine();
			ve.init();
			Template t;
			try {
				t = ve.getTemplate(templatePath);
			} catch (ResourceNotFoundException rnfe) {
				// try to load it from the classpath
				LOG.info("VM template not found - will try to load it from classpath.");
				ve = new VelocityEngine();
				ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
				ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
				ve.init();
				t = ve.getTemplate(templatePath);
				LOG.info("Loaded from classpath.");
			}
			VelocityContext context = new VelocityContext();
			context.put("template", outputTemplate);
			context.put("example", exampleProvider);
			context.put("Arrays", Arrays.class);
			context.put("CollectionUtils", CollectionUtils.class);
			t.merge(context, writer);
			writer.flush();
			writer.close();
		} else {
			try {
				URL url = getTemplateUri().toURL();
				InputStreamReader reader = new InputStreamReader(url.openStream(),
						Charset.forName("UTF-8"));
				Mustache mustache = getMustacheFactory().compile(reader,
						templatePath);

				mustache.execute(writer, outputTemplate).flush();
				writer.close();
				LOG.info("Done!");
			} catch (MalformedURLException e) {
				throw new GenerateException(e);
			} catch (IOException e) {
				throw new GenerateException(e);
			}
		}
	}

	private URI getTemplateUri() throws GenerateException {
		URI uri;
		try {
			uri = new URI(templatePath);
		} catch (URISyntaxException e) {
			File file = new File(templatePath);
			if (!file.exists()) {
				throw new GenerateException(
						"Template "
								+ file.getAbsoluteFile()
								+ " not found. You can go to https://github.com/kongchen/api-doc-template to get templates.");
			}
			uri = file.toURI();
		}
		if (!uri.isAbsolute()) {
			File file = new File(templatePath);
			if (!file.exists()) {
				throw new GenerateException(
						"Template "
								+ file.getAbsoluteFile()
								+ " not found. You can go to https://github.com/kongchen/api-doc-template to get templates.");
			} else {
				uri = new File(templatePath).toURI();
			}
		}
		return uri;
	}

	private DefaultMustacheFactory getMustacheFactory() {
		if (mustacheFileRoot == null) {
			return new DefaultMustacheFactory();
		} else {
			return new DefaultMustacheFactory(new File(mustacheFileRoot));
		}
	}

	public Object getExampleProvider() {
		return exampleProvider;
	}

}
