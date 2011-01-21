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
package br.com.sysmap.crux.core.rebind.widget.creator;

import br.com.sysmap.crux.core.client.declarative.TagEvent;
import br.com.sysmap.crux.core.client.declarative.TagEvents;
import br.com.sysmap.crux.core.client.screen.InterfaceConfigException;
import br.com.sysmap.crux.core.rebind.widget.WidgetCreatorContext;
import br.com.sysmap.crux.core.rebind.widget.creator.event.BlurEvtBind;
import br.com.sysmap.crux.core.rebind.widget.creator.event.FocusEvtBind;

import com.google.gwt.event.dom.client.HasAllFocusHandlers;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
public interface HasAllFocusHandlersFactory<T extends HasAllFocusHandlers, C extends WidgetCreatorContext>
{
	@TagEvents({
		@TagEvent(FocusEvtBind.class),
		@TagEvent(BlurEvtBind.class)
	})	
	void processEvents(C context) throws InterfaceConfigException;
}
