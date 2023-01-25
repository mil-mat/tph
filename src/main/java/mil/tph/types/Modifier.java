package mil.tph.types;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public enum Modifier {
	CTRL("Ctrl", (short) 1),
	SHIFT("Shift", (short) 2),
	ALT("Alt", (short) 3),
	META("Meta", (short) 4);

	private final String _name;
	private final short _order;

	Modifier(String name, short order) {
		this._name = name;
		this._order = order;
	}

	public static boolean isModifier(int keyCode) {
		return keyCode == NativeKeyEvent.VC_CONTROL
				|| keyCode == NativeKeyEvent.VC_SHIFT
				|| keyCode == NativeKeyEvent.VC_ALT
				|| keyCode == NativeKeyEvent.VC_META;
	}

	public String getName() {
		return _name;
	}

	/** @return The order that this modifier should be displayed */
	public short getOrder() {
		return _order;
	}
}
