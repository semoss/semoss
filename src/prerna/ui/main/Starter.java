package prerna.ui.main;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.ColorUIResource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prerna.rdf.engine.api.IEngine;
import prerna.ui.components.PlayPane;
import prerna.util.AbstractFileWatcher;
import prerna.util.Constants;
import prerna.util.DIHelper;

import com.ibm.icu.util.StringTokenizer;

public class Starter {

	// read the properties file - DBCM_RDF_Map.Prop
	/* create the PlayPane
	 * 1. Read the perspective properties to get all the perspectives
	 * 2. For each of the perspectives read all the question numbers
	 * 3. For each question get the description
	 * 4. Convert this into a 2 dimensional Hashtable Hash1 - Perspective Questions, Hash2 for each question description and layout classes
	 * 5. Set this information into the util DIHelper class
	 * 6. Populate the perspective combo-boxes with all the information retrieved on perspectives
	 * 7. Create the UI
	*/
	
	Object monitor = new Object();
	
	public static void main(String [] args) throws Exception
	{
		Starter starter = new Starter();
		String workingDir = System.getProperty("user.dir");
		String propFile = workingDir + "/RDF_Map.prop";
		Logger logger = Logger.getLogger(prerna.ui.main.Starter.class);
		//Object monitor = new Object(); // stupid object for being a monitor
		
		//logger.setLevel(Level.INFO);
		PropertyConfigurator.configure(workingDir + "/log4j.prop");
		
		// Nimbus me
		
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			if ("Nimbus".equals(info.getName())) {
				logger.info("Got the nimbus");
				UIManager.setLookAndFeel(info.getClassName());
				//Pretty Colors.
				UIManager.put("nimbusSelectionBackground", new Color(67,144,212)); //Light blue for selection
				//UIManager.put("nimbusBase", new Color(102,161,210)); //Light blue for everything else
				//UIManager.put("control", new Color(225,225,225)); //Light gray for the top bars behind the tabs
				//UIManager.put("nimbusBlueGrey", new Color(150,150,150)); //Separator bar, and disabled fields.
				UIManager.put("controlDkShadow", new Color(100,100,100)); //Color of scroll icons arrows
				UIManager.put("controlHighlight", new Color(100,100,100)); //Color of scroll icons highlights 
				UIManager.put("ProgressBar.repaintInterval", new Integer(100));//speed of indeterminate progress bar
				UIManager.put("ProgressBar.cycleTime", new Integer(1300));

				//UIManager.put("text", new Color(50,50,50)); //Color of text
				
				
				
				
				
				
				// comment this for nimbus
				//UIManager.setLookAndFeel ( WebLookAndFeel.class.getCanonicalName () );	
				UIDefaults defaults = UIManager.getLookAndFeelDefaults();
				
				UIDefaults tabPaneDefaults = new UIDefaults();
			    	tabPaneDefaults.put("TabbedPane.background", new ColorUIResource(Color.red));
		  	    //UIUtils.setPreferredLookAndFeel();

				//defaults.put("nimbusOrange",defaults.get("nimbusInfoBlue"));
				//defaults.put("Button.background",  Color.white);
				//defaults.put("TabbedPane.background", new Color(0,0,0));
				defaults.put("ToolTip.background", Color.LIGHT_GRAY);
				defaults.put("ToolTip[Enabled].backgroundPainter", null);
				defaults.put("ToolTip.contentMargins", new Insets(3,3,3,3));
				
				
				 
				break;
				}
			}
		} catch (Exception ignored) 
		{
			// handle exception
		} 

		// first get the engine file
		DIHelper.getInstance().loadCoreProp(propFile);
		
		// get the engine name
		//String engines = DIHelper.getInstance().getProperty(Constants.ENGINES);
		String engines = "";
		
		DIHelper.getInstance().setLocalProperty(Constants.ENGINES, engines);
		
		PlayPane frame = new PlayPane();
		frame.setVisible(true);
		SplashScreen ss = new SplashScreen();
		frame.start();
		
		// need to parameterize this sucker and let it roll
		// ok Load the ENGINE watcher first
		String watcherStr = DIHelper.getInstance().getProperty(Constants.ENGINE_WATCHER);
		System.out.println(watcherStr);
		StringTokenizer watchers = new StringTokenizer(watcherStr, ";");
		while(watchers.hasMoreElements())
		{
			String watcher = watchers.nextToken();
			String watcherClass = DIHelper.getInstance().getProperty(watcher);
			String folder = DIHelper.getInstance().getProperty(watcher + "_DIR");
			String ext = DIHelper.getInstance().getProperty(watcher + "_EXT");
			AbstractFileWatcher watcherInstance = (AbstractFileWatcher)Class.forName(watcherClass).getConstructor(null).newInstance(null);
			watcherInstance.setMonitor(starter.monitor);
			watcherInstance.setFolderToWatch(folder);
			watcherInstance.setExtension(ext);
			synchronized(starter.monitor)
			{
				watcherInstance.loadFirst();
				Thread thread = new Thread(watcherInstance);
				thread.start();
			}
		}
		
		// get this into a synchronized block
		// so this guy will wait
		// I do this so that I can get reference to the engine when I need it
		synchronized(starter.monitor)
		{
			watcherStr = DIHelper.getInstance().getProperty(Constants.WATCHERS);
			if(watcherStr != null )
			{
				watchers = new StringTokenizer(watcherStr, ";");
				while(watchers.hasMoreElements())
				{
					String watcher = watchers.nextToken();
					String watcherClass = DIHelper.getInstance().getProperty(watcher);
					String folder = DIHelper.getInstance().getProperty(watcher + "_DIR");
					String ext = DIHelper.getInstance().getProperty(watcher + "_EXT");
					String engineName = DIHelper.getInstance().getProperty(watcher+"_ENGINE");
					try
					{
						AbstractFileWatcher watcherInstance = (AbstractFileWatcher)Class.forName(watcherClass).getConstructor(null).newInstance(null);
						// engines should be loaded by now
						// hopefully :D
						if(engineName != null && DIHelper.getInstance().getLocalProp(engineName) != null)
						{
							IEngine engine = (IEngine)DIHelper.getInstance().getLocalProp(engineName);
							watcherInstance.setEngine(engine);
						}
						watcherInstance.setMonitor(starter.monitor);
						watcherInstance.setFolderToWatch(folder);
						watcherInstance.setExtension(ext);
						watcherInstance.loadFirst();
						Thread thread = new Thread(watcherInstance);
						thread.start();
					}catch(Exception ex)
					{
						// ok dont do anything the file was not there
					}
				}
			}
		}
		
		ss.setVisible(false);
	}
}
