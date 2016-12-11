
/*
 * 15/07/07 openCVについて知る
 * 15/07/08
 * 15/07/16 	15/07/07～15/07/16にかけてopenCVを利用することで可視光通信システムを自作できるのか見極めていた。
 * 				手探り状態で様々なコードを動かしていたため、本コード内にプログラムの残骸が転がっている。
 *
 * 15/07/17		残骸処理。念のため残骸txtに保存。
 * 				可視光通信システムのためのプログラミング処理の流れが固まってきた。本格的にプログラミングを進める。
 * 	15/07/29	コードに対してコメントやjavadocを追加。
 *
 *
 */
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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

/**
 * マーカver2用受信機<br>
 * startRunning()とstopRunning()を外部クラスから制御して利用。<br>
 * カラー・コードを認識するとListに情報を保持した状態で中断される。<br>
 * getDecodeList()でListを外部クラスに渡し、マッチテストは外部クラスで行う。<br>
 * CreateTransmisstionImage2で生成されたマーカver2に対応しており、CreateTransmisstionImage1で生成される
 * <br>
 * マーカver1とは互換性を持たない。<br>
 * フィールド変数であるdivisionの値をVisibleLightReceiver2のdivisionと揃えること。<br>
 *
 * @see CreateTransmisstionImage
 * @see CreateTransmisstionImage2
 * @see VisibleLightReceiver
 * @author iwao
 * @version 1.0
 */
public class VisibleLightReceiver2 extends Thread {


	private ImageDrawing imageDrawing = new ImageDrawing();
	private JFrame processedImageFrame;
	private JFrame hsvImageFrame;
	private ImageDrawing processedImagePanel;
	private ImageDrawing hsvImagePanel;
	private VideoCapture captureCamera;
	private boolean runningKey = false;
	private List<String> receiveList;
	private int TransformKey;
	private int division =8;
	private int misscount=0;

	public VisibleLightReceiver2() {
		// 自動生成されたコンストラクター・スタブ
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);// Opencvの利用のため
		captureCamera = new VideoCapture(0);// 使用webカメラの宣言
		processedImageFrame = new JFrame("processedImage");// 加工画像用ウィンドウフレーム
		hsvImageFrame = new JFrame("hsvImageFrame");// HSV画像用ウィンドウフレーム
		processedImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		hsvImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		processedImagePanel = new ImageDrawing();// 各種画像を載せるパネル
		hsvImagePanel = new ImageDrawing();// 各種画像を載せるパネル
		processedImageFrame.setContentPane(processedImagePanel);
		hsvImageFrame.setContentPane(hsvImagePanel);
		receiveList = new ArrayList<String>();// 最終的なデコード結果

	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see VisibleLightReceiver2#startRunning()
	 * @see VisibleLightReceiver2#stopRunning()
	 */
	public void run() {
		while (runningKey) {
			if (receiveList.isEmpty()) {
				receiverLoop();
			}
		}
	}

	/**
	 * 外部クラスから呼び出しクラスを実行する
	 *
	 * @see VisibleLightReceiver2#run()
	 * @see VisibleLightReceiver2#stopRunning()
	 */
	public void startRunning() {
		processedImageFrame.setVisible(true);
		hsvImageFrame.setVisible(true);
		runningKey = true;
		clearReceiveList();
		System.out.println(receiveList.size());
		new Thread(this).start();
	}

