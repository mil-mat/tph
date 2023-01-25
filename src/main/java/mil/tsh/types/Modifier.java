package mil.tsh.types;

public enum Modifier {
	CTRL("Ctrl"),
	SHIFT("Shift"),
	ALT("Alt"),
	META("Meta");

	private final String _name;

	Modifier(String name) {
		this._name = name;
	}

	public String getName() {
		return _name;
	}
}
