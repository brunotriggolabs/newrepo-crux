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
package br.com.sysmap.crux.core.client.event.bind;

import br.com.sysmap.crux.core.client.event.Event;
import br.com.sysmap.crux.core.client.event.EventFactory;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;

/**
 * Helper Class for close events binding
 * @author Thiago Bustamante
 *
 */
public class CloseEvtBind extends EvtBind
{
	public static <I> void bindEvent(Element element, HasCloseHandlers<I> widget, final String componentId)
	{
		final Event eventClose = getComponentEvent(element, EventFactory.EVENT_CLOSE);
		if (eventClose != null)
		{
			widget.addCloseHandler(new CloseHandler<I>()
			{
				@Override
				public void onClose(CloseEvent<I> event) 
				{
					EventFactory.callEvent(eventClose, componentId);
				}
			});
		}
	}

}