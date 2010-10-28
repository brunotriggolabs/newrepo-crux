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
package br.com.sysmap.crux.core.rebind.scanner.screen;
 
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.sysmap.crux.core.client.utils.StringUtils;
import br.com.sysmap.crux.core.i18n.MessagesFactory;
import br.com.sysmap.crux.core.rebind.GeneratorMessages;
import br.com.sysmap.crux.core.rebind.controller.ClientControllers;
import br.com.sysmap.crux.core.rebind.scanner.screen.datasource.DataSources;
import br.com.sysmap.crux.core.rebind.scanner.screen.formatter.Formatters;
import br.com.sysmap.crux.core.utils.RegexpPatterns;
import br.com.sysmap.crux.core.utils.XMLUtils;
import br.com.sysmap.crux.core.utils.XMLUtils.XMLException;

/**
 * Factory for screens at the application's server side. It is necessary for GWT generators 
 * and for parameters binding.
 *  
 * @author Thiago Bustamante
 *
 */
public class ScreenFactory 
{
	private static ScreenFactory instance = new ScreenFactory();
	private static final Log logger = LogFactory.getLog(ScreenFactory.class);
	
	private static GeneratorMessages messages = (GeneratorMessages)MessagesFactory.getMessages(GeneratorMessages.class);
	private static final Lock screenLock = new ReentrantLock();	

	private Map<String, Screen> screenCache = new HashMap<String, Screen>();	
		
	/**
	 * Singleton Constructor
	 */
	private ScreenFactory() 
	{
	}
	
	/**
	 * Singleton method
	 * @return
	 */
	public static ScreenFactory getInstance()
	{
		return instance;
	}
	
	/**
	 * Factory method for screens.
	 * @param id
	 * @return
	 * @throws ScreenConfigException
	 */
	public Screen getScreen(String id) throws ScreenConfigException
	{
		try 
		{
			Screen screen = screenCache.get(id);
			if (screen != null)
			{
				return screen;
			}

			InputStream stream = ScreenResourceResolverInitializer.getScreenResourceResolver().getScreenResource(id);
			if (stream == null)
			{
				throw new ScreenConfigException(messages.screenFactoryScreeResourceNotFound(id));
			}
			
			screenLock.lock();
			try
			{
				if (screenCache.get(id) == null)
				{
					screen = parseScreen(id, stream);
					if(screen != null)
					{
						screenCache.put(id, screen);
					}
				}
			}
			finally
			{
				screenLock.unlock();
			}
			return screenCache.get(id);
			
		} 
		catch (Throwable e) 
		{
			throw new ScreenConfigException(messages.screenFactoryErrorRetrievingScreen(id, e.getLocalizedMessage()), e);
		}
	}

	/**
	 * Creates a widget based in its &lt;span&gt; tag definition.
	 * 
	 * @param element
	 * @param screen
	 * @return
	 * @throws ScreenConfigException
	 */
	private Widget createWidget(JSONObject elem, Screen screen) throws ScreenConfigException
	{
		if (!elem.has("id"))
		{
			throw new ScreenConfigException(messages.screenFactoryWidgetIdRequired());
		}
		String widgetId;
        try
        {
	        widgetId = elem.getString("id");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(messages.screenFactoryWidgetIdRequired());
        }
		Widget widget = screen.getWidget(widgetId);
		if (widget != null)
		{
			throw new ScreenConfigException(messages.screenFactoryErrorDuplicatedWidget(widgetId));
		}
		
		widget = newWidget(elem, widgetId);
		if (widget == null)
		{
			throw new ScreenConfigException(messages.screenFactoryErrorCreateWidget(widgetId));
		}
		
		screen.addWidget(widget);
		
		createWidgetChildren(elem, screen, widgetId, widget);
		
		return widget;
	}

