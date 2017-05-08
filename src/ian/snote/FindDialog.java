package ian.snote;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FindDialog extends JDialog implements ActionListener, KeyListener {
	private static final long serialVersionUID = -8426512674206639572L;
	SecureEditor parent;
	JLabel label;
	JTextField textField;
	JButton find, close;
	boolean finishedFinding = true;
	Matcher matcher;
	public FindDialog(SecureEditor parent, boolean modal) {
		super(parent, modal);
		this.parent = parent;
		getContentPane().addKeyListener(this);
		getContentPane().setFocusable(true);
		initComponents();
		setTitle("Find");
		setLocationRelativeTo(parent);
		pack();
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				textField.requestFocusInWindow();
			}
		});
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		final String cmd = e.getActionCommand();
		if (cmd.equals("Find")) {
			find(textField.getText());
		} else if (cmd.equals("Close")) {
			closeDialog();
		}
	}
	private void closeDialog() {
		setVisible(false);
		dispose();
	}
	private void find(String pattern) {
		int offset = parent.textPane.getCaretPosition();
		String text = parent.getText().substring(offset);
		matcher = Pattern.compile(pattern).matcher(text);
		if (matcher.find()) {
			parent.textPane.select(offset + matcher.start(), offset + matcher.end());
		} else {
			parent.textPane.moveCaretPosition(0);
			parent.textPane.select(0, 0);
		}
	}
	private void initComponents() {
		setLayout(new GridLayout(2, 1));
		final JPanel panel1 = new JPanel();
		label = new JLabel("Find:");
		label.setDisplayedMnemonic('F');
		panel1.add(label);
		textField = new JTextField(15);
		panel1.add(textField);
		label.setLabelFor(textField);
		add(panel1);
		final JPanel panel2 = new JPanel();
		find = new JButton("Find");
		close = new JButton("Close");
		find.addActionListener(this);
		close.addActionListener(this);
		panel2.add(find);
		panel2.add(close);
		add(panel2);
		textField.addKeyListener(this);
		find.addKeyListener(this);
		close.addKeyListener(this);
	}
	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
			closeDialog();
			break;
		case KeyEvent.VK_ENTER:
			find.doClick();
			break;
		}
	}
	@Override
	public void keyReleased(KeyEvent e) {
	}
	@Override
	public void keyTyped(KeyEvent e) {
	}
	public void showDialog() {
		setVisible(true);
	}
}