	/**
	 * 外部クラスから呼び出しクラス中断する
	 *
	 * @see VisibleLightReceiver2#run()
	 * @see VisibleLightReceiver2#startRunning()
	 */
	public void stopRunning() {
		processedImageFrame.setVisible(false);
		hsvImageFrame.setVisible(false);
		runningKey = false;
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
	 * 受信リストをクリア
	 */
	private void clearReceiveList() {
		receiveList.clear();
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
			System.out.println("era");
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
	 * 適切な輪郭を判断する
	 *
	 * @param detectionContour
	 *            入力輪郭
	 * @param areaThreshold
	 *            輪郭大きさのしきい値
	 * @return 適切な輪郭であればTrueを返す
	 */
	private Boolean rectangleChecker(MatOfPoint detectionContour, int areaThreshold) {
		if (detectionContour.total() != 4) {// 辺の数の確認。矩形判断
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
		//////// ４色確認
		//////// 射影変換
		// 変換元座標設定

		float srcPoint[] = new float[8];
		for (int i = 0; i < 4; i++) {
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
		//////// ４色確認
		char[] calarCheckbox = new char[4];
		int boxCount = 0;
		double[] data = new double[3];// HSV各チャンネル格納用
		double oneThirdWidth = (datImage.rows()) / (double) division;
		double oneThirdHeight = (datImage.cols()) / (double) division;

		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
					int x = (int) ((j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					int y = (int) ((i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = cuttingImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意

					if (data[0] * 2 <= 45 || data[0] * 2 >= 315) {// H（色相）を元に色を判断
						calarCheckbox[boxCount++] = 'A';
					} else if (data[0] * 2 > 45 && data[0] * 2 <= 135) {
						calarCheckbox[boxCount++] = 'B';
					} else if (data[0] * 2 > 135 && data[0] * 2 <= 225) {
						calarCheckbox[boxCount++] = 'C';
					} else {
						// System.out.print( "青");
						calarCheckbox[boxCount++] = 'D';
					}

					Imgproc.circle(cuttingImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化

				}
			}
			// System.out.println("");
		}
		String sumWord = new String(calarCheckbox);

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
	 * フィールド変数の変換キーを設定する。
	 *
	 * @param setKey
	 *            変換キーの番号
	 */
	private void setTransformKey(int setKey) {
		TransformKey = setKey;
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
		double[] data = new double[3];// HSV各チャンネル格納用
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;

		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
				} else {
					int x = (int) (startX + (j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					int y = (int) (startY + (i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = srcImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意
					// H（色相）を元に色を判断;

//					// 3色用
//					 if (data[0] * 2 <= 60 || data[0] * 2 >= 300) {//
//					 receiveList.add("0");
//					 } else if (data[0] * 2 <= 180) {
//					 receiveList.add("120");
//					 } else {
//					 receiveList.add("240");
//					 }

					 //6色用
					if (data[0] * 2 <= 30 || data[0] * 2 >= 330) {
						receiveList.add("0");
					} else if (data[0] * 2 <= 90) {
						receiveList.add("60");
					} else if (data[0] * 2 <= 150) {
						receiveList.add("120");
					} else if (data[0] * 2 <= 210) {
						receiveList.add("180");
					} else if (data[0] * 2 <= 270) {
						receiveList.add("240");
					} else if (data[0] * 2 <= 330) {
						receiveList.add("300");
					}

					// 12色用
//					 if (data[0] * 2 <= 15 || data[0] * 2 >= 345) {
//					 receiveList.add("0");
//					 } else if (data[0] * 2 <= 45) {
//					 receiveList.add("30");
//					 } else if (data[0] * 2 <= 75) {
//					 receiveList.add("60");
//					 } else if (data[0] * 2 <= 115) {
//					 receiveList.add("90");
//					 } else if (data[0] * 2 <= 135) {
//					 receiveList.add("120");
//					 } else if (data[0] * 2 <= 165) {
//					 receiveList.add("150");
//					 } else if (data[0] * 2 <= 195) {
//					 receiveList.add("180");
//					 } else if (data[0] * 2 <= 225) {
//					 receiveList.add("210");
//					 } else if (data[0] * 2 <= 255) {
//					 receiveList.add("240");
//					 } else if (data[0] * 2 <= 285) {
//					 receiveList.add("270");
//					 } else if (data[0] * 2 <= 315) {
//					 receiveList.add("300");
//					 } else if (data[0] * 2 <= 345) {
//					 receiveList.add("330");
//					 }

					Imgproc.circle(srcImage, new Point(x, y), 1, new Scalar(255, 255, 255), -1);// 色情報取得点可視化

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

		// Listの宣言はループ内に移動Listのクリアが不要となった15/11/1(岩男
		Mat webcamImage = new Mat();// webカメラのイメージ
		captureCamera.read(webcamImage);// webカメラの映像を画像保存
		if (webcamImage.empty()) {// 画像が取得できているか判断
			System.out.println(" --(!) No captured frame -- Break!");// キャプチャの失敗時
			misscount+=1;
			if(misscount==Constants.NO_CAPTURED_LEVEL){
				JOptionPane.showMessageDialog(null, "カメラが認識されていません、再度確認してください", "Warn",
				        JOptionPane.WARNING_MESSAGE);
				stopRunning();
				misscount=0;
			}
			return;
		}
		Mat processedImage = new Mat(webcamImage.rows(), webcamImage.cols(), webcamImage.type());
		Mat hsvImage = new Mat(webcamImage.rows(), webcamImage.cols(), webcamImage.type());
		Imgproc.cvtColor(webcamImage, hsvImage, Imgproc.COLOR_BGR2HSV);
		List<Mat> hsvList = new ArrayList<Mat>();
		Core.split(hsvImage, hsvList);
		Mat valueImage = hsvList.get(2).clone();
		Imgproc.threshold(valueImage, processedImage, 50, 255, Imgproc.THRESH_BINARY);
		Mat hierarchyData = new Mat();// 読み取った輪郭線の階層情報
		processedImageFrame.setSize(processedImage.width() + 40, processedImage.height() + 60);// ウィンドウサイズを取得画像に合ったサイズに
		hsvImageFrame.setSize(markerImage.width() + 35, markerImage.height() + 55);// ウィンドウサイズを取得画像に合ったサイズに
		// Imgproc.cvtColor(webc0amImage, processedImage,
		// Imgproc.COLOR_BGR2GRAY);// グレースケール化
		// Imgproc.threshold(processedImage, processedImage, 0, 255,
		// Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);// 二値化
		Imgproc.findContours(processedImage, contoursList, hierarchyData, Imgproc.RETR_CCOMP,
				Imgproc.CHAIN_APPROX_SIMPLE);// 画像内の輪郭を検出

		for (int i = 0; i < contoursList.size(); i++) {// 取得した輪郭の総数でループ
			if (hierarchyData.get(0, i)[3] == -1) {// 内部輪郭を持つ輪郭を弾く
				MatOfPoint2f ptmat2Temp = new MatOfPoint2f();// 画像処理の途中でMatOfPoint2fに一時変換するため
				contoursList.get(i).convertTo(ptmat2Temp, CvType.CV_32FC2);// 画像処理のためMatOfPointをMatOfPoint2fに変換
				Imgproc.approxPolyDP(ptmat2Temp, ptmat2Temp, 10, true);// 輪郭を直線に近似する
				ptmat2Temp.convertTo(contoursList.get(i), CvType.CV_32S);// MatOfPoint2fをMatOfPointをに再変換
				if (rectangleChecker(contoursList.get(i), 5000)) {
					dorawOutLineList.add(contoursList.get(i));// マーカであることが確定した輪郭を描画リストに追加
				}
				if (markerChecker(contoursList.get(i), hsvImage, markerImage, 1000, division)) {// 輪郭が正方形であるかチェック
					transformMarker(markerImage, markerImage, TransformKey);
					colorDecorde(markerImage, 0, 0, markerImage.height(), markerImage.width(), division);
					List<Mat> hsvList2 = new ArrayList<Mat>();
					Core.split(markerImage, hsvList2);
					Mat hImage = hsvList2.get(0).clone();
					BufferedImage bufferedImageTemp1 = imageDrawing.matToBufferedImage(hImage);// 描画のためmat型からbufferedImage型に変換
					hsvImagePanel.setimage(bufferedImageTemp1);// 変換した画像をPanelに追加
					hsvImagePanel.repaint();// パネルを再描画

				}
			}
		}
		Imgproc.drawContours(processedImage, dorawOutLineList, -1, new Scalar(254, 0, 0), 5);// 輪郭画像にマーカ輪郭を表示
		contoursList.clear();// 輪郭リストをクリア
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(processedImage);// 描画のためmat型からbufferedImage型に変換
		processedImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		processedImagePanel.repaint();// パネルを再描画

	}
}