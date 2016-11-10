import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * マーカver2用画像生成器<br>
 * startRunning()とstopRunning()を外部クラスから制御して利用。<br>
 * カラー・コードを生成するとListに情報を保持した状態で中断される。<br>
 * getDecodeList()でListを外部クラスに渡し、マッチテストは外部クラスで行う。<br>
 * VisibleLightReceiver2と対応しており、VisibleLightReceiver1と互換性を持たない。
 * フィールド変数であるdivisionの値をVisibleLightReceiver2のdivisionと揃えること。<br>
 *
 * @see VisibleLightReceiver2
 * @author iwao
 * @version 1.0
 */
public class CreateTransmisstionImage2 extends Thread {
	private ImageDrawing imageDrawing = new ImageDrawing();
	private JFrame transmisstionImageFrame;
	private ImageDrawing transmisstionImagePanel;
	private List<String> transmissionList;
	private int division = 8;

	public CreateTransmisstionImage2() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// Opencvの利用のため
		transmisstionImageFrame = new JFrame("送信画像ver2");// 加工画像用ウィンドウフレーム
		transmisstionImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		transmisstionImagePanel = new ImageDrawing();// 各種画像を載せるパネル
		transmisstionImageFrame.setContentPane(transmisstionImagePanel);
		transmissionList = new ArrayList<String>();// エンコードを行う情報リスト
	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see CreateTransmisstionImage2#startRunning()
	 * @see CreateTransmisstionImage2#stopRunning()
	 */
	public void run() {
		createImageLoop();
	}

	/**
	 * 外部クラスから呼び出しクラスを実行する
	 *
	 * @see CreateTransmisstionImage2#run()
	 * @see CreateTransmisstionImage2#stopRunning()
	 */
	public void startRunning() {
		transmisstionImageFrame.setVisible(true);
		new Thread(this).start();
	}

	/**
	 * 外部クラスから呼び出しクラス中断する
	 *
	 * @see CreateTransmisstionImage2#run()
	 * @see CreateTransmisstionImage2#startRunning()
	 */
	public void stopRunning() {
		transmissionList.clear();
		transmisstionImageFrame.setVisible(false);
	}

	/**
	 * 送信内容をリストで返す
	 *
	 * @return 受信内容List
	 */
	public List<String> getTransmissionList() {
		return transmissionList;
	}

	private void colorEncode(Mat srcImage, double startX, double startY, double endX, double endY, int division) {
		int counttest = 1;
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		Scalar paintColorBGR = null;
		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
				} else {
					int x1 = (int) (startX + (j * oneThirdWidth));
					int y1 = (int) (startY + (i * oneThirdHeight));
					int x2 = (int) (startX + ((j + 1) * oneThirdWidth));
					int y2 = (int) (startY + ((i + 1) * oneThirdHeight));
					int randmNumber = (int) (Math.random() * 6) + 1;// 現在ではここで色の種類設定
					if (counttest > 12) {
						counttest = 1;
					}
					switch (randmNumber++) {
					case 1:
						transmissionList.add("0");
						paintColorBGR = new Scalar(0, 0, 255);
						break;
					case 2:
						transmissionList.add("120");
						paintColorBGR = new Scalar(0, 255, 0);
						break;
					case 3:
						transmissionList.add("240");
						paintColorBGR = new Scalar(255, 0, 0);
						break;
					case 4:
						transmissionList.add("60");
						paintColorBGR = new Scalar(0, 255, 255);
						break;
					case 5:
						transmissionList.add("180");
						paintColorBGR = new Scalar(255, 255, 0);
						break;
					case 6:
						transmissionList.add("300");
						paintColorBGR = new Scalar(255, 0, 255);
						break;
					case 7:
						transmissionList.add("30");
						paintColorBGR = new Scalar(0, 127, 255);
						break;
					case 8:
						transmissionList.add("90");
						paintColorBGR = new Scalar(0, 255, 127);
						break;
					case 9:
						transmissionList.add("150");
						paintColorBGR = new Scalar(127, 255, 0);
						break;
					case 10:
						transmissionList.add("210");
						paintColorBGR = new Scalar(255, 127, 0);
						break;
					case 11:
						transmissionList.add("270");
						paintColorBGR = new Scalar(255, 0, 127);
						break;
					case 12:
						transmissionList.add("330");
						paintColorBGR = new Scalar(127, 0, 255);
						break;
					case 13:// 枠外エラー
						System.out.println("colorEncodeエラー");
						return;
					}
					Imgproc.rectangle(srcImage, new Point(x1, y1), new Point(x2, y2), paintColorBGR, -1);
				}
			}
		}
	}

	/**
	 * カラー・コード生成処理をループ
	 */
	private void createImageLoop() {
		Mat markerImage = Imgcodecs.imread("mark2.jpg");
		transmisstionImageFrame.setSize(new Dimension(markerImage.rows() + 35, markerImage.cols() + 55));
		colorEncode(markerImage, 42, 42, markerImage.height() - 43, markerImage.width() - 43, division);
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(markerImage);
		transmisstionImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		transmisstionImagePanel.repaint();// パネルを再描画
	}

}
