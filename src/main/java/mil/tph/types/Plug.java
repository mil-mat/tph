package mil.tph.types;

import mil.tph.Application;
import mil.tph.util.APIUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Plug implements Serializable {

	public static final String SAVE_DIRECTORY = System.getProperty("user.dir") + "/data/devices/";

	static {
		new File(SAVE_DIRECTORY).mkdirs();
	}

	private final String _id;
	private String _name;
	private boolean _state;

	public Plug(String id, String name, boolean state) {
		this._id = id;
		this._name = name;
		this._state = state;
	}

	/**
	 * Toggles the state of this switch through Tuya's API.
	 * Refreshes the GUI.
	 */
	public void toggle(Token token) throws IOException {
		APIUtil.setState(token, this, !_state);
		setState(!_state);
		writeFile();

		Application.getFrame().refresh();
	}

	private static Plug readFile(File file) {
		try {
			return (Plug) new ObjectInputStream(new FileInputStream(file)).readObject();
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to read from device file! (" + file.getAbsolutePath() + ")");
			e.printStackTrace();
			return null;
		}
	}

	public static List<Plug> readAllFiles() {
		List<Plug> devices = new ArrayList<>();
		for (File f : Objects.requireNonNull(new File(SAVE_DIRECTORY).listFiles())) {
			devices.add(readFile(f));
		}
		return devices;
	}

	public void setState(boolean state) {
		this._state = state;
	}

	public void writeFile() throws IOException {
		try {
			ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream(SAVE_DIRECTORY + getId()));
			oStream.writeObject(this);
			oStream.close();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to write switch file! (" + getId() + ")");
			e.printStackTrace();
		}
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public boolean getState() {
		return _state;
	}

	@Override
	public String toString() {
		return "Device{" +
				"_id='" + _id + '\'' +
				", _name='" + _name + '\'' +
				", _state=" + _state +
				'}';
	}

}
