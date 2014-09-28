package com.github.kongchen.swagger.docgen.mustache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import scala.collection.JavaConversions;
import scala.collection.mutable.Buffer;

import com.github.kongchen.swagger.docgen.DocTemplateConstants;
import com.wordnik.swagger.core.ApiValues;
import com.wordnik.swagger.model.Authorization;
import com.wordnik.swagger.model.ResponseMessage;

public class MustacheOperation {
    private final int opIndex;

    private final String httpMethod;

    private final String summary;

    private final String notes;

    private final MustacheResponseClass responseClass;
    private List<MustacheResponseClass> responseClasses = new ArrayList<MustacheResponseClass>();

    private final String nickname;
    private final List<MustacheAuthorization> authorizations = new ArrayList<MustacheAuthorization>();

    private List<MustacheParameterSet> parameters;
    private final List<String> tags;

	private MustacheParameterSet requestQuery;
    private MustacheParameterSet requestHeader;
    private MustacheParameterSet requestBody;
    private MustacheParameterSet requestPath;
    private MustacheParameterSet responseHeader;

    private MustacheResponseClass requestBodyClass;

	private static final Pattern genericInNotes = Pattern.compile("(/\\*.*<)((\\w+|((\\w+\\.)+\\w+))|(((\\w+|((\\w+\\.)+\\w+)),)+(\\w+|((\\w+\\.)+\\w+))))(>.*\\*/)");

    private List<MustacheResponseMessage> errorResponses = new ArrayList<MustacheResponseMessage>();

    List<MustacheSample> samples;


    public MustacheOperation(MustacheDocument mustacheDocument, ExtendedOperation op) {
        Buffer<Authorization> authorBuffer = op.authorizations().toBuffer();
        for(Authorization authorization : JavaConversions.asJavaList(authorBuffer)) {
            this.authorizations.add(new MustacheAuthorization(authorization));
        }
        this.opIndex = op.position();
        this.httpMethod = op.method();
        AbstractMap.SimpleEntry<String, String> notesAndGenericStr = parseGenericFromNotes(op.notes());
        this.notes = notesAndGenericStr.getKey();
        this.summary = op.summary();
        this.nickname = op.nickname();
        this.tags = op.tags();
        this.parameters = mustacheDocument.analyzeParameters(op.parameters());
        responseClass = new MustacheResponseClass(op.responseClass() + notesAndGenericStr.getValue());
        Buffer<ResponseMessage> errorbuffer = op.responseMessages().toBuffer();
        List<ResponseMessage> responseMessages = JavaConversions.asJavaList(errorbuffer);
        for (ResponseMessage responseMessage : responseMessages) {
            if (!responseMessage.responseModel().isEmpty()) {
                String className = responseMessage.responseModel().get();
                this.responseClasses.add(new MustacheResponseClass(className));
            }
            this.errorResponses.add(new MustacheResponseMessage(responseMessage));
        }
        if (parameters == null) {
            return;
        }
        for (MustacheParameterSet para : parameters) {
            if (para.getParamType().equals(ApiValues.TYPE_QUERY())) {
                this.requestQuery = para;
            } else if (para.getParamType().equals(ApiValues.TYPE_HEADER())) {
                this.requestHeader = para;
            } else if (para.getParamType().equals(ApiValues.TYPE_BODY())) {
                this.requestBody = para;
                if (para.getParas() != null && !para.getParas().isEmpty()) {
                	if (para.getParas().get(0).getLinkType() != null) {
                		this.requestBodyClass = new MustacheResponseClass(para.getParas().get(0).getLinkType());
                	} else if (para.getParas().get(0).getType() != null) {
                		this.requestBodyClass = new MustacheResponseClass(para.getParas().get(0).getType());
                	}
                }
            } else if (para.getParamType().equals(ApiValues.TYPE_PATH())) {
                this.requestPath = para;
            } else if (para.getParamType().equals(DocTemplateConstants.TYPE_RESPONSE_HEADER)) {
                this.responseHeader = para;
            }
        }
    }

    private AbstractMap.SimpleEntry<String, String> parseGenericFromNotes(String notes) {
        Scanner scanner = new Scanner(notes);
        String genericString = scanner.findInLine(genericInNotes);
        if (genericString != null) {
            return new AbstractMap.SimpleEntry<String, String>(notes.replaceFirst(genericInNotes.pattern(), ""),
                    genericString.replaceAll("/\\*", "").replaceAll("\\*/", "").trim());
        } else {
            return new AbstractMap.SimpleEntry<String, String>(notes, "");
        }
    }

    public List<MustacheAuthorization> getAuthorizations() {
        return authorizations;
    }

    public MustacheParameterSet getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(MustacheParameterSet responseHeader) {
        this.responseHeader = responseHeader;
    }

    public MustacheParameterSet getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(MustacheParameterSet requestPath) {
        this.requestPath = requestPath;
    }

    public MustacheParameterSet getRequestQuery() {
        return requestQuery;
    }

    public void setRequestQuery(MustacheParameterSet requestQuery) {
        this.requestQuery = requestQuery;
    }

    public MustacheParameterSet getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(MustacheParameterSet requestHeader) {
        this.requestHeader = requestHeader;
    }

    public MustacheParameterSet getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(MustacheParameterSet requestBody) {
        this.requestBody = requestBody;
    }

    public List<MustacheSample> getSamples() {
        return samples;
    }

    public void setSamples(List<MustacheSample> samples) {
        this.samples = samples;
    }

    public int getOpIndex() {
        return opIndex;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getSummary() {
        return summary;
    }

    public String getNotes() {
        return notes;
    }

    public String getNickname() {
        return nickname;
    }

    public List<MustacheParameterSet> getParameters() {
        return parameters;
    }

    public List<MustacheResponseMessage> getErrorResponses() {
        return errorResponses;
    }

    public MustacheResponseClass getResponseClass() {
        return responseClass;
    }

    public List<MustacheResponseClass> getResponseClasses() {
        return responseClasses;
    }
    
    public List<String> getTags() {
		return tags;
	}
    
    public MustacheResponseClass getRequestBodyClass() {
		return requestBodyClass;
	}

}
