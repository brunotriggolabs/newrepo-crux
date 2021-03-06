/*
 * Copyright 2011 cruxframework.org.
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
package org.cruxframework.crux.classpath;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
public abstract class AbstractURLResourceHandler implements URLResourceHandler
{
	/**
	 * 
	 */
	public URL getChildResource(URL url, String path)
	{
		checkProtocol(url);
		
		try
		{
			String urlPath = url.toString();
			if (!urlPath.endsWith("/"))
			{
				urlPath += "/";
			}

			return new URL(urlPath+path);
		}
		catch (MalformedURLException e)
		{
			throw new URLResourceException(e.getLocalizedMessage(), e); 
		}
	}
	
	/**
	 * 
	 * @param url
	 */
	protected void checkProtocol(URL url)
	{
		if (!getProtocol().equalsIgnoreCase(url.getProtocol()))
		{
			throw new URLResourceException("Invalid Protocol: "+url.getProtocol());
		}
	}
}
