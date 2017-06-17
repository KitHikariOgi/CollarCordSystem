
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * カラー・コードシステムスタートメニュー
 *
 * @see CreateTransmisstionImage2_colerOfThree
 * @see VisibleLightReceiver_colerOfThree
 * @author iwao
 * @version 1.0
 */
public class StartScreen_colerOfThree extends JFrame implements ActionListener {

	public static final int START_FRAMESIZE_X= 420;// startのｘのframesize
	public static final int START_FRAMESIZE_Y= 200;// startのｙのframesize

	private JFrame mainFrame;

	private JToggleButton button1;
	private JToggleButton button2;
	private JToggleButton button5;
	private JToggleButton button6;
	private JButton button3;
	private JButton button4;

	private CreateTransmisstionImage_colerOfThree createTransmisstionImage_colerOfThree = new CreateTransmisstionImage_colerOfThree();
	private VisibleLightReceiver_colerOfThree visibleLightReceiver_colerOfThree = new VisibleLightReceiver_colerOfThree();

	private CreateTransmisstionImage2_colerOfThree createTransmisstionImage2_colerOfThree = new CreateTransmisstionImage2_colerOfThree();
	private VisibleLightReceiver2_colerOfThree visibleLightReceiver2_colerOfThree = new VisibleLightReceiver2_colerOfThree(createTransmisstionImage2_colerOfThree);

	private MatchTest_colerOfThree matchTest_colerOfThree = new MatchTest_colerOfThree();

	StartScreen_colerOfThree() {

		mainFrame = new JFrame("スタートメニュー");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(START_FRAMESIZE_X, START_FRAMESIZE_Y);
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
		visibleLightReceiver2_colerOfThree.setDivision(createTransmisstionImage2_colerOfThree.getDivision());
		mainFrame.addWindowListener(new myListener());

	}

	public void actionPerformed(ActionEvent event) {
		// ユーザの操作対象を判断
		if (event.getSource() == button1) {
			if (button1.isSelected() == true) {
				button1.setText("<html>マーカ生成ver1<br>【実行中】<html>");
				createTransmisstionImage_colerOfThree.startRunning();
				System.out.println("マーカ生成ver1【ON】");
			} else {
				button1.setText("マーカ生成ver1");
				createTransmisstionImage_colerOfThree.stopRunning();
				System.out.println("マーカ生成ver1【OFF】");
			}

		}
		if (event.getSource() == button2) {
			if (button2.isSelected() == true) {
				button2.setText("<html>マーカ受信ver1<br>【実行中】<html>");
				visibleLightReceiver_colerOfThree.startRunning();
				System.out.println("マーカ受信ver1【ON】");
			} else {
				button2.setText("マーカ受信ver1");
				visibleLightReceiver_colerOfThree.stopRunning();
				System.out.println("マーカ受信ver1【OFF】");
			}

		}
		if (event.getSource() == button3) {
			if (button1.isSelected() == true && button2.isSelected() == true) {
				System.out.print("送受信チェックver1 : ");
				matchTest_colerOfThree.startTest(visibleLightReceiver_colerOfThree.getReceiveList(),
						createTransmisstionImage_colerOfThree.getTransmissionList());
			}
			if (button5.isSelected() == true && button6.isSelected() == true) {
				System.out.print("送受信チェックver2 : \n");
				matchTest_colerOfThree.startTest(visibleLightReceiver2_colerOfThree.getReceiveList(),
						createTransmisstionImage2_colerOfThree.getTransmissionList());
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
				createTransmisstionImage2_colerOfThree.startRunning();
				System.out.println("マーカ生成ver2【ON】");
			} else {
				button5.setText("マーカ生成ver2");
				createTransmisstionImage2_colerOfThree.stopRunning();
				System.out.println("マーカ生成ver2【OFF】");
			}
		}
		if (event.getSource() == button6) {
			if (button6.isSelected() == true) {
				button6.setText("<html>マーカ受信ver2<br>【実行中】<html>");
				visibleLightReceiver2_colerOfThree.startRunning();
				System.out.println("マーカ受信2【ON】");
			} else {
				button6.setText("マーカ受信ver");
				visibleLightReceiver2_colerOfThree.stopRunning();
				System.out.println("マーカ受信ver2【OFF】");
			}
		}
		
	}

	public static void main(String[] args) {
		new StartScreen_colerOfThree();
	}

	  public class myListener extends WindowAdapter{
	    public void windowClosing(WindowEvent e) {
	      System.out.println("システムを終了します....");
	    }
	  }
}