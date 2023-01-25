package mil.tph.gui;

import mil.tph.Application;
import mil.tph.types.Hotkey;
import mil.tph.types.Plug;
import mil.tph.types.Token;
import mil.tph.util.APIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;

public class TPHFrame extends JFrame {

	private int _scrollbarProgress = 0; // save so that it doesn't reset on refresh();

	public TPHFrame() {
		setEnabled(true);

		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));

		if (SystemTray.isSupported()) {
			setDefaultCloseOperation(HIDE_ON_CLOSE);

			TrayIcon icon = new TrayIcon(getIconImage(), "TPH");
			PopupMenu menuPopup = new PopupMenu();

			MenuItem showI = new MenuItem("show");
			showI.addActionListener(e -> setVisible(true));
			menuPopup.add(showI);

			MenuItem hideI = new MenuItem("exit");
			hideI.addActionListener(e -> System.exit(0));
			menuPopup.add(hideI);

			icon.setImageAutoSize(true);
			icon.setPopupMenu(menuPopup);

			try {
				SystemTray.getSystemTray().add(icon);
			} catch (AWTException e) {
				e.printStackTrace();
			}

		}

		setTitle("Tuya Plug Hotkeys");
		setSize(600, 700);

		refresh();

		setResizable(false);
		setVisible(!Application.getPreferences().doesStartInTray());

		this.setLocationRelativeTo(null);
	}

	// TODO Performance could be improved by using a different model that doesn't require refreshing the whole frame for every change.
	// TODO ^ Perhaps split up into a couple different refresh methods, each for a different component/list of components.
	/**
	 * Refreshes the GUI. This brings all elements, such as the state of a device, up to date visually.
	 */
	public void refresh() { // UI programming is pain
		System.out.println("Refreshing Frame...");

		this.getContentPane().removeAll();
		setLayout(new BorderLayout(0, 10));

		if (!Application.isLoggedIn()) { // Prompt with log in form
			JPanel panelCentre = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = 1;

			gbc.gridx = 1;
			gbc.gridy++;
			JLabel labelError = new JLabel("Error - Credentials Rejected!");
			labelError.setFont(labelError.getFont().deriveFont(labelError.getFont().getStyle() | Font.BOLD)); // bold
			labelError.setForeground(Color.RED);
			panelCentre.add(labelError, gbc);
			labelError.setVisible(false);

			gbc.gridx = 0;
			gbc.gridy++;
			JLabel labelUsername = new JLabel("username  ");
			panelCentre.add(labelUsername, gbc);
			gbc.gridx = 1;
			JTextField tfUsername = new JTextField();
			tfUsername.setPreferredSize(new Dimension(200, 20));
			panelCentre.add(tfUsername, gbc);

			gbc.gridx = 0;
			gbc.gridy++;
			JLabel labelPassword = new JLabel("password  ");
			panelCentre.add(labelPassword, gbc);
			gbc.gridx = 1;
			JPasswordField tfPassword = new JPasswordField();
			panelCentre.add(tfPassword, gbc);

			gbc.gridx = 0;
			gbc.gridy++;
			JLabel labelCountryCode = new JLabel("country code  ");
			panelCentre.add(labelCountryCode, gbc);
			gbc.gridx = 1;

			NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(0);
			formatter.setMaximum(9999);
			formatter.setAllowsInvalid(false);
			JFormattedTextField tfCountryCode = new JFormattedTextField(formatter);
			panelCentre.add(tfCountryCode, gbc);

			gbc.gridy++;
			gbc.gridx = 1;
			JButton bLogIn = new JButton("Log In");
			bLogIn.addActionListener((e) -> {
				bLogIn.setEnabled(false);

				if (tfUsername.getText() != null && tfPassword.getPassword() != null && tfCountryCode.getValue() != null &&
						attemptLogin(tfUsername.getText(), new String(tfPassword.getPassword()), (int) tfCountryCode.getValue())) {
					refresh();
				} else {
					labelError.setVisible(true);
				}

				bLogIn.setEnabled(true);
			});
			panelCentre.add(bLogIn, gbc);

			add(panelCentre, BorderLayout.CENTER);

			repaint();
			revalidate();
			return;
		}

		// Hotkeys UI:

		{// BorderLayout CENTRE
			JPanel panelMain = new JPanel(new GridBagLayout());
			add(panelMain);

			GridBagConstraints gbc = new GridBagConstraints();

			JPanel panelChild = new JPanel(new BorderLayout()); // Child of panelMain, contains the scroll pane

			gbc.weightx = gbc.weighty = gbc.fill = 1; // Allows scroll pane to fill entire page (except for footer)

			{// Hotkeys List
				JPanel panelContainer = new JPanel(new BorderLayout()); // Child of the scroll pane, contains the components panel
				panelContainer.setBorder(new EmptyBorder(10, 0, 0, 0)); // Top Padding

				JPanel panelComponents = new JPanel(new GridBagLayout()); // Child of panelContainer, contains the components
				GridBagConstraints c = new GridBagConstraints();
				c.fill = 1;
				c.insets = new Insets(0, 0, 10, 10); // Padding between components
				c.anchor = GridBagConstraints.NORTH; // Start at top of page

				Collection<Hotkey> hotkeys = Application.getHotkeyUtil().getHotkeys();
				for (Hotkey hotkey : hotkeys) {
					c.gridy++;

					c.ipadx = 10;
					c.gridx = 0;
					JButton bRemove = new JButton("-");
					bRemove.addActionListener(e -> Application.getHotkeyUtil().removeHotkey(hotkey));
					panelComponents.add(bRemove, c);

					c.ipadx = 60;
					c.gridx = 1;
					JButton bCombo = new JButton(hotkey.getReadableKeyCombination());
					panelComponents.add(bCombo, c);
					bCombo.addActionListener(e -> {
						Application.getHotkeyUtil().setCurrentlyEditedHotkey(hotkey);
						hotkey.setKeyCombination(Hotkey.KEYCODE_AWAITING, new HashSet<>());
					});

					c.gridx = 2;
					JComboBox<String> comboDeviceSelect = new JComboBox<>(Application.getDeviceUtil().getKnownDevices().stream().map(Plug::getName).toArray(String[]::new)); // TODO performance
					comboDeviceSelect.setSelectedItem(hotkey.getDevice().getName());
					comboDeviceSelect.addItemListener((e) -> {
						if (e.getStateChange() == ItemEvent.SELECTED) {// Avoid firing twice (ItemEvent.DESELECTED also gets called)
							Application.getDeviceUtil().fromName((String) e.getItem()).ifPresent(hotkey::setDevice);
							Application.getHotkeyUtil().writeFile();
						}
					});
					panelComponents.add(comboDeviceSelect, c);

					c.gridx = 3;
					JButton bToggle = new JButton((hotkey.getDevice().getState() ? "On" : "Off"));
					bToggle.addActionListener((e) -> hotkey.perform());
					panelComponents.add(bToggle, c);
				}

				c.gridy++;

				JButton bAddNew;
				if (hotkeys.isEmpty()) {
					c.gridx = 1;
					bAddNew = new JButton("Add New");
				} else {
					c.gridx = 0;
					c.ipadx = 10;
					bAddNew = new JButton("+");
				}
				bAddNew.addActionListener((e) -> Application.getHotkeyUtil().addHotkey(new Hotkey(Application.getDeviceUtil().getKnownDevices().get(0), Hotkey.KEYCODE_UNBOUND)));

				panelComponents.add(bAddNew, c);

				panelContainer.add(panelComponents, BorderLayout.NORTH); // Make components start at top

				JScrollPane pane = new JScrollPane(panelContainer);
				pane.getHorizontalScrollBar().setEnabled(false);
				pane.getVerticalScrollBar().setValue(_scrollbarProgress);
				pane.getVerticalScrollBar().setUnitIncrement(20);
				pane.getVerticalScrollBar().addAdjustmentListener(e -> _scrollbarProgress = e.getValue());

				panelChild.add(pane);

			}

			panelMain.add(panelChild, gbc);
		}

		{// BorderLayout - SOUTH
			JPanel panelMain = new JPanel(new BorderLayout());
			panelMain.setBorder(new EmptyBorder(0, 10, 5, 10));

			panelMain.add(new JLabel("2023 Milan Mat"), BorderLayout.WEST);

			JPanel panelEast = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.ipadx = 60;
			c.insets = new Insets(0, 10, 0, 0);

			JCheckBox boxStartInTray = new JCheckBox("Start in Tray");
			boxStartInTray.setSelected(Application.getPreferences().doesStartInTray());
			boxStartInTray.addActionListener(e -> Application.getPreferences().setStartInTray(!Application.getPreferences().doesStartInTray()));
			panelEast.add(boxStartInTray, c);

			JButton bClearHotkeys = new JButton("Clear");
			bClearHotkeys.addActionListener(e -> Application.getHotkeyUtil().clearHotkeys());
			panelEast.add(bClearHotkeys, c);

			c.gridy++;
			JButton bLogOut = new JButton("Logout");
			bLogOut.addActionListener(e -> Application.logout());
			panelEast.add(bLogOut, c);

			panelMain.add(panelEast, BorderLayout.EAST);

			add(panelMain, BorderLayout.SOUTH);
		}

		repaint();
		revalidate();
	}

	/**
	 * @return If a token was successfully generated.
	 */
	private boolean attemptLogin(String username, String password, int countryCode) {
		Token token = APIUtil.getToken(username, password, countryCode);
		if (token == null) return false;

		token.writeFile();
		Application.setLoggedIn(true);

		return true;
	}

}
