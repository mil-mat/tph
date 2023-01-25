package mil.tph.util;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import mil.tph.Application;
import mil.tph.types.Hotkey;
import mil.tph.types.Modifier;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HotkeyUtil implements NativeKeyListener {

	private static final String SAVE_PATH = System.getProperty("user.dir") + "/data/hotkeys";

	private Hotkey _currentlyEditedHotkey = null;
	private Hotkey _cloneCurrentlyEditedHotkey = null; // Used to set key combination back if cancelled

	private List<Hotkey> _hotkeys = new ArrayList<>();

	public HotkeyUtil() {
		GlobalScreen.addNativeKeyListener(this);
	}

	public Collection<Hotkey> getHotkeys() {
		return Collections.unmodifiableCollection(_hotkeys);
	}

	public void addHotkey(Hotkey h) {
		_hotkeys.add(h);
		Application.getFrame().refresh();
		writeFile();
	}

	public void removeHotkey(Hotkey hotkey) {
		_hotkeys.remove(hotkey);
		Application.getFrame().refresh();
		writeFile();
	}

	public void clearHotkeys() {
		_hotkeys.clear();
		Application.getFrame().refresh();
		writeFile();
	}

	public boolean hasHotkey(int keyCode, Modifier... modifiers) {
		return !getHotkey(keyCode, modifiers).isEmpty();
	}

	/**
	 * @return A list of hotkeys with the specified keyCode and modifier combination.
	 */
	private List<Hotkey> getHotkey(int keyCode, Modifier... modifiers) { // more performant than hasHotkey() when you have to get the hotkey if hasHotkey() is true
		return _hotkeys.stream().filter(h -> (h.getKeyCode() == keyCode && h.getModifiers().equals(new HashSet<>(Arrays.asList(modifiers))))).collect(Collectors.toList());
	}

	@Override
	public void nativeKeyPressed(NativeKeyEvent e) {
		if (!Application.isLoggedIn()) return;

		if (_currentlyEditedHotkey == null) { // If user is not editing a hotkey, check if it can be performed
			if (Modifier.isModifier(e.getKeyCode())) return;
			getHotkey(e.getKeyCode(), getModifiers(e.getModifiers()).toArray(Modifier[]::new)).forEach(Hotkey::perform);
			return;
		}

		if (Modifier.isModifier(e.getKeyCode())) { // Custom function instead of e.isActionKey() since some "action keys" aren't modifiers (e.g. F1-24)
			Set<Modifier> pressedModifiers = getModifiers(e.getModifiers());
			if (_currentlyEditedHotkey.getModifiers().equals(pressedModifiers)) {
				return; // check if duplicate as to not call frame.refresh(); unnecessarily (through hotkey#setModifiers)
			}

			Set<Modifier> modifiers = _currentlyEditedHotkey.getModifiers();
			modifiers.addAll(getModifiers(e.getModifiers()));
			_currentlyEditedHotkey.setModifiers(modifiers);
			return;
		}

		// Key is not a modifier, so save the hotkey:
		_currentlyEditedHotkey.setKeyCode(e.getKeyCode());
		_currentlyEditedHotkey = null;
		_cloneCurrentlyEditedHotkey = null;

		writeFile();
	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent e) {
		if (_currentlyEditedHotkey == null) return;

		if (Modifier.isModifier(e.getKeyCode())) {
			Set<Modifier> modifiers = _currentlyEditedHotkey.getModifiers();
			modifiers.remove( // Have to do this manually instead of through getModifiers(int) since e.getModifiers() doesn't seem to function.
					switch (e.getKeyCode()) {
						case 29 -> Modifier.CTRL;
						case 42 -> Modifier.SHIFT;
						case 56 -> Modifier.ALT;
						case 3675 -> Modifier.META;
						default -> null;
					});

			if (modifiers.isEmpty()) { // Cancel the editing of this hotkey, as no keys are pressed. (since a non-modifier key saves the new key-combination)
				_currentlyEditedHotkey.setKeyCombination(_cloneCurrentlyEditedHotkey.getKeyCode(), _cloneCurrentlyEditedHotkey.getModifiers());
				_currentlyEditedHotkey = null;
				_cloneCurrentlyEditedHotkey = null;
				return;
			}

			_currentlyEditedHotkey.setModifiers(modifiers);
		}
	}

	/** Utility method to allow for quick converting from NativeKeyEvent modifiers to a list of our custom type */
	private Set<Modifier> getModifiers(int modifiers) {
		Set<Modifier> ret = new HashSet<>();

		if ((modifiers & NativeInputEvent.CTRL_MASK) != 0) ret.add(Modifier.CTRL);
		if ((modifiers & NativeInputEvent.SHIFT_MASK) != 0) ret.add(Modifier.SHIFT);
		if ((modifiers & NativeInputEvent.ALT_MASK) != 0) ret.add(Modifier.ALT);
		if ((modifiers & NativeInputEvent.META_MASK) != 0) ret.add(Modifier.META);

		return ret;
	}

	public Hotkey getCurrentlyEditedHotkey() {
		return _currentlyEditedHotkey;
	}

	public void setCurrentlyEditedHotkey(Hotkey hotkey) {
		_currentlyEditedHotkey = hotkey;
		_cloneCurrentlyEditedHotkey = hotkey.clone();
	}

	public void writeFile() {
		try {
			ObjectOutputStream oStream = new ObjectOutputStream(new FileOutputStream(SAVE_PATH));
			oStream.writeObject(_hotkeys);
			oStream.close();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.WARNING, "Writing hotkeys file failed!");
			e.printStackTrace();
		}
	}

	public void updateFromDisk() throws IOException, ClassNotFoundException {
		if (!new File(SAVE_PATH).exists()) {
			_hotkeys = new ArrayList<>();
			return;
		}
		ObjectInputStream iStream = new ObjectInputStream(new FileInputStream(SAVE_PATH));
		_hotkeys = (List<Hotkey>) iStream.readObject();
	}

}
