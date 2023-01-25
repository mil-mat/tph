package mil.tsh.types;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import mil.tsh.Application;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Hotkey implements Serializable, Cloneable {

	public static final int KEYCODE_AWAITING = -1;
	public static final int KEYCODE_UNBOUND = -2;

	private Set<Modifier> _modifiers;
	private int _keyCode;

	private Switch _device;

	public Hotkey(Switch device, int keyCode, Modifier... modifiers) {
		this._keyCode = keyCode;
		this._modifiers = new HashSet<>(Arrays.asList(modifiers));
		this._device = device;
	}

	/**
	 * @return A Human-Readable concatenation of the current keycode and modifiers.
	 */
	public String getReadableKeyCombination() {
		if (_keyCode == KEYCODE_AWAITING && _modifiers.isEmpty()) return "Awaiting Input...";
		if (_keyCode == KEYCODE_UNBOUND) return "Not Bound";

		StringBuilder builder = new StringBuilder();
		getModifiers().forEach(m -> builder.append(m.getName()).append(" "));
		if (_keyCode != KEYCODE_AWAITING) { // Check if keycode is valid - if it isn't, don't append it.
			builder.append(NativeKeyEvent.getKeyText(_keyCode));
		}
		return builder.toString();
	}

	/**
	 * Facilitates setting keycode and modifiers in one go, allowing for a singular refresh of the GUI.
	 */
	public void setKeyCombination(int keyCode, Set<Modifier> modifiers) {
		_modifiers = modifiers;
		_keyCode = keyCode;
		Application.getFrame().refresh();
	}

	public Set<Modifier> getModifiers() {
		return _modifiers;
	}

	/** Sets modifier keys and refreshes the GUI. */
	public void setModifiers(Set<Modifier> modifiers) {
		_modifiers = modifiers;
		Application.getFrame().refresh();
	}

	public int getKeyCode() {
		return _keyCode;
	}

	/** Sets keycode and refreshes the GUI. */
	public void setKeyCode(int keyCode) {
		_keyCode = keyCode;
		Application.getFrame().refresh();
	}

	public Switch getDevice() {
		return _device;
	}

	/** Sets device and refreshes the GUI. */
	public void setDevice(Switch device) {
		this._device = device;
		Application.getFrame().refresh();
	}

	/** Attempts to toggle associated device. */
	public void perform() {
		try {
			_device.toggle(Application.getDeviceUtil().getToken());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Hotkey{" +
				"_modifiers=" + _modifiers.toString() +
				", _keyCode=" + _keyCode + "(" + NativeKeyEvent.getKeyText(_keyCode) + ")" +
				", device=" + _device.getName() +
				'}';
	}

	@Override
	public Hotkey clone() {
		return new Hotkey(getDevice(), getKeyCode(), getModifiers().toArray(new Modifier[0]));
	}
}