	/**
	 * @param elem
	 * @param screen
	 * @param widgetId
	 * @param widget
	 * @throws ScreenConfigException
	 */
	private void createWidgetChildren(JSONObject elem, Screen screen, String widgetId, Widget widget) throws ScreenConfigException
    {
	    if (elem.has("children"))
		{
			try
            {
	            JSONArray children = elem.getJSONArray("children");
	            if (children != null)
	            {
	            	for (int i=0; i< children.length(); i++)
	            	{
	            		JSONObject childElem = children.getJSONObject(i);
	            		if (isValidWidget(childElem))
	            		{
	            			Widget child = createWidget(childElem, screen);
	            			child.setParent(widget);
	            		}
	            		else
	            		{
	            			createWidgetChildren(childElem, screen, widgetId, widget);
	            		}
	            	}
	            }
            }//TODO tratar o HTMLPanel no xslt.
			//TODO tratar text dentro de tags com namespace crux (filhos de widgets)...colocar como uma propriedade chamada "_text" no elemento JSON da tag
			//TODO fazer trim no texto interno dos spans no xslt. Fazer scape de " ao montar o array json
            catch (JSONException e)
            {
    			throw new ScreenConfigException(messages.screenFactoryErrorCreateWidget(widgetId), e);
            }
		}
    }

	/**
	 * @param nodeList
	 * @return
	 * @throws ScreenConfigException
	 */
	private String getScreenModule(NodeList nodeList) throws ScreenConfigException
	{
		String result = null;
		
		int length = nodeList.getLength();
		for (int i = 0; i < length; i++)
		{
			Element item = (Element) nodeList.item(i);
			
			String src = item.getAttribute("src");
			
			if (src != null && src.endsWith(".nocache.js"))
			{
				if (result != null)
				{
					throw new ScreenConfigException(messages.screenFactoryErrorMultipleModulesOnPage());
				}
				
				int lastSlash = src.lastIndexOf("/");
				
				if(lastSlash >= 0)
				{
					int firstDotAfterSlash = src.indexOf(".", lastSlash);
					result = src.substring(lastSlash + 1, firstDotAfterSlash);
				}
				else
				{
					int firstDot = src.indexOf(".");
					result = src.substring(0, firstDot);
				}
			}
		}
		return result;
	}
	
	/**
	 * Test if a target json object represents a Screen definition for Crux.
	 * @param cruxObject
	 * @return
	 * @throws JSONException 
	 */
	private boolean isScreenDefinition(JSONObject cruxObject) throws JSONException
	{
		if (cruxObject.has("type"))
		{
			String type = cruxObject.getString("type");
			return (type != null && "screen".equals(type));
		}
		return false;
	}
	
	/**
	 * Test if a target json object represents a widget definition for Crux.
	 * @param cruxObject
	 * @return
	 * @throws JSONException
	 */
	private boolean isValidWidget(JSONObject cruxObject) throws JSONException
	{
		if (cruxObject.has("type"))
		{
			String type = cruxObject.getString("type");
			return (type != null && !"screen".equals(type));
		}
		return false;
	}
	
	
	/**
	 * Builds a new widget, based on its &lt;span&gt; tag definition.
	 * @param element
	 * @param widgetId
	 * @return
	 * @throws ScreenConfigException
	 */
	private Widget newWidget(JSONObject elem, String widgetId) throws ScreenConfigException
	{
		try 
		{
			String type = elem.getString("type");
			WidgetParser parser = new WidgetParserImpl();
			Widget widget = new Widget();
			widget.setId(widgetId);
			widget.setType(type);
			parser.parse(widget, elem);
			return widget;
		} 
		catch (Throwable e) 
		{
			throw new ScreenConfigException(messages.screenFactoryErrorCreateWidget(widgetId), e);
		} 
	}
	
