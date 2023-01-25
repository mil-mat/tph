package mil.tph;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Preferences implements Serializable {

	public static final String SAVE_PATH = System.getProperty("user.dir") + "/data/preferences";

	private boolean _startInTray = false;

	public static Preferences fromFile() {
		if (!fileExists()) {
			Preferences prefs = new Preferences();
			prefs.writeFile();
			return prefs;
		}

		try {
			return (Preferences) new ObjectInputStream(new FileInputStream(SAVE_PATH)).readObject();
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to read from preferences file!");
			e.printStackTrace();
			return null;
		}
	}

	public static boolean fileExists() {
		return new File(SAVE_PATH).exists();
	}

	public Preferences setStartInTray(boolean newValue) {
		_startInTray = newValue;
		writeFile();
		return this;
	}

	public boolean doesStartInTray() {
		return _startInTray;
	}

	public void writeFile() {
		try {
			ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream(SAVE_PATH));
			oStream.writeObject(this);
			oStream.close();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.WARNING, "Writing preferences file failed!");
			e.printStackTrace();
		}
	}

}
