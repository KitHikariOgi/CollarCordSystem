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
 * マーカver1用画像生成器<br>
 * startRunning()とstopRunning()を外部クラスから制御して利用。<br>
 * カラー・コードを生成するとListに情報を保持した状態で中断される。<br>
 * getDecodeList()でListを外部クラスに渡し、マッチテストは外部クラスで行う。<br>
 * VisibleLightReceiverと対応しており、VisibleLightReceiver2と互換性を持たない。
 * CreateTransmisstionImage2およびVisibleLightReceiver2を利用するマーカーver2と比較して
 * マーカver1が優れている点はないと思われる。現状マーカver2との性能比較のためのクラスである。
 *
 * @see VisibleLightReceiver_colerOfThree
 * @author iwao
 * @version 1.0
 */
public class CreateTransmisstionImage_colerOfThree extends Thread {
	public static final int DIVISION = 8;// ブロックの一辺の数
	public static final int COLLAR_VARIATION = 3;// 色の種類
	public static final int THICKNESS = -1;// ボーダーの太さ
	public static final int ROW_MARGIN = 35;// マーカーの横の余白
	public static final int COL_MARGIN = 55;// マーカーの縦の余白
	// public static final int DIVISION = 3;

	private ImageDrawing_colerOfThree imageDrawing_colerOfThree = new ImageDrawing_colerOfThree();
	private JFrame transmisstionImageFrame;
	private ImageDrawing_colerOfThree transmisstionImagePanel;
	private List<String> transmissionList;
	private int division = 3;

	public CreateTransmisstionImage_colerOfThree() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// Opencvの利用のため
		transmisstionImageFrame = new JFrame("送信画像ver1");// 加工画像用ウィンドウフレーム
		transmisstionImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		transmisstionImagePanel = new ImageDrawing_colerOfThree();// 各種画像を載せるパネル
		transmisstionImageFrame.setContentPane(transmisstionImagePanel);
		transmissionList = new ArrayList<String>();// エンコードを行う情報リスト
	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see CreateTransmisstionImage_colerOfThree#startRunning()
	 * @see CreateTransmisstionImage_colerOfThree#stopRunning()
	 */
	public void run() {
		createImageLoop();
	}

	/**
	 * 外部クラスから呼び出しクラスを実行する
	 *
	 * @see CreateTransmisstionImage_colerOfThree#run()
	 * @see CreateTransmisstionImage_colerOfThree#stopRunning()
	 */
	public void startRunning() {
		transmisstionImageFrame.setVisible(true);
		new Thread(this).start();
	}

	/**
	 * 外部クラスから呼び出しクラス中断する
	 *
	 * @see CreateTransmisstionImage_colerOfThree#run()
	 * @see CreateTransmisstionImage_colerOfThree#startRunning()
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

	/**
	 * 入力されたベース画像にカラー・コードを書き込みコード内容をListに保存する。
	 *
	 * @param srcImage
	 *            入力画像
	 * @param startX
	 *            処理範囲の左上ｘ座標
	 * @param startY
	 *            処理範囲の左上ｙ座標
	 * @param endX
	 *            処理範囲の右下ｘ座標
	 * @param endY
	 *            処理範囲の右下ｘ座標
	 * @param diveCount
	 *            再帰処理を行う回数
	 */
	private void colorEncode(Mat srcImage, double startX, double startY, double endX, double endY, int diveCount) {
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		Scalar paintColorBGR = null;
		if (diveCount == 0) {
			for (int i = 0; i < division; i++) {
				for (int j = 0; j < division; j++) {
					int x1 = (int) (startX + (j * oneThirdWidth));
					int y1 = (int) (startY + (i * oneThirdHeight));
					int x2 = (int) (startX + ((j + 1) * oneThirdWidth));
					int y2 = (int) (startY + ((i + 1) * oneThirdHeight));
					int randmNumber = (int) (Math.random() * 3) + 1;
					switch (randmNumber) {
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
					Imgproc.rectangle(srcImage, new Point(x1, y1), new Point(x2, y2), paintColorBGR, THICKNESS);
				}
			}
		} else {
			for (int i = 0; i < division; i++) {
				for (int j = 0; j < division; j++) {
					if (diveCount == 1) {
						if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0) {
						} else {
							colorEncode(srcImage, startX + j * oneThirdWidth, startY + i * oneThirdHeight,
									startX + (j + 1) * oneThirdWidth, startY + (i + 1) * oneThirdHeight, diveCount - 1);
						}
					} else {
						colorEncode(srcImage, startX + j * oneThirdWidth, startY + i * oneThirdHeight,
								startX + (j + 1) * oneThirdWidth, startY + (i + 1) * oneThirdHeight, diveCount - 1);
					}
				}
			}
		}
	}

	/**
	 * カラー・コード生成処理をループ
	 */
	private void createImageLoop() {
		Mat markerImage = Imgcodecs.imread("mark.jpg");
		transmisstionImageFrame.setSize(new Dimension(markerImage.rows() + ROW_MARGIN, markerImage.cols() + COL_MARGIN));
		colorEncode(markerImage, 0, 0, markerImage.height(), markerImage.width(), 1);
		BufferedImage bufferedImageTemp = imageDrawing_colerOfThree.matToBufferedImage(markerImage);
		transmisstionImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		transmisstionImagePanel.repaint();// パネルを再描画
	}
}
