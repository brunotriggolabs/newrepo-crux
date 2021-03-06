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
package org.cruxframework.crux.core.rebind.screen.widget.creator;

import org.cruxframework.crux.core.rebind.screen.widget.WidgetCreatorContext;
import org.cruxframework.crux.core.rebind.screen.widget.creator.event.BlurEvtBind;
import org.cruxframework.crux.core.rebind.screen.widget.creator.event.FocusEvtBind;
import org.cruxframework.crux.core.rebind.screen.widget.declarative.TagEvent;
import org.cruxframework.crux.core.rebind.screen.widget.declarative.TagEvents;

/**
 * A helper class to help on HasAllFocusHandlers widgets creation, based on crux pages metadata.
 * @author Thiago da Rosa de Bustamante
 */
@TagEvents({
	@TagEvent(value=FocusEvtBind.class, description="Inform the handler for onFocus event. This event is fired when the widget receives focus."),
	@TagEvent(value=BlurEvtBind.class, description="Inform the handler for onBlur event. This event is fired when the widget loses focus.")
})	
public interface HasAllFocusHandlersFactory<C extends WidgetCreatorContext>
{
}
