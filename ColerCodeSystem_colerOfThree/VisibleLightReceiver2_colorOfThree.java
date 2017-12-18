import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;//java.awt.Pointとorg.opencv.core.Pointには互換性はない注意！！
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * 岩男/マーカver2用受信機<br>
 * startRunning()とstopRunning()を外部クラスから制御して利用。<br>
 * カラー・コードを認識するとListに情報を保持した状態で中断される。<br>
 * getDecodeList()でListを外部クラスに渡し、マッチテストは外部クラスで行う。<br>
 * CreateTransmisstionImage2で生成されたマーカver2に対応しており、CreateTransmisstionImage1で生成される
 * <br>
 * マーカver1とは互換性を持たない。<br>
 * フィールド変数であるdivisionの値をVisibleLightReceiver2のdivisionと揃えること。<br>
 * *****************************************************************************
 *
 * @see CreateTransmisstionImage_colorOfThree
 * @see CreateTransmisstionImage2_colorOfThree
 * @see VisibleLightReceiver_colorOfThree
 * @author Ogi
 * @version 1.0
 */
public class VisibleLightReceiver2_colorOfThree extends Thread {
	private VideoCapture captureCamera;
	private JFrame processedImageFrame;
	private JFrame hsvImageFrame;
	private JFrame decordeImgFrame;

	private ImageDrawing_colorOfThree imageDrawing_colorOfThree;
	private ImageDrawing_colorOfThree processedImagePanel;
	private ImageDrawing_colorOfThree hsvImagePanel;
	private ImageDrawing_colorOfThree decordeImgPanel;

	private int division;
	private int TransformKey;
	private boolean runningKey;
	private boolean listCountCheck;

	private List<String> receiveList;
	private List<Byte> inImgBytes;// 取得メディアバイナリデータ
	private HashMap<String, Byte> colorPatternMap_V;

	private CreateTransmisstionImage2_colorOfThree createTransmisstionImage2_colorOfThree;

	public VisibleLightReceiver2_colorOfThree(
			CreateTransmisstionImage2_colorOfThree createTransmisstionImage2_colorOfThree) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// Opencvの利用のため
		captureCamera = new VideoCapture(0);// 使用webカメラの宣言
		this.createTransmisstionImage2_colorOfThree = createTransmisstionImage2_colorOfThree;
		// 加工画像用ウィンドウフレーム
		processedImageFrame = new JFrame("processedImage");
		processedImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		decordeImgFrame = new JFrame("byte配列からのimg変換");
		decordeImgFrame.setSize(500, 500);
		decordeImgFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		processedImagePanel = new ImageDrawing_colorOfThree();
		decordeImgPanel = new ImageDrawing_colorOfThree();
		processedImageFrame.setContentPane(processedImagePanel);
		decordeImgFrame.setContentPane(decordeImgPanel);
		// HSV画像用ウィンドウフレーム
		hsvImageFrame = new JFrame("hsvImageFrame");
		hsvImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		hsvImagePanel = new ImageDrawing_colorOfThree();
		hsvImageFrame.setContentPane(hsvImagePanel);
		imageDrawing_colorOfThree = new ImageDrawing_colorOfThree();
		runningKey = false;
		listCountCheck = false;
		// 最終的なデコード結果
		receiveList = new ArrayList<String>();
		// メディアバイナリデータ
		inImgBytes = new ArrayList<Byte>();
		// ブロックでのカラーパターンマップを記憶
		colorPatternMap_V = new HashMap<String, Byte>();
		Pattern(colorPatternMap_V);

	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see VisibleLightReceiver2_colorOfThree#startRunning()
	 * @see VisibleLightReceiver2_colorOfThree#stopRunning()
	 */
	public void run() {
		while (runningKey) {
			receiverLoop();
		}
	}

	/**
	 * 外部クラスから呼び出しクラスを実行する
	 *
	 * @see VisibleLightReceiver2_colorOfThree#run()
	 * @see VisibleLightReceiver2_colorOfThree#stopRunning()
	 */
	public void startRunning() {
		processedImageFrame.setVisible(true);
		hsvImageFrame.setVisible(true);
		setRunningKey(true);
		clearReceiveImgList();
		new Thread(this).start();
	}

