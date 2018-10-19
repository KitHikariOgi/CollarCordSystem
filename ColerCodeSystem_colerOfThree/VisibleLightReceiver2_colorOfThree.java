import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
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
	private byte loopCountKeep;
	private int cameraCount;

	double[][] parts_Of_Data;
	String[][] receiveList_Panel;

	private boolean runningKey;
	private boolean keepCheck;
	private boolean listCountCheck;
	private boolean receiveAction;
	private boolean[] codeCheck;
	private boolean codeCountCheck;/*
									 * 今回は5つのパネルを用いて5つのコードをあらかじめ設定して動画像処理に焦点を合わせてプログラムしている。
									 * したがって、何枚で構成されているか分からないコードについては認識できない。
									 * 今後コード情報の部分にも全部で何枚であるのかというコードも組み込まなくてはならない。
									 */
	private char codeNoKeep = '\0';//コード情報(№)二ブロックのうち1ブロック目
	private String codeNo = "", PatternKeep_First_str = "";//コード情報(№)
	private ArrayList<String> receiveList_Parts;//最終的なコードの配列とパネルのコード配列
	private ArrayList<String> receiveList;//最終的なパネルのコードを二次元で管理(最後に受信リストに代入するもの)
	private List<Byte> inImgBytes;// 取得メディアバイナリデータ
	private HashMap<String, Integer> colorPatternMap_V;

	private CreateTransmisstionImage2_colorOfThree createTransmisstionImage2_colorOfThree;

	public VisibleLightReceiver2_colorOfThree(
			CreateTransmisstionImage2_colorOfThree createTransmisstionImage2_colorOfThree) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// Opencvの利用のため
		this.createTransmisstionImage2_colorOfThree = createTransmisstionImage2_colorOfThree;
		codeReceiverSystemPresetting();
		flagCheckSet();
		Pattern(colorPatternMap_V);
	}

	/**
	 * 各種初期設定
	 */
	private void codeReceiverSystemPresetting() {
		captureCamera = new VideoCapture(0);// 使用webカメラの宣言
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
		// 最終的なデコード受信結果(それぞれのパネルデータ)
		receiveList_Panel = new String[Constants_colorOfThree.CODE_NUMBER][Constants_colorOfThree.NUMBER＿OF_PANELDATA];
		receiveList_Parts = new ArrayList<String>();
		receiveList = new ArrayList<String>();
		// メディアバイナリデータ
		inImgBytes = new ArrayList<Byte>();
		// ブロックでのカラーパターンマップを記憶
		colorPatternMap_V = new HashMap<String, Integer>();
		codeCheck = new boolean[Constants_colorOfThree.CODE_NUMBER];
		//今回はコード枚数5枚,1枚の情報量が定まっているため可変長ではなく固定としてデータ配列を定めている。
		parts_Of_Data = new double[Constants_colorOfThree.CODE_NUMBER][Constants_colorOfThree.NUMBER＿OF_PARTS_OF_DATA];
	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see VisibleLightReceiver2_colorOfThree#startRunning()
	 * @see VisibleLightReceiver2_colorOfThree#stopRunning()
	 */
	public void run() {
		while (runningKey) {
			//System.out.println(cameraCount);
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
	public String[][] getReceiveList() {
		return receiveList_Panel;
	}

	/**
	 * 受信リスト,メディアバイナリデータをクリア
	 */
	private void clearReceiveImgList() {
		//明示的にByte[]outImgBytesを0に初期化
		//		if (receiveList_Panel.length != 0) {
		//			Arrays.fill(receiveList, null);
		//		}
		receiveList_Parts.clear();
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
	 * フラグの設定
	 */
	private void flagCheckSet() {
		runningKey = false;
		listCountCheck = false;
		codeCountCheck = false;
		keepCheck = false;
		receiveAction = true;
		for (int i = 0; i < Constants_colorOfThree.CODE_NUMBER; i++) {
			codeCheck[i] = false;
		}

	}

	/**
	 * カラーパターンの色の順序によって数値を振り分け設置
	 *
	 * @palam colorPatternMap2
	 */
	private void Pattern(HashMap<String, Integer> colorPatternMap2) {
		for (byte i = 0; i <= Constants_colorOfThree.COLOR_PATTERN; i++) {
			switch (i) {
			case 0:
				colorPatternMap2.put("赤緑", Constants_colorOfThree.BLOCPATTERN_RED_GREEN);
				break;
			case 1:
				colorPatternMap2.put("赤赤", Constants_colorOfThree.BLOCPATTERN_RED_RED);
				break;
			case 2:
				colorPatternMap2.put("赤青", Constants_colorOfThree.BLOCPATTERN_RED_BLUE);
				break;
			case 3:
				colorPatternMap2.put("赤白", Constants_colorOfThree.BLOCPATTERN_RED_WHITE);
				break;
			case 4:
				colorPatternMap2.put("青赤", Constants_colorOfThree.BLOCPATTERN_BLUE_RED);
				break;
			case 5:
				colorPatternMap2.put("青青", Constants_colorOfThree.BLOCPATTERN_BLUE_BLUE);
				break;
			case 6:
				colorPatternMap2.put("青緑", Constants_colorOfThree.BLOCPATTERN_BLUE_GREEN);
				break;
			case 7:
				colorPatternMap2.put("青白", Constants_colorOfThree.BLOCPATTERN_BLUE_WHITE);
				break;
			case 8:
				colorPatternMap2.put("緑赤", Constants_colorOfThree.BLOCPATTERN_GREEN_RED);
				break;
			case 9:
				colorPatternMap2.put("緑青", Constants_colorOfThree.BLOCPATTERN_GREEN_BLUE);
				break;
			case 10:
				colorPatternMap2.put("緑緑", Constants_colorOfThree.BLOCPATTERN_GREEN_GREEN);
				break;
			case 11:
				colorPatternMap2.put("緑白", Constants_colorOfThree.BLOCPATTERN_GREEN_WHITE);
				break;
			case 12:
				colorPatternMap2.put("白赤", Constants_colorOfThree.BLOCPATTERN_WHITE_RED);
				break;
			case 13:
				colorPatternMap2.put("白青", Constants_colorOfThree.BLOCPATTERN_WHITE_BLUE);
				break;
			case 14:
				colorPatternMap2.put("白緑", Constants_colorOfThree.BLOCPATTERN_WHITE_GREEN);
				break;
			case 15:
				colorPatternMap2.put("白白", Constants_colorOfThree.BLOCPATTERN_WHITE_WHITE);
				break;
			case 16:
				colorPatternMap2.put("ss", Constants_colorOfThree.BLOCPATTERN_SPACE);
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
	 * 			入力輪郭
	 * @param areaThreshold
	 * 			輪郭大きさのしきい値
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
	private Boolean markerChecker(MatOfPoint detectionContour, Mat srcImage, Mat datImage, int areaThreshold, int division) {
		if (rectangleChecker(detectionContour, areaThreshold) == false) {
			return false;
		}
		/*
		 * ４色確認 射影変換 変換元座標設定////////////////////////////////////////////////////////////////////////////
		 */
		float srcPoint[] = new float[8];
		Mat srcPointMat = new Mat(4, 2, CvType.CV_32F);
		for (int i = 0; i < Constants_colorOfThree.MARKER_BLOCK; i++) {
			srcPoint[i * 2] = (float) detectionContour.get(i, 0)[0];
			srcPoint[i * 2 + 1] = (float) detectionContour.get(i, 0)[1];
		}
		srcPointMat.put(0, 0, srcPoint);
		// 変換後座標設定//////////////////////////////////////////////////////////////////////////////////////////////
		Mat dstPointMat = new Mat(4, 2, CvType.CV_32F);
		float[] dstPoint;
		dstPoint = new float[] { datImage.cols(), datImage.rows(), datImage.cols(), 0, 0, 0, 0, datImage.rows() };
		dstPointMat.put(0, 0, dstPoint);
		// 変換行列作成////////////////////////////////////////////////////////////////////////////////////////////////
		Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);
		// 図形変換処理////////////////////////////////////////////////////////////////////////////////////////////////
		Mat dstMat = new Mat(datImage.rows(), datImage.cols(), datImage.type());
		Mat cuttingImage = new Mat(dstMat, new Rect(0, 0, datImage.cols(), datImage.rows()));
		Imgproc.warpPerspective(srcImage, dstMat, r_mat, dstMat.size(), Imgproc.INTER_LINEAR);
		cuttingImage.copyTo(datImage);
		//４色+コード№確認Collar//////////////////////////////////////////////////////////////////////////////////////
		char[] collarCheckbox = new char[Constants_colorOfThree.MARKER_BLOCK];
		int boxCount = 0, x = 0, y = 0;
		double[] data = new double[Constants_colorOfThree.HSV_CH];// HSV各チャンネル格納用
		double oneThirdWidth = (datImage.rows()) / (double) division;
		double oneThirdHeight = (datImage.cols()) / (double) division;
		codeNoKeep = ' ';
		codeNo = "";
		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0 || i == division - 1 && j == division - 1) {
					x = (int) ((j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					y = (int) ((i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = cuttingImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意(cuttingImage)
					if (data[0] * 2 <= 45 || data[0] * 2 >= 330) {// H（色相）を元に色を判断
						collarCheckbox[boxCount++] = 'A';
					} else if (data[0] * 2 > 45 && data[0] * 2 <= 135) {
						collarCheckbox[boxCount++] = 'B';
					} else if (data[0] * 2 > 135 && data[0] * 2 <= 225) {
						collarCheckbox[boxCount++] = 'C';
					} else {
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
		//		System.out.println(sumWord);
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
		int indexCounter = 0, missCount = 0, inImgBytesIndex = 0, x = 0, y = 0;
		byte loopCount = 0;
		String mapOfPattern_First_str = null;// カラーパターン格納(前半２ブロック)
		String mapOfPattern_Second_str = null;// カラーパターン格納(後半２ブロック)
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		// int inImgByteArrayIndex = 0;
		if (keepCheck == true) {
			loopCount = loopCountKeep;
			mapOfPattern_First_str = PatternKeep_First_str;
			keepCheck = false;
		}
		for (int i = 0; i < division; i++) {
			if (missCount > 0) {
				missCount = 0;
				receiveList_Parts.clear();
				break;
			}
			for (int j = 0; j < division; j++) {
				System.out.println(codeNo);
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0) {
				} else if (i == 0 && j == 1) {
					x = (int) ((j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					y = (int) ((i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = srcImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意
					Imgproc.circle(srcImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
					if (data[1] < 100 && data[2] >= 150) {// H（色相）S(彩度) V(明度)を元に色を判断;
						codeNoKeep = '白';
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 0 && data[0] * 2 < 80) {
						codeNoKeep = '赤';
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 80 && data[0] * 2 < 148) {
						codeNoKeep = '緑';
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 148 && data[0] * 2 < 258) {
						codeNoKeep = '青';
						continue;
					} else {
						codeNoKeep = ' ';
						//System.out.println("errorの数値\n" + data[0] * 2 + " " + data[1] + " " + data[2]);
						continue;
					}
				} else if (i == 0 && j == 2) {
					x = (int) ((j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					y = (int) ((i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = srcImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意
					Imgproc.circle(srcImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
					if (data[1] < 100 && data[2] >= 150) {// H（色相）S(彩度) V(明度)を元に色を判断;
						codeNo = codeNoKeep + "白";
						receiveList_Parts.clear();
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 0 && data[0] * 2 < 80) {
						codeNo = codeNoKeep + "赤";
						receiveList_Parts.clear();
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 80 && data[0] * 2 < 148) {
						codeNo = codeNoKeep + "緑";
						receiveList_Parts.clear();
						continue;
					} else if (data[1] >= 100 && data[0] * 2 >= 148 && data[0] * 2 < 258) {
						codeNo = codeNoKeep + "青";
						receiveList_Parts.clear();
						continue;
					} else {
						codeNoKeep = ' ';
						codeNo = "";
						receiveList_Parts.clear();
						//						System.out.println("errorの数値\n" + data[0] * 2 + " " + data[1] + " " + data[2]);
						continue;
					}
				} else if (i == division - 1 && j == division - 1) {
					if (loopCount == 2 && keepCheck == false) {
						PatternKeep_First_str = mapOfPattern_First_str;
						keepCheck = true;
					}
					switch (codeNo) {
					case "赤赤":
						if (codeCheck[0] == false) {
							codeCheck[0] = true;
							for (int j2 = 0; j2 < receiveList_Parts.size(); j2++) {
								receiveList_Panel[0][j2] = receiveList_Parts.get(j2);
							}
							codeCountCheck();
							receiveList_Parts.clear();
						}
						break;
					case "緑緑":
						if (codeCheck[1] == false) {
							codeCheck[1] = true;
							for (int j2 = 0; j2 < receiveList_Parts.size(); j2++) {
								receiveList_Panel[1][j2] = receiveList_Parts.get(j2);
							}
							codeCountCheck();
							receiveList_Parts.clear();
						}
						break;
					case "青青":
						if (codeCheck[2] == false) {
							codeCheck[2] = true;
							for (int j2 = 0; j2 < receiveList_Parts.size(); j2++) {
								receiveList_Panel[2][j2] = receiveList_Parts.get(j2);
							}
							codeCountCheck();
							receiveList_Parts.clear();
						}
						break;
					case "白白":
						if (codeCheck[3] == false) {
							codeCheck[3] = true;
							for (int j2 = 0; j2 < receiveList_Parts.size(); j2++) {
								receiveList_Panel[3][j2] = receiveList_Parts.get(j2);
							}
							codeCountCheck();
							receiveList_Parts.clear();
						}
						break;
					case "赤緑":
						if (codeCheck[4] == false) {
							codeCheck[4] = true;
							for (int j2 = 0; j2 < receiveList_Parts.size(); j2++) {
								receiveList_Panel[4][j2] = receiveList_Parts.get(j2);
							}
							codeCountCheck();
							receiveList_Parts.clear();
						}
						break;
					default:
						codeNoKeep = ' ';
						codeNo = "";
						receiveList_Parts.clear();
						break;
					}
				} else {
					x = (int) (startX + (j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					y = (int) (startY + (i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
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
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
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
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
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
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
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
									1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化
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
					if (data[1] < 100 && data[2] >= 150) {// H（色相）S(彩度) V(明度)を元に色を判断;
						receiveList_Parts.add("4");// 白
						colorPattern[loopCount] = "白";
					} else if (data[2] < 100) {
						receiveList_Parts.add("space");// 黒
						colorPattern[loopCount] = "s";
					} else if (data[1] >= 100 && data[0] * 2 >= 0 && data[0] * 2 < 80) {
						receiveList_Parts.add("1");// 赤
						colorPattern[loopCount] = "赤";
					} else if (data[1] >= 100 && data[0] * 2 >= 80 && data[0] * 2 < 148) {
						receiveList_Parts.add("2");// 緑
						colorPattern[loopCount] = "緑";
					} else if (data[1] >= 100 && data[0] * 2 >= 148 && data[0] * 2 < 258) {
						receiveList_Parts.add("3");// 青
						colorPattern[loopCount] = "青";
					} else {
						receiveList_Parts.add("error");
						missCount++;
						colorPattern[loopCount] = "エラ";
					}
					//					if (receiveList.isEmpty()
					//							|| createTransmisstionImage2_colorOfThree.getTransmissionList().isEmpty()) {
					//						System.out.println("送受信が行われていません");
					//					}
					//					else
					//						if (createTransmisstionImage2_colorOfThree.getTransmissionList().size() != receiveList.size()) {
					//						System.out.println("送受信の設定が間違っています");
					//						 System.out.println("受信データ数" + receiveList.size());
					//						 System.out.println("送信データ数" +
					//						 createTransmisstionImage2_colerOfThree.getTransmissionList().size());
					//					} else if (createTransmisstionImage2_colorOfThree.getTransmissionList().size() == receiveList
					//							.size()) {
					//						listCountCheck = true;
					//					} else {
					//					}
					//					System.out.println(createTransmisstionImage2_colorOfThree.getTransmissionList().size());
					//					if (!receiveList.isEmpty() && !createTransmisstionImage2_colorOfThree.getTransmissionList().get(indexCounter).equals(receiveList.get(indexCounter))) {
					//						missCount++;
					//						System.out.println(indexCounter + "つめの送信dataが"
					//								+ createTransmisstionImage2_colorOfThree.getTransmissionList().get(indexCounter)
					//								+ "に対して" + "受信されたもの値が" + receiveList.get(indexCounter) + "だったため取得ミスです");
					//						listCountCheck = false;
					//						clearReceiveImgList();
					//						indexCounter = 0;
					//						break;
					//					} else
					if (codeCountCheck == true && receiveAction == true) {//(missCount == 0 && listCountCheck == true)
						System.out.println("全てのコードパネル受信完了");
						setRunningKey(false);
						for (int k = 0; k < receiveList_Panel.length; k++) {
							for (int k2 = 0; k2 < receiveList_Panel[k].length; k2++) {
								if(!(StringUtils.isEmpty(receiveList_Panel[k][k2]))){
									receiveList.add(receiveList_Panel[k][k2]);
								}
							}
						}
						System.out.print("\n"+receiveList+"\n");
						System.out.print("\n"+this.createTransmisstionImage2_colorOfThree.getTransmissionList()+"\n");
						receiveAction = false;
						break;
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
							/*
							 * インデックスが一つしか用意していないので一つのパネルにおいて取得が時間内に間に合わなかった場合インデックスが初期化されてしまう可能性あり
							 */
							switch (codeNo) {
							case "赤赤":
								//								if (codeCheck[Constants_colorOfThree.PANELPATTERN_RED_RED] == false) {
								//									parts_Of_Data[Constants_colorOfThree.PANELPATTERN_RED_RED][inImgBytesIndex] = ((byte) ((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
								//											| (colorPatternMap_V.get(mapOfPattern_Second_str)));
								//								}
								break;
							case "緑緑":
								//								if (codeCheck[Constants_colorOfThree.PANELPATTERN_GREEN_GREEN] == false) {
								//									parts_Of_Data[Constants_colorOfThree.PANELPATTERN_GREEN_GREEN][inImgBytesIndex] = ((byte) ((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
								//											| (colorPatternMap_V.get(mapOfPattern_Second_str)));
								//								}
								break;
							case "青青":
								//								if (codeCheck[Constants_colorOfThree.PANELPATTERN_BLUE_BLUE] == false) {
								//									parts_Of_Data[Constants_colorOfThree.PANELPATTERN_BLUE_BLUE][inImgBytesIndex] = ((byte) ((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
								//											| (colorPatternMap_V.get(mapOfPattern_Second_str)));
								//								}
								break;
							case "白白":
								//								if (codeCheck[Constants_colorOfThree.PANELPATTERN_WHITE_WHITE] == false) {
								//									parts_Of_Data[Constants_colorOfThree.PANELPATTERN_WHITE_WHITE][inImgBytesIndex] = ((byte) ((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
								//											| (colorPatternMap_V.get(mapOfPattern_Second_str)));
								//								}
								break;
							case "赤緑":
								//								if (codeCheck[Constants_colorOfThree.PANELPATTERN_RED_GREEN] == false) {
								//									parts_Of_Data[Constants_colorOfThree.PANELPATTERN_RED_GREEN][inImgBytesIndex] = ((byte) ((colorPatternMap_V.get(mapOfPattern_First_str)) << 4)
								//											| (colorPatternMap_V.get(mapOfPattern_Second_str)));
								//								}
								break;
							default:
								break;
							}
							inImgBytesIndex++;
							//inImgBytes.add((byte) (((colorPatternMap_V.get(mapOfPattern_First_str)) << 4) | (colorPatternMap_V.get(mapOfPattern_Second_str))));
						}
					}
				}
			}
		}

	}

	/**
	 * 全パネルのコードが取得できたかを判別
	 */
	private void codeCountCheck() {
		codeCountCheck = true;
		for (boolean Count_TorF : codeCheck) {
			if (!Count_TorF) {
				codeCountCheck = false;
				break;
			}
		}
	}

	/**
	 * カラー・コードを認識するまで受信処理をループ
	 * ( Listの宣言はループ内に移動Listのクリアが不要となった 15/11/1 岩男 )
	 */
	private void receiverLoop() {
		Mat markerImage = new Mat(500, 500, 16);
		List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();// 読み取った輪郭線を格納
		List<MatOfPoint> dorawOutLineList = new ArrayList<>();// 認識した矩形マーカの輪郭線を格納
		List<Mat> hsvList = new ArrayList<Mat>();
		Mat webcamImage = new Mat();// webカメラのイメージ
		captureCamera.read(webcamImage);// webカメラの映像を画像保存
		cameraCheck(webcamImage);
		//カメラ(設定項目ID) 露出(15) ゲイン(14) 明るさ(10) コントラスト(11) 色の強さ(12) 白バランス(17)
		cameraCount++;
		if (cameraCount == 100) {
			System.out.println("露出：" + captureCamera.get(Constants_colorOfThree.ID_EXP));
			System.out.println("ゲイン：" + captureCamera.get(Constants_colorOfThree.ID_GAIN));
			System.out.println("明るさ：" + captureCamera.get(Constants_colorOfThree.ID_BRIGHT));
			System.out.println("コントラスト：" + captureCamera.get(Constants_colorOfThree.ID_CON));
			System.out.println("色の強さ：" + captureCamera.get(Constants_colorOfThree.ID_SAT));
			System.out.println("白バランス：" + captureCamera.get(Constants_colorOfThree.ID_WHITE));
			cameraCount = 0;
		}
		captureCamera.set(Constants_colorOfThree.ID_EXP, Constants_colorOfThree.EXP);
		captureCamera.set(Constants_colorOfThree.ID_GAIN, Constants_colorOfThree.GAIN);
		captureCamera.set(Constants_colorOfThree.ID_BRIGHT, Constants_colorOfThree.BRIGHT);
		captureCamera.set(Constants_colorOfThree.ID_CON, Constants_colorOfThree.CON);
		captureCamera.set(Constants_colorOfThree.ID_SAT, Constants_colorOfThree.SAT);
		captureCamera.set(Constants_colorOfThree.ID_WHITE, Constants_colorOfThree.WHITE);
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
		Imgproc.findContours(processedImage, contoursList, hierarchyData, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);// 画像内の輪郭を検出
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
					if (codeNo.equals("赤赤") && codeCheck[0] == true) {
						System.out.println("赤赤" + codeCheck[0]);
					} else if (codeNo.equals("緑緑") && codeCheck[1] == true) {
						System.out.println("緑緑" + codeCheck[1]);
					} else if (codeNo.equals("青青") && codeCheck[2] == true) {
						System.out.println("青青" + codeCheck[2]);
					} else if (codeNo.equals("白白") && codeCheck[3] == true) {
						System.out.println("白白" + codeCheck[3]);
					} else if (codeNo.equals("赤緑") && codeCheck[4] == true) {
						System.out.println("赤緑" + codeCheck[4]);
					} else {
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
						//						System.out.println("設定でのフレームレートは\n" + captureCamera.get(Videoio.CAP_PROP_FPS) + "\n現在のキャプチャモードは"
						//								+ captureCamera.get(Videoio.CAP_MODE_BGR) + "\n明るさは"
						//								+ captureCamera.get(Videoio.CAP_PROP_BRIGHTNESS) + "\nコントラストは"
						//								+ captureCamera.get(Videoio.CAP_PROP_CONTRAST) + "\n彩度は"
						//								+ captureCamera.get(Videoio.CAP_PROP_SATURATION) + "\n色相は"
						//								+ captureCamera.get(Videoio.CAP_PROP_HUE) + "\nゲインは"
						//								+ captureCamera.get(Videoio.CAP_PROP_GAIN) + "\n露出は"
						//								+ captureCamera.get(Videoio.CAP_PROP_EXPOSURE));
						if (runningKey == false && listCountCheck == true) {
							//							System.out.println("受信されたリストのサイズ" + receiveList.size() + "\nバイナリデータサイズ" + inImgBytes.size() + "により\n取得成功しました");
							//							for (Iterator iterator = inImgBytes.iterator(); iterator.hasNext();) {
							//								System.out.println((Byte) iterator.next());
							//							}
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

	/**
	 * @param webcamImage
	 */
	private void cameraCheck(Mat webcamImage) {
		if (webcamImage.empty()) {// 画像が取得できているか判断
			System.out.println(" --(!) No captured frame -- Break!");// キャプチャの失敗時
			if (!captureCamera.isOpened()) {
				JOptionPane.showMessageDialog(null, "カメラが認識されていません、再度確認してください", "Warn", JOptionPane.WARNING_MESSAGE);
				stopRunning();
			}
			return;
		}
	}
}