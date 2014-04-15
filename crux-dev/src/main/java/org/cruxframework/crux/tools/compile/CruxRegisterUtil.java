package org.cruxframework.crux.tools.compile;

import java.io.File;
import java.net.MalformedURLException;

import org.cruxframework.crux.core.rebind.DevelopmentScanners;
import org.cruxframework.crux.core.server.CruxBridge;

public class CruxRegisterUtil 
{
	public static void registerFilesCruxBridge(String[] args) throws MalformedURLException 
	{
		DevelopmentScanners.initializeScanners();
		
		String webDir = "./war/";
		if(args != null)
		{
			boolean warParamFound = false;
			for (int i=0; i< (args.length-1); i++)
			{
				String arg = args[i];
				if ("-webDir".equals(arg))
				{
					webDir = args[i+1];
					//change to -war
					args[i] = "-war";
				} 
				else if("-war".equals(arg))
				{
					webDir = args[i+1];
					if(warParamFound)
					{
						//kill this duplicated param
						args[i] = "";
						args[i+1] = "";
					}
					warParamFound = true;
				}
			}
		}
		
		File webinfClassesDir = new File(webDir, "WEB-INF/classes");
		File webinfLibDir = new File(webDir, "WEB-INF/lib");
		
		CruxBridge.getInstance().registerWebinfClasses(webinfClassesDir.toURI().toURL().toString());
		CruxBridge.getInstance().registerWebinfLib(webinfLibDir.toURI().toURL().toString());
	}
	
	public static String getModule(String[] args)
    {
		String startupUrl = null;
		String module = null;
		for (int i=0; i< (args.length-1); i++)
        {
			String arg = args[i];
	        if ("-startupUrl".equals(arg))
	        {
	        	startupUrl = args[i+1];
	        }
	        else if ("-module".equals(arg))
	        {
	        	module = args[i+1];
	        } 
        }
		if (module == null || module.length() == 0)
		{
			if (startupUrl != null && startupUrl.length() > 0)
			{
				int index = startupUrl.indexOf('/');
				if (index > 0)
				{
					module = startupUrl.substring(0, index);
				}
			}
		}
		
	    return module;
    }
}