	/**
	 * 外部クラスから呼び出しクラス中断する
	 *
	 * @see VisibleLightReceiver2_colorOfThree#run()
	 * @see VisibleLightReceiver2_colorOfThree#startRunning()
	 */
	public void stopRunning() {
		processedImageFrame.setVisible(false);
		hsvImageFrame.setVisible(false);
		decordeImgFrame.setVisible(false);
		setRunningKey(false);
	}

	/**
	 * 受信内容をリストで返す
	 *
	 * @return 受信内容List
	 */
	public List<String> getReceiveList() {
		return receiveList;
	}

	/**
	 * 受信リスト,メディアバイナリデータをクリア
	 */
	private void clearReceiveImgList() {
		receiveList.clear();
		inImgBytes.clear();
	}

	/**
	 * 一辺のブロック数セット
	 */
	public void setDivision(int division) {
		this.division = division;
	}

	/**
	 * 受信ループの制御値セット
	 */
	public void setRunningKey(boolean runningKey) {
		this.runningKey = runningKey;
	}

	/**
	 * カラーパターンの色の順序によって数値を振り分け設置
	 *
	 * @palam colorPatternMap2
	 */
	private void Pattern(HashMap<String, Byte> colorPatternMap2) {
		for (Byte i = 0; i <= Constants_colorOfThree.COLOR_PATTERN; i++) {
			switch (i) {
			case 0:
				colorPatternMap2.put("赤緑", i);
				break;
			case 1:
				colorPatternMap2.put("赤赤", i);
				break;
			case 2:
				colorPatternMap2.put("赤青", i);
				break;
			case 3:
				colorPatternMap2.put("赤白", i);
				break;
			case 4:
				colorPatternMap2.put("青赤", i);
				break;
			case 5:
				colorPatternMap2.put("青青", i);
				break;
			case 6:
				colorPatternMap2.put("青緑", i);
				break;
			case 7:
				colorPatternMap2.put("青白", i);
				break;
			case 8:
				colorPatternMap2.put("緑赤", i);
				break;
			case 9:
				colorPatternMap2.put("緑青", i);
				break;
			case 10:
				colorPatternMap2.put("緑緑", i);
				break;
			case 11:
				colorPatternMap2.put("緑白", i);
				break;
			case 12:
				colorPatternMap2.put("白赤", i);
				break;
			case 13:
				colorPatternMap2.put("白青", i);
				break;
			case 14:
				colorPatternMap2.put("白緑", i);
				break;
			case 15:
				colorPatternMap2.put("白白", i);
				break;
			case 16:
				colorPatternMap2.put("ss", i);
				break;
			}
		}
	}

	/**
	 * バイト列→イメージに変換
	 *
	 * @param バイト列
	 *            bytes
	 */
	public void getImageFromBytes(byte[] bytes) throws IOException {
		ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
		BufferedImage img = ImageIO.read(baos);
		decordeImgFrame.setVisible(true);
		decordeImgPanel.setimage(img);
		decordeImgFrame.repaint();
	}

	/**
	 * 適切な輪郭を判断する
	 *
	 * @param detectionContour
	 *            入力輪郭
	 * @param areaThreshold
	 *            輪郭大きさのしきい値
	 * @return 適切な輪郭であればTrueを返す
	 */
	private Boolean rectangleChecker(MatOfPoint detectionContour, int areaThreshold) {
		if (detectionContour.total() != Constants_colorOfThree.SIDE_OF_THE_RECTANGLE) {// 辺の数の確認。矩形判断
			return false;
		}
		if (Imgproc.contourArea(detectionContour) < areaThreshold) {// 面積の小さすぎる矩形を除外
			return false;
		}
		if (!Imgproc.isContourConvex(detectionContour)) { // 画像が凸であるかの確認。
			return false;
		}
		return true;
	}

	/**
	 * フィールド変数の変換キーを設定する。
	 *
	 * @param setKey
	 *            変換キーの番号
	 */
	private void setTransformKey(int setKey) {
		TransformKey = setKey;
	}

