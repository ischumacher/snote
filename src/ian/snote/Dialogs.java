package ian.snote;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class Dialogs {
	public static String getCreatePasswordDialog(Component parent) {
		final JPanel panel = new JPanel();
		final JPasswordField pass = new JPasswordField(25);
		final JPasswordField confirm = new JPasswordField(25);
		panel.setLayout(new GridLayout(4, 1));
		final JLabel lab1 = new JLabel("Password:");
		pass.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					confirm.requestFocus();
				}
			}
		});
		final JLabel lab2 = new JLabel("Confirm:");
		panel.add(lab1);
		panel.add(pass);
		panel.add(lab2);
		panel.add(confirm);
		final String[] options = new String[] { "OK" };
		JOptionPane jop = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.NO_OPTION, null, options,
				options[0]);

		JDialog dialog = jop.createDialog(parent, "Create Password");
		dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				pass.requestFocusInWindow();
			}
		});
		dialog.setVisible(true);
		jop.getValue();
		dialog.dispose();
		final char[] confirmStr = confirm.getPassword();
		final char[] passStr = pass.getPassword();
		if (passStr.length == 0) {
			JOptionPane.showMessageDialog(parent, "Password have to be more than zero characters long", "No Password",
					JOptionPane.ERROR_MESSAGE);
			getCreatePasswordDialog(parent);
		}
		if (!Arrays.equals(passStr, confirmStr)) {
			JOptionPane.showMessageDialog(parent, "Password fields did not match", "Password mismatch",
					JOptionPane.ERROR_MESSAGE);
			getCreatePasswordDialog(parent);
		}
		return new String(passStr);
	}
	public static String getReadPasswordDialog(Component parent) {
		JPanel panel = new JPanel(new GridLayout(2, 1));
		JPasswordField jpf = new JPasswordField(25);
		final JLabel lab1 = new JLabel("Password:");
		panel.add(lab1);
		panel.add(jpf);
		final String[] options = new String[] { "OK" };
		JOptionPane jop = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.NO_OPTION, null, options,
				options[0]);
		JDialog dialog = jop.createDialog(parent, "Enter Password");
		dialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				jpf.requestFocusInWindow();
			}
		});
		dialog.setVisible(true);
		jop.getValue();
		dialog.dispose();
		char[] password = null;
		password = jpf.getPassword();
		if (password.length == 0) {
			JOptionPane.showMessageDialog(parent, "Password have to be more than zero characters long", "No Password",
					JOptionPane.ERROR_MESSAGE);
			getReadPasswordDialog(parent);
		}
		return new String(password);
	}
	public static void loadError(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, "Error loading file " + message, "File Load Error",
				JOptionPane.ERROR_MESSAGE);
	}
	public static void saveError(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, "Error saving file " + message, "File Save Error",
				JOptionPane.ERROR_MESSAGE);
	}
}