	/**
	 * Parse the HTML page and build the Crux Screen. 
	 * @param id
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws ScreenConfigException 
	 */
	private Screen parseScreen(String id, InputStream stream) throws IOException, ScreenConfigException
	{
		Screen screen = null;
		Document source = null;
		
		try
		{
			source = XMLUtils.createNSUnawareDocument(stream);
		}
		catch (XMLException e)
		{
			logger.error(e.getMessage(), e);
			throw new ScreenConfigException(messages.screenFactoryErrorParsingScreen(id, e.getMessage()));
		}

		String screenModule = getScreenModule(source.getElementsByTagName("script"));
		
		try
        {
	        if(screenModule != null)
	        {
	        	screen = new Screen(id, screenModule);
	        	
	        	JSONArray metaData = getMetaData(source, id);
	        	
	        	int length = metaData.length();
	        	for (int i = 0; i < length; i++) 
	        	{
	        		JSONObject compCandidate = metaData.getJSONObject(i);
	        		
	        		if (isScreenDefinition(compCandidate))
	        		{
	        			parseScreenElement(screen,compCandidate);
	        		}
	        		else if (isValidWidget(compCandidate))
	        		{
	        			try 
	        			{
	        				createWidget(compCandidate, screen);
	        			} 
	        			catch (ScreenConfigException e) 
	        			{
	        				logger.error(messages.screenFactoryGenericErrorCreateWidget(id, e.getLocalizedMessage()));
	        			}
	        		}
	        	}
	        }
        }
        catch (JSONException e)
        {
        	throw new ScreenConfigException(messages.screenFactoryErrorParsingScreen(id, e.getMessage()));
        }
		
		return screen;
	}

	/**
	 * @param source
	 * @return
	 * @throws JSONException
	 * @throws ScreenConfigException 
	 */
	private JSONArray getMetaData(Document source, String id) throws JSONException, ScreenConfigException
    {
		
		Element cruxMetaData = getCruxMetaDataElement(source);
		
	    String metaData = cruxMetaData.getTextContent();

	    if (metaData != null)
	    {
	    	metaData = metaData.trim();
	    	if (metaData.startsWith("<!--"))
	    	{
	    		metaData = metaData.substring(4).trim();
	    		if (metaData.endsWith("-->"))
	    		{
	    			metaData = metaData.substring(0, metaData.length()-3);
	    		}
	    	}
	    	
	    	JSONArray meta = new JSONArray(metaData);
	    	return meta;
	    }
	    
	    throw new ScreenConfigException(messages.screenFactoryErrorParsingScreenMetaData(id));
    }

	/**
	 * @param source
	 * @return
	 */
	private Element getCruxMetaDataElement(Document source)
    {
		NodeList nodeList = source.getElementsByTagName("div");
		for (int i=0; i< nodeList.getLength(); i++)
		{
			Element item = (Element) nodeList.item(i);
			String metaDataId = item.getAttribute("id");
			if (metaDataId != null && metaDataId.equals("__CruxMetaData_"))
			{
				return item;
			}
		}
	    return null;
    }

	/**
	 * Parse screen element
	 * @param screen
	 * @param compCandidate
	 * @throws ScreenConfigException 
	 */
	private void parseScreenElement(Screen screen, JSONObject elem) throws ScreenConfigException 
	{
		try
        {
	        String[] attributes = JSONObject.getNames(elem);
	        int length = attributes.length;
	        
	        for (int i = 0; i < length; i++) 
	        {
	        	String attrName = attributes[i];
	        	
	        	if(attrName.equals("useController"))
	        	{
	        		parseScreenUseControllerAttribute(screen, elem);
	        	}
	        	else if(attrName.equals("useSerializable"))
	        	{
	        		parseScreenUseSerializableAttribute(screen, elem);
	        	}
	        	else if(attrName.equals("useFormatter"))
	        	{
	        		parseScreenUseFormatterAttribute(screen, elem);
	        	}
	        	else if(attrName.equals("useDataSource"))
	        	{
	        		parseScreenUseDatasourceAttribute(screen, elem);
	        	}
	        	else if (attrName.startsWith("on"))
	        	{
	        		Event event = EventFactory.getEvent(attrName, elem.getString(attrName));
	        		if (event != null)
	        		{
	        			screen.addEvent(event);
	        		}
	        	}
	        	else if (attrName.equals("title"))
	        	{
	        		String title = elem.getString(attrName);
	        		if (title != null && title.length() > 0)
	        		{
	        			screen.setTitle(title);
	        		}
	        	}
	        	else if (!attrName.equals("id") && !attrName.equals("type"))
	        	{
	        		if (logger.isDebugEnabled()) logger.debug(messages.screenPropertyError(attrName.substring(1), screen.getId()));
	        	}
	        }
        }
        catch (JSONException e)
        {
	        throw new ScreenConfigException(messages.screenFactoryErrorParsingScreenMetaData(screen.getId()));
        }
	}