	/**
	 * 画像と輪郭を判断し、画像の傾きを分類したキーをフィールド変数TransformKeyに与える。<br>
	 * 画像の歪みを修正後、輪郭の各辺を水平、垂直に修正し出力する。
	 *
	 * @param detectionContour
	 *            入力画像内のマーカを示す輪郭
	 * @param srcImage
	 *            入力画像
	 * @param datImage
	 *            出力画像
	 * @param areaThreshold
	 *            輪郭大きさのしきい値
	 * @param division
	 *            マーカの行列分割値
	 * @return カラー・コードマーカを検出できたらTrueを返す。
	 */
	private Boolean markerChecker(MatOfPoint detectionContour, Mat srcImage, Mat datImage, int areaThreshold,
			int division) {
		if (rectangleChecker(detectionContour, areaThreshold) == false) {
			return false;
		}
		/*
		 * ４色確認 射影変換 変換元座標設定
		 */
		float srcPoint[] = new float[8];
		for (int i = 0; i < Constants_colorOfThree.MARKER_BLOCK; i++) {
			srcPoint[i * 2] = (float) detectionContour.get(i, 0)[0];
			srcPoint[i * 2 + 1] = (float) detectionContour.get(i, 0)[1];
		}
		Mat srcPointMat = new Mat(4, 2, CvType.CV_32F);
		srcPointMat.put(0, 0, srcPoint);

		// 変換後座標設定
		Mat dstPointMat = new Mat(4, 2, CvType.CV_32F);
		float[] dstPoint;
		dstPoint = new float[] { datImage.cols(), datImage.rows(), datImage.cols(), 0, 0, 0, 0, datImage.rows() };
		dstPointMat.put(0, 0, dstPoint);

		// 変換行列作成
		Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);

		// 図形変換処理
		Mat dstMat = new Mat(datImage.rows(), datImage.cols(), datImage.type());
		Imgproc.warpPerspective(srcImage, dstMat, r_mat, dstMat.size(), Imgproc.INTER_LINEAR);
		Mat cuttingImage = new Mat(dstMat, new Rect(0, 0, datImage.cols(), datImage.rows()));
		cuttingImage.copyTo(datImage);

		////////////////////////////////////// ４色確認Collar
		char[] collarCheckbox = new char[Constants_colorOfThree.MARKER_BLOCK];
		int boxCount = 0;
		double[] data = new double[Constants_colorOfThree.HSV_CH];// HSV各チャンネル格納用
		double oneThirdWidth = (datImage.rows()) / (double) division;
		double oneThirdHeight = (datImage.cols()) / (double) division;

		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
					int x = (int) ((j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					int y = (int) ((i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = cuttingImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意
					if (data[0] * 2 <= 45 || data[0] * 2 >= 330) {// H（色相）を元に色を判断
						collarCheckbox[boxCount++] = 'A';
					} else if (data[0] * 2 > 45 && data[0] * 2 <= 135) {
						collarCheckbox[boxCount++] = 'B';
					} else if (data[0] * 2 > 135 && data[0] * 2 <= 225) {
						collarCheckbox[boxCount++] = 'C';
					} else {
						// System.out.print( "青");
						collarCheckbox[boxCount++] = 'D';
					}
					Imgproc.circle(cuttingImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
				}
			}
		}
		String sumWord = new String(collarCheckbox);

		switch (sumWord) {
		case "ABCD":// 基準マーカ（マーカの角）はmarkerOutLinePoint[0]である
			setTransformKey(1);
			break;
		case "CADB":// markerOutLinePoint[1]が左上になるように変換
			setTransformKey(2);
			break;
		case "DCBA":// markerOutLinePoint[2]が左上になるように変換
			setTransformKey(3);
			break;
		case "BDAC":// markerOutLinePoint[3]が左上になるように変換
			setTransformKey(4);
			break;
		default:// 枠外エラー
			return false;
		}
		System.out.println(sumWord);
		cuttingImage.copyTo(datImage);
		return true;
	}

	/**
	 * 画像の傾きを分類したキーを元に射影変換を行い傾きを修正する 変換後の画像を出力サイズにカットし出力。
	 *
	 * @param srcImage
	 *            入力イメージ
	 * @param datImage
	 *            出力先イメージ
	 * @param TransformKey
	 *            変換キー
	 */
	private void transformMarker(Mat srcImage, Mat datImage, int TransformKey) {
		// 変換元座標設定
		float srcPoint[] = { 0, 0, srcImage.cols(), 0, srcImage.cols(), srcImage.rows(), 0, srcImage.rows() };

		Mat srcPointMat = new Mat(4, 2, CvType.CV_32F);
		srcPointMat.put(0, 0, srcPoint);
		// 変換後座標設定
		Mat dstPointMat = new Mat(4, 2, CvType.CV_32F);
		float[] dstPoint;
		switch (TransformKey) {
		case 1:// 基準マーカ（マーカの角）はmarkerOutLinePoint[0]である
			dstPoint = new float[] { 0, 0, srcImage.cols(), 0, srcImage.cols(), srcImage.rows(), 0, srcImage.rows() };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過1");
			break;
		case 2:// markerOutLinePoint[1]が左上になるように変換
			dstPoint = new float[] { 0, srcImage.rows(), 0, 0, srcImage.cols(), 0, srcImage.cols(), srcImage.rows() };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過2");
			break;
		case 3:// markerOutLinePoint[2]が左上になるように変換
			dstPoint = new float[] { srcImage.cols(), srcImage.rows(), 0, srcImage.rows(), 0, 0, srcImage.cols(), 0 };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過3");
			break;
		case 4:// markerOutLinePoint[3]が左上になるように変換
			dstPoint = new float[] { srcImage.cols(), 0, srcImage.cols(), srcImage.rows(), 0, srcImage.rows(), 0, 0 };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過4");
			break;
		default:// 枠外エラー
			System.out.println("error");
			return;
		}

		// 変換行列作成
		Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);
		// 図形変換処理
		Mat dstMat = new Mat(datImage.rows(), datImage.cols(), datImage.type());
		Imgproc.warpPerspective(srcImage, dstMat, r_mat, dstMat.size(), Imgproc.INTER_LINEAR);
		Mat cuttingImage = new Mat(dstMat, new Rect(0, 0, datImage.cols(), datImage.rows()));
		cuttingImage.copyTo(datImage);
	}

