
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * カラー・コードシステムスタートメニュー
 * 
 * @see CreateTransmisstionImage2
 * @see VisibleLightReceiver
 * @author iwao
 * @version 1.0
 */
public class StartScreen extends JFrame implements ActionListener {

	private JToggleButton button1;
	private JToggleButton button2;
	private JToggleButton button5;
	private JToggleButton button6;
	private JButton button3;
	private JButton button4;
	private VisibleLightReceiver visibleLightReceiver = new VisibleLightReceiver();
	private CreateTransmisstionImage createTransmisstionImage = new CreateTransmisstionImage();
	private VisibleLightReceiver2 visibleLightReceiver2 = new VisibleLightReceiver2();
	private CreateTransmisstionImage2 createTransmisstionImage2 = new CreateTransmisstionImage2();
	private MatchTest matchTest = new MatchTest();

	StartScreen() {

		JFrame mainFrame = new JFrame("スタートメニュー");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(420, 200);
		mainFrame.setLocationRelativeTo(null);
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new GridLayout(2, 3));
		// buttonPane.setLayout(null);

		button1 = new JToggleButton("マーカ生成ver1");
		button1.setPreferredSize(new Dimension(100, 100));
		// button1.setBounds(10, 10, 80, 30)
		button1.addActionListener(this);

		button2 = new JToggleButton("マーカ受信ver1");
		button2.setPreferredSize(new Dimension(100, 100));
		// button2.setBounds(30, 30, 80, 30);
		button2.addActionListener(this);

		button5 = new JToggleButton("マーカ生成ver2");
		button5.setPreferredSize(new Dimension(100, 100));
		// button1.setBounds(10, 10, 80, 30)
		button5.addActionListener(this);

		button6 = new JToggleButton("マーカ受信ver2");
		button6.setPreferredSize(new Dimension(100, 100));
		// button2.setBounds(30, 30, 80, 30);
		button6.addActionListener(this);

		button3 = new JButton("データ比較");
		button3.setPreferredSize(new Dimension(100, 100));
		// button3.setBounds(50, 50, 80, 30);
		button3.addActionListener(this);

		button4 = new JButton("カメラプロパティ");
		button4.setPreferredSize(new Dimension(100, 100));
		// button3.setBounds(50, 50, 80, 30);
		button4.addActionListener(this);
		buttonPane.add(button1);
		buttonPane.add(button2);
		buttonPane.add(button3);
		buttonPane.add(button5);
		buttonPane.add(button6);
		buttonPane.add(button4);

		mainFrame.add(buttonPane, BorderLayout.CENTER);
		mainFrame.setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {
		// ユーザの操作対象を判断
		if (event.getSource() == button1) {
			if (button1.isSelected() == true) {
				button1.setText("<html>マーカ生成ver1<br>【実行中】<html>");
				createTransmisstionImage.startRunning();
				System.out.println("マーカ生成ver1【ON】");
			} else {
				button1.setText("マーカ生成ver1");
				createTransmisstionImage.stopRunning();
				System.out.println("マーカ生成ver1【OFF】");
			}

		}
		if (event.getSource() == button2) {
			if (button2.isSelected() == true) {
				button2.setText("<html>マーカ受信ver1<br>【実行中】<html>");
				visibleLightReceiver.startRunning();
				System.out.println("マーカ受信ver1【ON】");
			} else {
				button2.setText("マーカ受信ver1");
				visibleLightReceiver.stopRunning();
				System.out.println("マーカ受信ver1【OFF】");
			}

		}
		if (event.getSource() == button3) {
			if (button1.isSelected() == true && button2.isSelected() == true) {
				System.out.print("送受信チェックver1 : ");
				matchTest.startTest(visibleLightReceiver.getReceiveList(),
						createTransmisstionImage.getTransmissionList());
			}
			if (button5.isSelected() == true && button6.isSelected() == true) {
				System.out.print("送受信チェックver2 : ");
				matchTest.startTest(visibleLightReceiver2.getReceiveList(),
						createTransmisstionImage2.getTransmissionList());
			}
		}
		if (event.getSource() == button4) {
			System.out.println("カメラプロパティ開きます");
			String filename = " C:/Windows/SysWOW64/LogiDPPApp.exe";
			try {
				Runtime.getRuntime().exec(filename);
			} catch (Exception e) {
				System.out.println("C:/Windows/SysWOW64/LogiDPPApp.exeが正常に開けません。");
			}
		}
		if (event.getSource() == button5) {
			if (button5.isSelected() == true) {
				button5.setText("<html>マーカ生成ver2<br>【実行中】<html>");
				createTransmisstionImage2.startRunning();
				System.out.println("マーカ生成ver2【ON】");
			} else {
				button5.setText("マーカ生成ver2");
				createTransmisstionImage2.stopRunning();
				System.out.println("マーカ生成ver2【OFF】");
			}
		}
		if (event.getSource() == button6) {
			if (button6.isSelected() == true) {
				button6.setText("<html>マーカ受信ver2<br>【実行中】<html>");
				visibleLightReceiver2.startRunning();
				System.out.println("マーカ受信2【ON】");
			} else {
				button6.setText("マーカ受信ver");
				visibleLightReceiver2.stopRunning();
				System.out.println("マーカ受信ver2【OFF】");
			}
		}
	}

	public static void main(String[] args) {
		new StartScreen();
	}
}