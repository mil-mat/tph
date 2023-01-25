package mil.tsh;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import mil.tsh.gui.TSHFrame;
import mil.tsh.types.Token;
import mil.tsh.util.DeviceUtil;
import mil.tsh.util.HotkeyUtil;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.io.IOException;

public class Application {

	private static DeviceUtil _deviceUtil = null;
	private static HotkeyUtil _hotkeyUtil = null;
	private static boolean _isLoggedIn = false;
	private static TSHFrame _frame;

	public static DeviceUtil getDeviceUtil() {
		return _deviceUtil;
	}

	public static HotkeyUtil getHotkeyUtil() {
		return _hotkeyUtil;
	}

	public static boolean isLoggedIn() {
		return _isLoggedIn;
	}

	public static void setLoggedIn(boolean newVal) {
		_isLoggedIn = newVal;

		initDeviceUtil();
	}

	public static TSHFrame getFrame() {
		return _frame;
	}

	public static void main(String[] args) throws NativeHookException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		GlobalScreen.registerNativeHook(); // Enable native key events

		_isLoggedIn = Token.fileExists();
		initDeviceUtil();

		_hotkeyUtil = new HotkeyUtil();
		_hotkeyUtil.updateFromDisk();

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		UIManager.put("ToolTip.background", new ColorUIResource(255, 255, 255));

		_frame = new TSHFrame();
	}

	/**
	 * If is logged in - Initializes _deviceUtil, then updates known devices from disk.
	 */
	private static void initDeviceUtil() {
		if (isLoggedIn()) {
			_deviceUtil = new DeviceUtil(Token.readFile());
			_deviceUtil.updateFromDisk();
		}
	}

	/** Deletes the current token file and all references, plus hotkeys, then refreshes GUI. */
	public static void logout() {
		getDeviceUtil().getToken().deleteFile();
		getDeviceUtil().setToken(null);
		getHotkeyUtil().clearHotkeys();

		setLoggedIn(false);
		getFrame().refresh();
	}

}