	/**
	 * @param screen
	 * @param attr
	 * @throws ScreenConfigException 
	 */
	private void parseScreenUseDatasourceAttribute(Screen screen, JSONObject elem) throws ScreenConfigException
    {
	    String datasourceStr;
        try
        {
        	datasourceStr = elem.getString("useDataSource");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(e);
        }
	    if (datasourceStr != null)
	    {
	    	String[] datasources = RegexpPatterns.REGEXP_COMMA.split(datasourceStr);
	    	for (String datasource : datasources)
	    	{
	    		datasource = datasource.trim();
	    		if (!StringUtils.isEmpty(datasource))
	    		{
	    			if (DataSources.getDataSource(datasource) == null)
	    			{
	    				throw new ScreenConfigException(messages.screenFactoryInvalidDataSource(datasource, screen.getId()));
	    			}
	    			screen.addDataSource(datasource);
	    		}
	    	}
	    }
    }

	/**
	 * @param screen
	 * @param attr
	 * @throws ScreenConfigException 
	 */
	private void parseScreenUseFormatterAttribute(Screen screen, JSONObject elem) throws ScreenConfigException
    {
	    String formatterStr;
        try
        {
        	formatterStr = elem.getString("useFormatter");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(e);
        }
	    if (formatterStr != null)
	    {
	    	String[] formatters = RegexpPatterns.REGEXP_COMMA.split(formatterStr);
	    	for (String formatter : formatters)
	    	{
	    		formatter = formatter.trim();
	    		if (!StringUtils.isEmpty(formatter))
	    		{
	    			if (Formatters.getFormatter(formatter) == null)
	    			{
	    				throw new ScreenConfigException(messages.screenFactoryInvalidFormatter(formatter, screen.getId()));
	    			}
	    			screen.addFormatter(formatter);
	    		}
	    	}
	    }
    }

	/**
	 * @param screen
	 * @param attr
	 * @throws ScreenConfigException 
	 */
	@SuppressWarnings("deprecation")
    private void parseScreenUseSerializableAttribute(Screen screen, JSONObject elem) throws ScreenConfigException
    {
	    String serializerStr;
        try
        {
	        serializerStr = elem.getString("useSerializable");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(e);
        }
	    if (serializerStr != null)
	    {
	    	String[] serializers = RegexpPatterns.REGEXP_COMMA.split(serializerStr);
	    	for (String serializer : serializers)
	    	{
	    		serializer = serializer.trim();
	    		if (!StringUtils.isEmpty(serializer))
	    		{
	    			if (br.com.sysmap.crux.core.rebind.scanner.screen.serializable.Serializers.getCruxSerializable(serializer) == null)
	    			{
	    				throw new ScreenConfigException(messages.screenFactoryInvalidSerializable(serializer, screen.getId()));
	    			}
	    			screen.addSerializer(serializer);
	    		}
	    	}
	    }
    }

	/**
	 * @param screen
	 * @param attr
	 * @throws ScreenConfigException 
	 */
	private void parseScreenUseControllerAttribute(Screen screen, JSONObject elem) throws ScreenConfigException
    {
	    String handlerStr;
        try
        {
	        handlerStr = elem.getString("useController");
        }
        catch (JSONException e)
        {
        	throw new ScreenConfigException(e);
        }
	    if (handlerStr != null)
	    {
	    	String[] handlers = RegexpPatterns.REGEXP_COMMA.split(handlerStr);
	    	for (String handler : handlers)
	    	{
	    		handler = handler.trim();
	    		if (!StringUtils.isEmpty(handler))
	    		{
	    			if (ClientControllers.getController(handler) == null)
	    			{
	    				throw new ScreenConfigException(messages.screenFactoryInvalidController(handler, screen.getId()));
	    			}
	    			screen.addController(handler);
	    		}
	    	}
	    }
    }
}
