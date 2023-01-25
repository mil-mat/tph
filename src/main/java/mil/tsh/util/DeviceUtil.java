package mil.tsh.util;

import mil.tsh.types.Switch;
import mil.tsh.types.Token;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeviceUtil {

	public static final int REFRESH_DEVICES_FROM_API_EVERY_MINUTES = 60;

	private final Map<String, Switch> _knownDevices = new HashMap<>(); // Key is ID
	private Token _token;

	public DeviceUtil(Token token) {
		this._token = token;

		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(this::updateFromAPI, 0, REFRESH_DEVICES_FROM_API_EVERY_MINUTES, TimeUnit.MINUTES);
	}

	public Token getToken() {
		return _token;
	}

	public void setToken(Token token) {
		_token = token;
	}

	public Optional<Switch> fromName(String name) {
		return _knownDevices.values().stream().filter(e -> e.getName().equals(name)).findFirst();
	}

	public Switch getDevice(String id) {
		return _knownDevices.get(id);
	}

	public List<Switch> getKnownDevices() {
		return new ArrayList<>(_knownDevices.values());
	}

	public boolean knowsDevice(String id) {
		return _knownDevices.containsKey(id);
	}

	/** Writes all known devices to disk, deleting unknown ones. */
	public void writeFiles() {
		for (File file : Objects.requireNonNull(new File(Switch.SAVE_DIRECTORY).listFiles())) {
			if (!_knownDevices.containsKey(file.getName()))
				file.delete(); // Delete files of switches that no longer exist
		}

		_knownDevices.values().forEach(d -> {
			try {
				d.writeFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/** Updates the list of known devices from the device files saved on disk. */
	public void updateFromDisk() {
		Switch.readAllFiles().forEach(d -> _knownDevices.put(d.getId(), d));
	}

	/** Updates the list of known devices from the Tuya API. */
	public void updateFromAPI() {
		List<Switch> devices = APIUtil.getDevices(_token);
		if (devices == null) {
			Logger.getGlobal().log(Level.WARNING, "Invalid device API response! Unable to update devices.");
			return;
		}
		_knownDevices.clear();
		devices.forEach(d -> _knownDevices.put(d.getId(), d));
		writeFiles();
	}

}
