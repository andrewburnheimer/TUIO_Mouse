/*
	TUIO Mouse Driver - part of the reacTIVision project
	http://reactivision.sourceforge.net/

	Copyright (c) 2005-2009 Martin Kaltenbrunner <mkalten@iua.upf.edu>
	Copyright (c) 2011 Andreas Willich <sabotageandi@gmail.com>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

import java.awt.*;
import java.awt.event.*;
import TUIO.*;
import java.util.Date;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.cli.*;

public class TuioMouse implements TuioListener {
	private static final long LOCK_EXPIRY = 10 * 1000;
	private static long doubleClickTolerance;
	private static String loggingConfigFile;
	private static Logger logger;
	private Robot robot = null;
	private int width = 0;
	private int height = 0;
	private long mouse = -1;
	private Date locked;
	private Date lastClick;

	public void addTuioObject(TuioObject tobj) {}
	public void updateTuioObject(TuioObject tobj) {}
	public void removeTuioObject(TuioObject tobj) {}
	public void refresh(TuioTime bundleTime) {}

	public void addTuioCursor(TuioCursor tcur) {
		Date now = new Date();
		// Test if lock is zeroed, or just old
		if ( now.after( new Date(locked.getTime() + LOCK_EXPIRY) ) ) {

			if ( locked.getTime() != 0 ) // the lock timed-out, report it
			{
				logger.warn("TuioCursor lock timed-out and is being reset now");
			}

			locked = now;
			// Block is entered on all single-touch events
			if (mouse<0)
			{
				logger.debug("addTuioCursor - single-touch: " + mouse);
				mouse = tcur.getSessionID();
				if (robot!=null &&
						now.after( new Date(lastClick.getTime() +
																doubleClickTolerance) ) )
				{
					robot.mouseMove(tcur.getScreenX(width),tcur.getScreenY(height));
				} else {
					logger.debug("Not moving, second of a double-click");
				}
				lastClick = now;
				if (robot!=null) robot.mousePress(InputEvent.BUTTON1_MASK);
			}
			else // Block entered on multi-touch events: once for two fingers
			     // twice for three fingers ... four times for five fingers.
			{
				logger.debug("addTuioCursor - multi-touch: " + mouse);
				if (robot != null) robot.mouseRelease(InputEvent.BUTTON1_MASK);
			}
			locked = new Date(0);
		}
	}

	public void updateTuioCursor(TuioCursor tcur) {
		if (mouse==tcur.getSessionID()) {
			if (robot!=null) robot.mouseMove(tcur.getScreenX(width),tcur.getScreenY(height));
		}
	}

	public void removeTuioCursor(TuioCursor tcur) {
		if (mouse==tcur.getSessionID()) {
			mouse=-1;
			if (robot!=null) robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}

	}

	public TuioMouse() {
		PropertyConfigurator.configureAndWatch(loggingConfigFile);
		logger = LogManager.getLogger(TuioMouse.class);
		
		try { robot = new Robot(); }
		catch (Exception e) {
			String message = "failed to initialize mouse robot";
			logger.fatal(message);
			System.out.println(message);
			System.exit(1);
		}

		width  = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		locked = new Date(0);
		lastClick = new Date(0);
	}

	public static void main(String argv[]) {
		CommandLineParser parser = new BasicParser();

		Options options = new Options();
		options.addOption("h", "help", false, "display this usage statement");
		options.addOption("l", "log-config", true, "filename with logging " +
				"configuration (Default: \"log4jna_tuio.properties\")");
		options.addOption("p", "port", true, "port number to run service " +
				"over (Default: 3333)");
		options.addOption("t", "tolerance", true, "milliseconds within " +
				"which double-clicks should occur (Default: 200)");
		int port = 0;

		try {
			CommandLine cmd = parser.parse( options, argv );

			if(cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "TuioMouse", options );
				System.exit(0);
			}

			if(cmd.hasOption("l")) {
				loggingConfigFile = cmd.getOptionValue("l");
			}
			else {
				loggingConfigFile = "log4jna_tuio.properties";
			}

			if(cmd.hasOption("p")) {
				port = Integer.parseInt(cmd.getOptionValue("p"));
			}
			else {
				port = 3333;
			}

			if(cmd.hasOption("t")) {
				doubleClickTolerance = Integer.parseInt(cmd.getOptionValue("t"));
			}
			else {
				doubleClickTolerance = 200;
			}
		}
		catch ( ParseException exp ) {
			System.out.println("Unexpected exception: " + exp.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "TuioMouse", options );
			System.exit(1);
		}

 		TuioMouse mouse = new TuioMouse();
		TuioClient client = new TuioClient(port);

		try {
			logger.info("TuioMouse process started on port " + port);
		} catch (Throwable t) {
			System.out.println("Unable to report events to OS");
			t.printStackTrace();
		}
		client.addTuioListener(mouse);
		client.connect();
	}
}
