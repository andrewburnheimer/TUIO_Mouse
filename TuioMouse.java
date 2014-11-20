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

public class TuioMouse implements TuioListener {
	private static final long LOCK_EXPIRY = 10 * 1000;
	private static final long DOUBLE_CLICK_TOLERANCE = 200;
	private Robot robot = null;
	private int width = 0;
	private int height = 0;
	private long mouse = -1;
	private Date locked;
	private Logger logger;
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
																DOUBLE_CLICK_TOLERANCE) ) )
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
		PropertyConfigurator.configureAndWatch("log4jna_tuio.properties");
		logger = LogManager.getLogger(TuioMouse.class);
		try {
			logger.info("TuioMouse process started...");
		} catch (Throwable t) {
			System.out.println("Failed to report event to OS");
			t.printStackTrace();
		}
		
		try { robot = new Robot(); }
		catch (Exception e) {
			String message = "failed to initialize mouse robot";
			logger.fatal(message);
			System.out.println(message);
			System.exit(0);
		}

		width  = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		locked = new Date(0);
		lastClick = new Date(0);
	}

	public static void main(String argv[]) {

		int port = 3333;

		if (argv.length==1) {
			try { port = Integer.parseInt(argv[1]); }
			catch (Exception e) { System.out.println("usage: java TuioMouse [port]"); }
		}

 		TuioMouse mouse = new TuioMouse();
		TuioClient client = new TuioClient(port);

		System.out.println("listening to TUIO messages at port "+port);
		client.addTuioListener(mouse);
		client.connect();
	}
}