	/**
	 * 入力された画像内のカラー・コードを取得しListに保存する。
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
	 * @param division
	 *            マーカの行列分割値
	 */
	private void colorDecorde(Mat srcImage, double startX, double startY, double endX, double endY, int division) {
		double[] data = new double[Constants_colorOfThree.HSV_CH];// HSV各チャンネル格納用
		double[] pointData = new double[Constants_colorOfThree.HSV_CH];// HSV各チャンネル格納用(取得)
		double[] averageData = new double[Constants_colorOfThree.HSV_CH];// HSV各チャンネル格納用(平均算出)
		String[] colorPattern = new String[Constants_colorOfThree.BLOCK_OF_BYTE];// カラーパターン格納(取得)
		String mapOfPattern_First_str = null;// カラーパターン格納(前半２ブロック)
		String mapOfPattern_Second_str = null;// カラーパターン格納(後半２ブロック)
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		int indexCounter = 0;
		int missCount = 0;
		int loopCount = 0;
		int inImgBytesIndex = 0;
		// int inImgByteArrayIndex = 0;
		byte blocDecordeCount = 1;

		for (int i = 0; i < division; i++) {
			if (missCount > 0) {
				break;
			}
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
				} else {
					int x = (int) (startX + (j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					int y = (int) (startY + (i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					if (loopCount >= Constants_colorOfThree.BLOCK_OF_BYTE) {
						loopCount = 0;
					}
					// ---------------------取得点の色情報平均化-----------------------------------------------------------------------------------------------
					for (int k = 0; k < Constants_colorOfThree.NUMBER＿OF_POINT; k++) {
						switch (k) {
						case 0:
							averageData = srcImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意
							Imgproc.circle(srcImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
							break;
						case 1:
							pointData = srcImage.get(y + Constants_colorOfThree.DISTANCE＿OF_POINT,
									x - Constants_colorOfThree.DISTANCE＿OF_POINT);
							Imgproc.circle(srcImage,
									new Point(x - Constants_colorOfThree.DISTANCE＿OF_POINT,
											y + Constants_colorOfThree.DISTANCE＿OF_POINT),
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化(まだ平均値を取得未実装)
							for (int k2 = 0; k2 < averageData.length; k2++) {
								averageData[k2] += pointData[k2];
							}
							break;
						case 2:
							pointData = srcImage.get(y + Constants_colorOfThree.DISTANCE＿OF_POINT,
									x + Constants_colorOfThree.DISTANCE＿OF_POINT);
							Imgproc.circle(srcImage,
									new Point(x + Constants_colorOfThree.DISTANCE＿OF_POINT,
											y + Constants_colorOfThree.DISTANCE＿OF_POINT),
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化(まだ平均値を取得未実装)
							for (int k2 = 0; k2 < averageData.length; k2++) {
								averageData[k2] += pointData[k2];
							}
							break;
						case 3:
							pointData = srcImage.get(y - Constants_colorOfThree.DISTANCE＿OF_POINT,
									x + Constants_colorOfThree.DISTANCE＿OF_POINT);
							Imgproc.circle(srcImage,
									new Point(x + Constants_colorOfThree.DISTANCE＿OF_POINT,
											y - Constants_colorOfThree.DISTANCE＿OF_POINT),
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化(まだ平均値を取得未実装)
							for (int k2 = 0; k2 < averageData.length; k2++) {
								averageData[k2] += pointData[k2];
							}
							break;
						case 4:
							pointData = srcImage.get(y - Constants_colorOfThree.DISTANCE＿OF_POINT,
									x - Constants_colorOfThree.DISTANCE＿OF_POINT);
							Imgproc.circle(srcImage,
									new Point(x - Constants_colorOfThree.DISTANCE＿OF_POINT,
											y - Constants_colorOfThree.DISTANCE＿OF_POINT),
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化(まだ平均値を取得未実装)
							for (int k2 = 0; k2 < averageData.length; k2++) {
								averageData[k2] += pointData[k2];
							}
							break;
						}
						hsvImageFrame.repaint();// パネルを再描画
					}
					for (int k = 0; k < averageData.length; k++) {
						data[k] = (averageData[k] / Constants_colorOfThree.NUMBER＿OF_POINT);// 色相、彩度、明度それぞれの平均抽出
					}
					// ------------------------------------------------------------------------------------------------------------------------------------------
					// H（色相）S(彩度) V(明度)を元に色を判断;
					if (data[1] < 100 && data[2] >= 150) {
						receiveList.add("no");// 白
						colorPattern[loopCount] = "白";
					} else if (data[2] < 100) {
						receiveList.add("space");// 黒
						colorPattern[loopCount] = "s";
					} else if (data[1] >= 100 && data[0] * 2 >= 0 && data[0] * 2 < 80) {
						receiveList.add("1");// 赤
						colorPattern[loopCount] = "赤";
					} else if (data[1] >= 100 && data[0] * 2 >= 80 && data[0] * 2 < 148) {
						receiveList.add("2");// 緑
						colorPattern[loopCount] = "緑";
					} else if (data[1] >= 100 && data[0] * 2 >= 148 && data[0] * 2 < 258) {
						receiveList.add("3");// 青
						colorPattern[loopCount] = "青";
					} else {
						receiveList.add("error");
						System.out.println("errorの数値\n" + data[0] * 2 + " " + data[1] + " " + data[2]);
						colorPattern[loopCount] = "エラ";
					}
					// if (blocDecordeCount <=
					// Constants_colorOfThree.BLOCK_OF_BYTE) {
					// inImgBytes[inImgByteArrayIndex] = (byte)
					// hexadecimalOfBloc;
					// blocDecordeCount = 0;
					// hexadecimalOfBloc = 0;
					// inImgByteArrayIndex++;
					// }
					// if (blocDecordeCount ==
					// Constants_colorOfThree.COLORENCORD_BITS_SECOND_HALF) {
					// hexadecimalOfBloc = (hexadecimalOfBloc << 4);
					// }
					System.out.println(
							(i * (division - 1) + j) + "つ目のブロック" + (data[0] * 2) + " " + data[1] + " " + data[2]);
					blocDecordeCount++;
					// loopCount++;
					if (receiveList.isEmpty()
							|| createTransmisstionImage2_colorOfThree.getTransmissionList().isEmpty()) {
						System.out.println("送受信が行われていません");
					} else
						if (createTransmisstionImage2_colorOfThree.getTransmissionList().size() != receiveList.size()) {
						System.out.println("送受信の設定が間違っています");
						// System.out.println("受信データ数" + receiveList.size());
						// System.out.println("送信データ数" +
						// createTransmisstionImage2_colerOfThree.getTransmissionList().size());
					} else if (createTransmisstionImage2_colorOfThree.getTransmissionList().size() == receiveList
							.size()) {
						listCountCheck = true;
					} else {
					}
					System.out.println(createTransmisstionImage2_colorOfThree.getTransmissionList().size());
					if (!receiveList.isEmpty() && !createTransmisstionImage2_colorOfThree.getTransmissionList()
							.get(indexCounter).equals(receiveList.get(indexCounter))) {
						missCount++;
						System.out.println(indexCounter + "つめの送信dataが"
								+ createTransmisstionImage2_colorOfThree.getTransmissionList().get(indexCounter)
								+ "に対して" + "受信されたもの値が" + receiveList.get(indexCounter) + "だったため取得ミスです");
						listCountCheck = false;
						clearReceiveImgList();
						indexCounter = 0;
						break;
					} else if (missCount == 0 && listCountCheck == true) {
						setRunningKey(false);
					}
					if (loopCount == Constants_colorOfThree.COLORENCORD_BITS_FIRST_HALF_V) {
						mapOfPattern_First_str = colorPattern[Constants_colorOfThree.BLOCK_1_4]
								+ colorPattern[Constants_colorOfThree.BLOCK_2_4];
					} else if (loopCount == Constants_colorOfThree.COLORENCORD_BITS_SECOND_HALF_V) {
						mapOfPattern_Second_str = colorPattern[Constants_colorOfThree.BLOCK_3_4]
								+ colorPattern[Constants_colorOfThree.BLOCK_4_4];
					}
					indexCounter++;
					loopCount++;
					if (loopCount == Constants_colorOfThree.BLOCK_OF_BYTE) {
						if (!(mapOfPattern_First_str.equals("ss")) || !(mapOfPattern_Second_str.equals("ss"))) {
							inImgBytes.add((byte) (((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
									| (colorPatternMap_V.get(mapOfPattern_Second_str))));
							inImgBytesIndex++;
						}

					}
				}
			}
		}
	}

	/**
	 * カラー・コードを認識するまで受信処理をループ
	 */
	private void receiverLoop() {
		Mat markerImage = new Mat(500, 500, 16);

		List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();// 読み取った輪郭線を格納
		List<MatOfPoint> dorawOutLineList = new ArrayList<>();// 認識した矩形マーカの輪郭線を格納
		List<Mat> hsvList = new ArrayList<Mat>();
		// Listの宣言はループ内に移動Listのクリアが不要となった15/11/1(岩男
		Mat webcamImage = new Mat();// webカメラのイメージ
		captureCamera.read(webcamImage);// webカメラの映像を画像保存
		if (webcamImage.empty()) {// 画像が取得できているか判断
			System.out.println(" --(!) No captured frame -- Break!");// キャプチャの失敗時
			if (!captureCamera.isOpened()) {
				JOptionPane.showMessageDialog(null, "カメラが認識されていません、再度確認してください", "Warn", JOptionPane.WARNING_MESSAGE);
				stopRunning();
			}
			return;
		}
		Mat processedImage = new Mat(webcamImage.rows(), webcamImage.cols(), webcamImage.type());
		Mat hsvImage = new Mat(webcamImage.rows(), webcamImage.cols(), webcamImage.type());
		Imgproc.cvtColor(webcamImage, hsvImage, Imgproc.COLOR_BGR2HSV);// HSV変換
		Core.split(hsvImage, hsvList);// HSVでのマルチチャンネルをシングルチャネルとしてListにいれている（sizeは３）
		Mat valueImage = hsvList.get(2).clone();// そこから最後のチャネルを取ってきて複製
		Imgproc.threshold(valueImage, processedImage, 50, 255, Imgproc.THRESH_BINARY);// 画像の二値化
		Mat hierarchyData = new Mat();// 読み取った輪郭線の階層情報
		processedImageFrame.setSize(processedImage.width() + 40, processedImage.height() + 60);// ウィンドウサイズを取得画像に合ったサイズに
		hsvImageFrame.setSize(markerImage.width() + 35, markerImage.height() + 55);// ウィンドウサイズを取得画像に合ったサイズに
		// Imgproc.cvtColor(webc0amImage,
		// processedImage,Imgproc.COLOR_BGR2GRAY);グレースケール化
		// Imgproc.threshold(processedImage, processedImage, 0,
		// 255,Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);// 二値化
		Imgproc.findContours(processedImage, contoursList, hierarchyData, Imgproc.RETR_CCOMP,
				Imgproc.CHAIN_APPROX_SIMPLE);// 画像内の輪郭を検出

		for (int i = 0; i < contoursList.size(); i++) {// 取得した輪郭の総数でループ
			if (hierarchyData.get(0, i)[3] == -1) {// 内部輪郭を持つ輪郭を弾く
				MatOfPoint2f ptmat2Temp = new MatOfPoint2f();// 画像処理の途中でMatOfPoint2fに一時変換するため
				contoursList.get(i).convertTo(ptmat2Temp, CvType.CV_32FC2);// 画像処理のためMatOfPointをMatOfPoint2fに変換
				Imgproc.approxPolyDP(ptmat2Temp, ptmat2Temp, 10, true);// 輪郭を直線に近似する
				ptmat2Temp.convertTo(contoursList.get(i), CvType.CV_32S);// MatOfPoint2fをMatOfPointに再変換
				if (rectangleChecker(contoursList.get(i), 4000)) {
					dorawOutLineList.add(contoursList.get(i));// マーカであることが確定した輪郭を描画リストに追加
				}
				if (markerChecker(contoursList.get(i), hsvImage, markerImage, 1000, division)) {// 輪郭が正方形であるかチェック
					transformMarker(markerImage, markerImage, TransformKey);
					// Imgproc.medianBlur(markerImage, markerImage, 3);//
					// 画像のノイズ処理→平滑化
					colorDecorde(markerImage, 0, 0, markerImage.height(), markerImage.width(), division);
					List<Mat> hsvList2 = new ArrayList<Mat>();
					Core.split(markerImage, hsvList2);
					Mat hImage = hsvList2.get(0).clone();
					BufferedImage bufferedImageTemp1 = imageDrawing_colorOfThree.matToBufferedImage(hImage);// 描画のためmat型からbufferedImage型に変換
					hsvImagePanel.setimage(bufferedImageTemp1);// 変換した画像をPanelに追加
					hsvImageFrame.repaint();// パネルを再描画
					System.out.println("設定でのフレームレートは\n" + captureCamera.get(Videoio.CAP_PROP_FPS) + "\n現在のキャプチャモードは"
							+ captureCamera.get(Videoio.CAP_MODE_BGR) + "\n明るさは"
							+ captureCamera.get(Videoio.CAP_PROP_BRIGHTNESS) + "\nコントラストは"
							+ captureCamera.get(Videoio.CAP_PROP_CONTRAST) + "\n彩度は"
							+ captureCamera.get(Videoio.CAP_PROP_SATURATION) + "\n色相は"
							+ captureCamera.get(Videoio.CAP_PROP_HUE) + "\nゲインは"
							+ captureCamera.get(Videoio.CAP_PROP_GAIN) + "\n露出は"
							+ captureCamera.get(Videoio.CAP_PROP_EXPOSURE));
					if (runningKey == false && listCountCheck == true) {
						System.out.println("受信されたリストのサイズ" + receiveList.size() + "\nバイナリデータサイズ" + inImgBytes.size()
								+ "により\n取得成功しました");
						for (Iterator iterator = inImgBytes.iterator(); iterator.hasNext();) {
							System.out.println((Byte) iterator.next());

						}
					}
				}
			}
		}
		Imgproc.drawContours(processedImage, dorawOutLineList, -1, new Scalar(254, 0, 0), 5);// 輪郭画像にマーカ輪郭を表示
		contoursList.clear();// 輪郭リストをクリア
		BufferedImage bufferedImageTemp = imageDrawing_colorOfThree.matToBufferedImage(processedImage);// 描画のためmat型からbufferedImage型に変換
		processedImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		processedImageFrame.repaint();// パネルを再描画

	}
}

//// 3色用
// if (data[0] * 2 <= 60 || data[0] * 2 >= 300) {//
// receiveList.add("0");
// } else if (data[0] * 2 <= 180) {
// receiveList.add("120");
// } else {
// receiveList.add("240");
// }
// System.out.println(data[2]);

// 12色用
// if (data[0] * 2 <= 15 || data[0] * 2 >= 345) {
// receiveList.add("0");
// } else if (data[0] * 2 <= 45) {
// receiveList.add("30");
// } else if (data[0] * 2 <= 75) {
// receiveList.add("60");
// } else if (data[0] * 2 <= 115) {
// receiveList.add("90");
// } else if (data[0] * 2 <= 135) {
// receiveList.add("120");
// } else if (data[0] * 2 <= 165) {
// receiveList.add("150");
// } else if (data[0] * 2 <= 195) {
// receiveList.add("180");
// } else if (data[0] * 2 <= 225) {
// receiveList.add("210");
// } else if (data[0] * 2 <= 255) {
// receiveList.add("240");
// } else if (data[0] * 2 <= 285) {
// receiveList.add("270");
// } else if (data[0] * 2 <= 315) {
// receiveList.add("300");
// } else if (data[0] * 2 <= 345) {
// receiveList.add("330");
// }