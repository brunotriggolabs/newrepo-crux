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
package org.cruxframework.crux.core.client.screen;


import org.cruxframework.crux.core.client.event.CruxEvent;

import com.google.gwt.user.client.ui.Widget;


/**
 * @author Thiago da Rosa de Bustamante
 *
 */
public class WidgetLoadEvent extends CruxEvent<Widget>
{
	/**
	 * Constructor
	 */
	protected WidgetLoadEvent(Widget source, String widgetId)
	{
		super(source, widgetId);
	}
}
