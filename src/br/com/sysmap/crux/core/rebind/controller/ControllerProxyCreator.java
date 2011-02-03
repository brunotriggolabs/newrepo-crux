/*
 * Copyright 2009 Sysmap Solutions Software e Consultoria Ltda.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package br.com.sysmap.crux.core.rebind.controller;

import java.io.PrintWriter;

import br.com.sysmap.crux.core.client.Crux;
import br.com.sysmap.crux.core.client.collection.FastMap;
import br.com.sysmap.crux.core.client.controller.Controller;
import br.com.sysmap.crux.core.client.controller.Expose;
import br.com.sysmap.crux.core.client.controller.Validate;
import br.com.sysmap.crux.core.client.controller.crossdoc.ClientSerializationStreamReader;
import br.com.sysmap.crux.core.client.controller.crossdoc.ClientSerializationStreamWriter;
import br.com.sysmap.crux.core.client.controller.crossdoc.CrossDocument;
import br.com.sysmap.crux.core.client.event.ControllerInvoker;
import br.com.sysmap.crux.core.client.event.CrossDocumentInvoker;
import br.com.sysmap.crux.core.client.event.CruxEvent;
import br.com.sysmap.crux.core.client.event.EventProcessor;
import br.com.sysmap.crux.core.client.formatter.HasFormatter;
import br.com.sysmap.crux.core.i18n.MessagesFactory;
import br.com.sysmap.crux.core.rebind.AbstractInvocableProxyCreator;
import br.com.sysmap.crux.core.rebind.CruxGeneratorException;
import br.com.sysmap.crux.core.rebind.GeneratorMessages;
import br.com.sysmap.crux.core.rebind.crossdocument.gwt.SerializationUtils;
import br.com.sysmap.crux.core.rebind.crossdocument.gwt.Shared;
import br.com.sysmap.crux.core.rebind.crossdocument.gwt.TypeSerializerCreator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
public class ControllerProxyCreator extends AbstractInvocableProxyCreator
{
	protected static GeneratorMessages messages = (GeneratorMessages)MessagesFactory.getMessages(GeneratorMessages.class);
	private static final String CONTROLLER_PROXY_SUFFIX = "_ControllerProxy";
	
	private final JClassType controllerClass;
	private final boolean isAutoBindEnabled;
	private final boolean isCrossDoc;
	private final boolean isSingleton;

	
	/**
	 * Constructor
	 * 
	 * @param logger
	 * @param context
	 * @param crossDocumentIntf
	 */
	public ControllerProxyCreator(TreeLogger logger, GeneratorContext context, JClassType controllerClass)
	{
		super(logger, context, getCrossDocumentInterface(logger, context, controllerClass), controllerClass);
		this.controllerClass = controllerClass;
        try
        {
        	JClassType crossDocumentType = controllerClass.getOracle().getType(CrossDocument.class.getCanonicalName());
        	this.isCrossDoc = (crossDocumentType.isAssignableFrom(controllerClass));
        	if (isCrossDoc && this.baseProxyType == null)
        	{
        		throw new CruxGeneratorException(messages.crossDocumentCanNotFindControllerCrossDocInterface(controllerClass.getQualifiedSourceName()));
        	}
        }
        catch (NotFoundException e)
        {
        	throw new CruxGeneratorException(messages.crossDocumentCanNotFindControllerCrossDocInterface(controllerClass.getQualifiedSourceName()));
        }
		Controller controllerAnnot = controllerClass.getAnnotation(Controller.class);
		this.isSingleton = (controllerAnnot == null || controllerAnnot.stateful());
		this.isAutoBindEnabled = (controllerAnnot == null || controllerAnnot.autoBind());
	}
	
	/**
	 * @param logger
	 * @param context
	 * @param controllerClass
	 * @return
	 */
	private static JClassType getCrossDocumentInterface(TreeLogger logger, GeneratorContext context, JClassType controllerClass) 
	{
		TypeOracle typeOracle = context.getTypeOracle();
		assert (typeOracle != null);

		JClassType crossDocument = typeOracle.findType(controllerClass.getQualifiedSourceName()+"CrossDoc");
		return crossDocument==null?null:crossDocument.isInterface();
	}
	
	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateProxyContructor(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateProxyContructor(SourceWriter srcWriter)
	{
		srcWriter.println();
		srcWriter.println("public " + getProxySimpleName() + "() {");
		srcWriter.indent();
		generateAutoCreateFields(srcWriter, "this");
		srcWriter.outdent();
		srcWriter.println("}");
	}
	
	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateProxyFields(com.google.gwt.user.rebind.SourceWriter)
	 */
	@Override
	protected void generateProxyFields(SourceWriter srcWriter) throws CruxGeneratorException
	{
		srcWriter.println("FastMap<Boolean> __runningMethods = new FastMap<Boolean>();");

		if (isCrossDoc)
		{
			String typeSerializerName = SerializationUtils.getTypeDeserializerQualifiedName(baseProxyType);
			srcWriter.println("private static final " + typeSerializerName + " SERIALIZER = new " + typeSerializerName + "();");
		}
		srcWriter.println();
	}
	
	@Override
	protected void generateProxyMethods(SourceWriter srcWriter)
	{
		generateInvokeMethod(srcWriter);
		generateScreenUpdateWidgetsFunction(controllerClass, srcWriter);
		generateControllerUpdateObjectsFunction(controllerClass, srcWriter);
		generateIsAutoBindEnabledMethod(srcWriter, isAutoBindEnabled);
		
		if (isCrossDoc)
		{
			generateCrossDocInvokeMethod(srcWriter);
			generateCreateStreamReaderMethod(srcWriter);
			generateCreateStreamWriterMethod(srcWriter);
		}
	}

	/**
	 * @see br.com.sysmap.crux.core.rebind.AbstractProxyCreator#generateTypeSerializers(SerializableTypeOracle, SerializableTypeOracle)
	 */
	@Override
	protected void generateTypeSerializers(SerializableTypeOracle typesSentFromBrowser, SerializableTypeOracle typesSentToBrowser) throws CruxGeneratorException
	{
		if (this.baseProxyType != null)
		{
			try
            {
	            TypeSerializerCreator tsc = new TypeSerializerCreator(logger, typesSentToBrowser, typesSentFromBrowser, context, 
	            		SerializationUtils.getTypeDeserializerQualifiedName(baseProxyType),
		                SerializationUtils.getTypeDeserializerSimpleName(baseProxyType));
	            tsc.realize(logger);
            }
            catch (Exception e)
            {
            	throw new CruxGeneratorException(e.getMessage(), e);
            }
		}
	}

	/**
	 * @return
	 */
	protected String getClientSerializationStreamReaderClassName()
	{
		return ClientSerializationStreamReader.class.getCanonicalName();
	}
	
	/**
	 * @return
	 */
	protected String getClientSerializationStreamWriterClassName()
	{
		return ClientSerializationStreamWriter.class.getCanonicalName();
	}
	
	/**
	 * @return
	 */
	protected String[] getImports()
    {
	    String[] imports = new String[] {
    		GWT.class.getCanonicalName(), 
    		br.com.sysmap.crux.core.client.screen.Screen.class.getCanonicalName(),
    		FastMap.class.getCanonicalName(),
    		CruxEvent.class.getCanonicalName(),
    		GwtEvent.class.getCanonicalName(),
    		HasValue.class.getCanonicalName(),
    		HasText.class.getCanonicalName(),
    		HasFormatter.class.getCanonicalName(),
    		Widget.class.getCanonicalName(),
    		RunAsyncCallback.class.getCanonicalName(),
    		EventProcessor.class.getCanonicalName(),
    		Crux.class.getCanonicalName(), 
    		SerializationException.class.getCanonicalName(), 
    		SerializationStreamReader.class.getCanonicalName(),
    		SerializationStreamWriter.class.getCanonicalName()
		};
	    return imports;
    }
	
	/**
	 * @return the full qualified name of the proxy object.
	 */
	protected String getProxyQualifiedName()
	{
		return controllerClass.getPackage().getName() + "." + getProxySimpleName();
	}
	
	/**
	 * @return the simple name of the proxy object.
	 */
	protected String getProxySimpleName()
	{
		return controllerClass.getSimpleSourceName() + CONTROLLER_PROXY_SUFFIX;
	}
	
	/**
	 * @return a sourceWriter for the proxy class
	 */
	protected SourceWriter getSourceWriter()
	{
		JPackage crossDocIntfPkg = controllerClass.getPackage();
		String packageName = crossDocIntfPkg == null ? "" : crossDocIntfPkg.getName();
		PrintWriter printWriter = context.tryCreate(logger, packageName, getProxySimpleName());

		if (printWriter == null)
		{
			return null;
		}

		ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, getProxySimpleName());

		String[] imports = getImports();
		for (String imp : imports)
		{
			composerFactory.addImport(imp);
		}

		composerFactory.setSuperclass(controllerClass.getQualifiedSourceName());
		String baseInterface = isCrossDoc?CrossDocumentInvoker.class.getCanonicalName():ControllerInvoker.class.getCanonicalName();
		composerFactory.addImplementedInterface(baseInterface);

		return composerFactory.createSourceWriter(context, printWriter);
	}
	
	/**
	 * @param srcWriter
	 */
	private void generateCreateStreamReaderMethod(SourceWriter srcWriter)
	{
		srcWriter.println("protected SerializationStreamReader createStreamReader(String encoded) throws SerializationException {");
		srcWriter.indent();
		String readerClassName = getClientSerializationStreamReaderClassName();

		srcWriter.println(readerClassName+" clientSerializationStreamReader = new "+readerClassName+"(SERIALIZER);");
		srcWriter.println("clientSerializationStreamReader.prepareToRead(encoded);");
		srcWriter.println("return clientSerializationStreamReader;");
		srcWriter.outdent();
		srcWriter.println("}");
	}
	
	/**
	 * @param srcWriter
	 */
	private void generateCreateStreamWriterMethod(SourceWriter srcWriter)
	{
		srcWriter.println("protected SerializationStreamWriter createStreamWriter(){");
		srcWriter.indent();
		String writerClassName = getClientSerializationStreamWriterClassName();

		srcWriter.println(writerClassName+" clientSerializationStreamWriter = new "+writerClassName+"(SERIALIZER);");
		srcWriter.println("clientSerializationStreamWriter.prepareToWrite();");
		srcWriter.println("return clientSerializationStreamWriter;");
		srcWriter.outdent();
		srcWriter.println("}");	
	}

	/**
	 * @param sourceWriter
	 * @param method
	 */
	private void generateCrosDocInvokeBlockForMethod(SourceWriter sourceWriter, JMethod method)
	{
		JParameter[] params = method.getParameters();
		JType returnType = method.getReturnType();

		sourceWriter.println("if (methodCalled.equals(\""+getJsniSimpleSignature(method)+"\")){");
    	sourceWriter.indent();
    	sourceWriter.println("try{");
    	sourceWriter.indent();

    	if (returnType != JPrimitiveType.VOID)
    	{
        	sourceWriter.print("streamWriter."+Shared.getStreamWriteMethodNameFor(returnType)+"(");
    		
    	}
    	sourceWriter.println("wrapper." + method.getName()+"(");

		for (int i = 0; i < params.length ; ++i)
		{
			JParameter param = params[i];
			if (i > 0)
			{
				sourceWriter.print(",");
			}
			sourceWriter.println("("+param.getType().getParameterizedQualifiedSourceName()+")streamReader."+Shared.getStreamReadMethodNameFor(param.getType())+"()");
		}
    	sourceWriter.print(")");
		if (returnType != JPrimitiveType.VOID)
		{
			sourceWriter.print(")");
		}
		sourceWriter.println(";");
		sourceWriter.println("isExecutionOK = true;");

    	sourceWriter.outdent();
    	sourceWriter.println("}catch(Throwable e){");
    	sourceWriter.indent();

    	sourceWriter.println("isExecutionOK = false;");
		sourceWriter.println("streamWriter.writeObject(e);");

		sourceWriter.outdent();
    	sourceWriter.println("}");
    	sourceWriter.outdent();
    	sourceWriter.println("}");
	}

	/**
	 * @param sourceWriter
	 */
	private void generateCrossDocInvokeExceptionHandlingBlock(SourceWriter sourceWriter)
    {
	    sourceWriter.println("try{");
		sourceWriter.indent();
    	sourceWriter.println("isExecutionOK = false;");
		sourceWriter.println("streamWriter.writeObject(ex);");
		sourceWriter.outdent();
		sourceWriter.println("}catch (Exception ex2){");
		sourceWriter.indent();
		sourceWriter.println("return Crux.getMessages().crossDocumentInvocationGeneralError(ex2.getMessage());"); 
		sourceWriter.outdent();
		sourceWriter.println("}");
    }

	/**
	 * @param sourceWriter
	 */
	private void generateCrossDocInvokeMethod(SourceWriter sourceWriter)
	{
		sourceWriter.println("public String invoke(String serializedData){ ");
		sourceWriter.indent();

		sourceWriter.println(getProxySimpleName()+" wrapper = "+(isSingleton?"this":"new "+getProxySimpleName()+"()")+";");
		
		sourceWriter.println("boolean isExecutionOK = true;");

		sourceWriter.println(SerializationStreamWriter.class.getSimpleName()+" streamWriter = createStreamWriter();");
		sourceWriter.println("try{");
		sourceWriter.indent();

		generateMethodIdentificationBlock(sourceWriter);
		
		sourceWriter.println(SerializationStreamReader.class.getSimpleName()+" streamReader = null;");
		sourceWriter.println("if(serializedData.length() > 0){");
		sourceWriter.indent();
		sourceWriter.println("streamReader = createStreamReader(serializedData);");
		sourceWriter.outdent();
		sourceWriter.println("}");
		
		if (isAutoBindEnabled)
		{
			sourceWriter.println("wrapper.updateControllerObjects();");
		}

		boolean first = true;
		JMethod[] methods = baseProxyType.getOverridableMethods();
		for (JMethod method : methods)
		{
			if (!first)
			{
				sourceWriter.print("else ");
			}

			generateCrosDocInvokeBlockForMethod(sourceWriter, method);

			first = false;
		}
		if (!first)
		{
			sourceWriter.println(" else {");
		}
		
		sourceWriter.println("return Crux.getMessages().crossDocumentMethodNotFound();"); 
		
    	if (!first)
		{
			sourceWriter.println("}");
		}

		if (!first && isAutoBindEnabled)
		{
			sourceWriter.println("wrapper.updateScreenWidgets();");
		}
		sourceWriter.outdent();
		sourceWriter.println("}catch (Exception ex){");
		sourceWriter.indent();
		generateCrossDocInvokeExceptionHandlingBlock(sourceWriter);
		sourceWriter.outdent();
		sourceWriter.println("}");

		sourceWriter.println("return (isExecutionOK?\"//OK\":\"//EX\")+streamWriter.toString();");
		sourceWriter.outdent();
		sourceWriter.println("}");
	}	
	
	
	/**
	 * @param logger
	 * @param sourceWriter
	 * @param controllerClass
	 * @param method
	 */
    @SuppressWarnings("deprecation")
	private void generateInvokeBlockForMethod(SourceWriter sourceWriter, JMethod method)
    {
	    if (method.getAnnotation(br.com.sysmap.crux.core.client.controller.ExposeOutOfModule.class) != null)
	    {
	    	sourceWriter.println("if (\""+method.getName()+"\".equals(metodo)) {");
			sourceWriter.indent();
	    }
	    else
	    {
	    	sourceWriter.println("if (\""+method.getName()+"\".equals(metodo) && !fromOutOfModule) {");
			sourceWriter.indent();
	    }
	    
	    boolean allowMultipleClicks = isAllowMultipleClicks(method);
	    if (!allowMultipleClicks)
	    {
			sourceWriter.println("if (!__runningMethods.containsKey(metodo)){");
			sourceWriter.indent();
	    	sourceWriter.println("__runningMethods.put(metodo,true);");
	    	sourceWriter.println("try{");
			sourceWriter.indent();
	    }
	    
	    Validate annot = method.getAnnotation(Validate.class);
	    if (annot != null)
	    {
	    	sourceWriter.println("try{");
			sourceWriter.indent();
	    	String validateMethod = annot.value();
	    	if (validateMethod == null || validateMethod.length() == 0)
	    	{
	    		String methodName = method.getName();
	    		methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
	    		validateMethod = "validate"+ methodName;
	    	}
	    	generateValidateMethodCall(method, validateMethod, sourceWriter);
			sourceWriter.outdent();
	    	sourceWriter.println("}catch (Throwable e){");
			sourceWriter.indent();
	    	sourceWriter.println("__runMethod = false;");
	    	sourceWriter.println("eventProcessor.setValidationMessage(e.getMessage());");
			sourceWriter.outdent();
	    	sourceWriter.println("}");
	    }
	    sourceWriter.println("if (__runMethod){");
		sourceWriter.indent();
	    sourceWriter.println("try{");
		sourceWriter.indent();
	    
	    boolean hasReturn = method.getReturnType().getErasedType() != JPrimitiveType.VOID;
		if (hasReturn)
	    {
	    	sourceWriter.println("eventProcessor.setHasReturn(true);");
	    	sourceWriter.println("eventProcessor.setReturnValue(");
	    }
	    generateMethodCall(method, sourceWriter, !hasReturn);
		if (hasReturn)
	    {
	    	sourceWriter.println(");");
	    }
	    
		sourceWriter.outdent();
	    sourceWriter.println("}catch (Throwable e){");
		sourceWriter.indent();
	    sourceWriter.println("eventProcessor.setException(e);");
		sourceWriter.outdent();
	    sourceWriter.println("}");
		sourceWriter.outdent();
	    sourceWriter.println("}");

	    if (!allowMultipleClicks)
	    {
			sourceWriter.outdent();
	    	sourceWriter.println("}finally{");
			sourceWriter.indent();
	    	sourceWriter.println("__runningMethods.remove(metodo);");
			sourceWriter.outdent();
	    	sourceWriter.println("}");
			sourceWriter.outdent();
	    	sourceWriter.println("}");
	    }
	    
		sourceWriter.outdent();
	    sourceWriter.println("}");
    }
	
	/**
	 * @param logger
	 * @param sourceWriter
	 * @param controllerClass
	 * @param className
	 * @param methods
	 * @param singleton
	 * @param autoBindEnabled
	 */
	private void generateInvokeMethod(SourceWriter sourceWriter)
    {
	    sourceWriter.println("public void invoke(String metodo, Object sourceEvent, boolean fromOutOfModule, EventProcessor eventProcessor) throws Exception{ ");
		sourceWriter.indent();
		sourceWriter.println("boolean __runMethod = true;");
		
		sourceWriter.println(getProxySimpleName()+" wrapper = "+(isSingleton?"this":"new "+getProxySimpleName()+"()")+";");
		
		if (isAutoBindEnabled)
		{
			sourceWriter.println("if (!__runningMethods.containsKey(metodo)){");
			sourceWriter.indent();
			sourceWriter.println("wrapper.updateControllerObjects();");
			sourceWriter.outdent();
			sourceWriter.println("}");
		}
		
		boolean first = true;
		JMethod[] methods = controllerClass.getOverridableMethods(); 
		for (JMethod method: methods) 
		{
			if (isControllerMethodSignatureValid(method))
			{
				if (!first)
				{
					sourceWriter.print("else ");
				}
				
				generateInvokeBlockForMethod(sourceWriter, method);

				first = false;
			}
		}
		if (!first)
		{
			sourceWriter.println(" else ");
		}
		sourceWriter.println("throw new Exception(\""+messages.errorInvokingGeneratedMethod()+" \"+metodo);");

		if (!first && isAutoBindEnabled)
		{
			sourceWriter.println("wrapper.updateScreenWidgets();");
		}		
		
		sourceWriter.outdent();
		sourceWriter.println("}");
    }
	
	/** 
	 * Generates the controller method call.
	 * @param method
	 * @param sourceWriter
	 */
	private void generateMethodCall(JMethod method, SourceWriter sourceWriter, boolean finalizeCommand)
	{
		JParameter[] params = method.getParameters();
		if (params != null && params.length == 1)
		{
			sourceWriter.print("wrapper."+method.getName()+"(("+params[0].getType().getParameterizedQualifiedSourceName()+")sourceEvent)");
		}
		else 
		{
			sourceWriter.print("wrapper."+method.getName()+"()");
		}
		if (finalizeCommand)
		{
			sourceWriter.print(";");
		}
	}	
	
	/**
	 * @param sourceWriter
	 */
	private void generateMethodIdentificationBlock(SourceWriter sourceWriter)
    {
	    sourceWriter.println("String methodCalled = null;");
		sourceWriter.println("int idx = serializedData.indexOf('|');");
		sourceWriter.println("if (idx > 0){");
		sourceWriter.indent();
		sourceWriter.println("methodCalled = serializedData.substring(0,idx);");
		sourceWriter.println("serializedData = serializedData.substring(idx+1);");
		sourceWriter.outdent();
		sourceWriter.println("}else{");
		sourceWriter.indent();
		sourceWriter.println("return Crux.getMessages().crossDocumentCanNotIdentifyMethod();"); 
		sourceWriter.outdent();
		sourceWriter.println("}");
    }	

	/**
	 * 
	 * @param logger
	 * @param controllerClass
	 * @param method
	 * @param validateMethod
	 * @param sourceWriter
	 */
	private void generateValidateMethodCall(JMethod method, String validateMethod, SourceWriter sourceWriter)
	{
		JParameter[] params = method.getParameters();
		try
		{
			JMethod validate = null;
			if (params != null && params.length == 1)
			{
				validate = controllerClass.findMethod(validateMethod, new JType[]{params[0].getType()});
				if(validate == null)
				{
					validate = controllerClass.findMethod(validateMethod, new JType[]{});
				}
			}
			else
			{
				validate = controllerClass.findMethod(validateMethod, new JType[]{});
			}
			generateMethodCall(validate, sourceWriter, true);
		}
		catch (Exception e)
		{
			logger.log(TreeLogger.ERROR, messages.errorGeneratingRegisteredControllerInvalidValidateMethod(validateMethod), e);
		}
	}
	
	/**
	 * @param method
	 * @return
	 */
    @SuppressWarnings("deprecation")
    private boolean isAllowMultipleClicks(JMethod method)
    {
	    Expose exposeAnnot = method.getAnnotation(Expose.class);
	    if (exposeAnnot != null)
	    {
	    	return exposeAnnot.allowMultipleCalls();
	    }
		
	    br.com.sysmap.crux.core.client.controller.ExposeOutOfModule exposeOutAnnot = method.getAnnotation(br.com.sysmap.crux.core.client.controller.ExposeOutOfModule.class);
	    if (exposeOutAnnot != null)
	    {
	    	return exposeOutAnnot.allowMultipleCalls();
	    }
		return false;
    }
	
	/**
	 * Verify if a method must be included in the list of callable methods in the 
	 * generated invoker class
	 * @param method
	 * @return
	 */
	@SuppressWarnings("deprecation")
    private boolean isControllerMethodSignatureValid(JMethod method)
	{
		try
        {
	        if (!method.isPublic())
	        {
	        	return false;
	        }
	        
	        JParameter[] parameters = method.getParameters();
	        if (parameters != null && parameters.length != 0 && parameters.length != 1)
	        {
	        	return false;
	        }
	        if (parameters != null && parameters.length == 1)
	        {
	        	JClassType gwtEventType = controllerClass.getOracle().getType(GwtEvent.class.getCanonicalName());
	        	JClassType cruxEventType = controllerClass.getOracle().getType(CruxEvent.class.getCanonicalName());
	        	JClassType parameterType = parameters[0].getType().isClassOrInterface();
	        	if (parameterType == null || (!gwtEventType.isAssignableFrom(parameterType) && !cruxEventType.isAssignableFrom(parameterType)))
	        	{
	        		return false;
	        	}
	        }
	        
	        JClassType objectType = controllerClass.getOracle().getType(Object.class.getCanonicalName());
	        if (method.getEnclosingType().equals(objectType))
	        {
	        	return false;
	        }
	        
	        if (method.getAnnotation(Expose.class) == null && method.getAnnotation(br.com.sysmap.crux.core.client.controller.ExposeOutOfModule.class) == null)
	        {
	        	return false;
	        }
	        
	        return true;
        }
        catch (NotFoundException e)
        {
        	throw new CruxGeneratorException(e.getMessage(), e);
        }
	}
}
