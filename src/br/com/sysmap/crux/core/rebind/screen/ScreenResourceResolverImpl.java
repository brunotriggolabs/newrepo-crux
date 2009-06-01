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
package br.com.sysmap.crux.core.rebind.screen;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import br.com.sysmap.crux.core.client.component.InterfaceConfigException;
import br.com.sysmap.crux.core.i18n.MessagesFactory;
import br.com.sysmap.crux.core.server.ServerMessages;

/**
 * 
 * @author Thiago da Rosa de Bustamante <code>tr_bustamante@yahoo.com.br</code>
 */
public class ScreenResourceResolverImpl implements ScreenResourceResolver
{
	private static ServerMessages messages = (ServerMessages)MessagesFactory.getMessages(ServerMessages.class);

	public InputStream getScreenResource(String screenId) throws InterfaceConfigException
	{
		try
		{
			URL url = getClass().getResource("/"+screenId);
			if (url == null)
			{
				url = new File(screenId).toURI().toURL();
			}
			
			return url.openStream();
		}
		catch (Exception e)
		{
			throw new InterfaceConfigException(messages.screenResourceResolverFindResourceError(screenId, e.getMessage()), e);
		}
	}

}
