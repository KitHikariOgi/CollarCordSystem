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
//import java.awt.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 * マーカver1用受信機<br>
 * startRunning()とstopRunning()を外部クラスから制御して利用。<br>
 * カラー・コードを認識するとListに情報を保持した状態で中断される。<br>
 * getDecodeList()でListを外部クラスに渡し、マッチテストは外部クラスで行う。<br>
 * CreateTransmisstionImageで生成されたマーカver1に対応しており、CreateTransmisstionImage2で生成される
 * <br>
 * マーカver2とは互換性を持たない。<br>
 * 画像の傾きを補正する事ができるが、歪みに対する補正機能が正しく働いていない。
 * 原因は３点の認識マーカを含む最小の「矩形」を生成しマーカ範囲を設定しているため、 歪んだ状態のマーカを歪んだ状態のまま取得できていないためである。<br>
 * CreateTransmisstionImage2およびVisibleLightReceiver2を利用するマーカーver2と比較して
 * マーカver1が優れている点はないと思われる。現状マーカver2との性能比較のためのクラスである。
 *
 * @see CreateTransmisstionImage
 * @author iwao
 * @version 1.0
 */
public class VisibleLightReceiver extends Thread {
	private ImageDrawing imageDrawing = new ImageDrawing();
	private JFrame processedImageFrame;
	private JFrame hsvImageFrame;
	private ImageDrawing processedImagePanel;
	private ImageDrawing hsvImagePanel;
	private VideoCapture captureCamera;
	private boolean runningKey = false;
	private List<String> receiveList;
	private int division = 3;

	public VisibleLightReceiver() {
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
	 * @see VisibleLightReceiver#startRunning()
	 * @see VisibleLightReceiver#stopRunning()
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
	 * @see VisibleLightReceiver#run()
	 * @see VisibleLightReceiver#stopRunning()
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
	 * @see VisibleLightReceiver#run()
	 * @see VisibleLightReceiver#startRunning()
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
	 * 座標ABの長さを求めます。
	 *
	 * @param pointA
	 *            線分ABを構成する点Aの座標
	 * @param pointB
	 *            線分ABを構成する点Bの座標
	 * @return 線分ABの長さ
	 */
	private int getDistance(Point pointA, Point pointB) {
		double distance = Math
				.sqrt((pointB.x - pointA.x) * (pointB.x - pointA.x) + (pointB.y - pointA.y) * (pointB.y - pointA.y));
		return (int) distance;
	}

	/**
	 * 座標ABのベクトルを求めます。
	 *
	 * @param pointA
	 *            ベクトルABを構成する点Aの座標
	 * @param pointB
	 *            ベクトルABを構成する点Bの座標
	 * @return ベクトルAB
	 */
	private Point getVector(Point pointA, Point pointB) {
		Point vectorAB = new Point(pointB.x - pointA.x, pointB.y - pointA.y);
		return vectorAB;
	}

	/**
	 * ベクトルA、ベクトルBの内積を求める
	 *
	 * @param A
	 *            ベクトルA
	 * @param B
	 *            ベクトルB
	 * @return ベクトルA、ベクトルBの内積の絶対値
	 */
	private double getInnerProduct(Point A, Point B) {
		double innerProduct;
		innerProduct = (A.x * B.x + A.y * B.y);
		return Math.abs(innerProduct);
	}

	/**
	 * 矩形の中心座標を求める
	 *
	 * @param A
	 *            矩形頂点A
	 * @param B
	 *            矩形頂点B
	 * @param C
	 *            矩形頂点C
	 * @param D
	 *            矩形頂点D
	 * @return 四点座標の絶対値
	 */
	private Point getCenterPoint(Point A, Point B, Point C, Point D) {
		double xTotal;
		double yTotal;
		xTotal = (A.x + B.x + C.x + D.x) / 4;
		yTotal = (B.y + B.y + C.y + D.y) / 4;
		return new Point(xTotal, yTotal);
	}

	/**
	 * 画像を判断し、画像の傾きを分類したキーを返す
	 *
	 * @param markerOutLinePoint
	 *            判断画像の頂点リスト
	 * @param markCollerPoint
	 *            3点ある認識マーカの中央座標リスト
	 * @param rowsLimit
	 *            イメージの高さ
	 * @param colsLimit
	 *            イメージの幅
	 * @return 四点座標の絶対値
	 */
	private int getTransformKey(Point[] markerOutLinePoint, Point[] markCollerPoint, int rowsLimit, int colsLimit) {
		Point baceMarkerPoint;
		int TransformKey = 0;
		for (int i = 0; i < 4; ++i) {
			if (colsLimit < markerOutLinePoint[i].x || markerOutLinePoint[i].x < 0
					|| rowsLimit < markerOutLinePoint[i].y || markerOutLinePoint[i].x < 0) {
				return 4;
			}
		}
		if (getDistance(markCollerPoint[0], markCollerPoint[1]) > getDistance(markCollerPoint[0], markCollerPoint[2])
				&& getDistance(markCollerPoint[0], markCollerPoint[1]) > getDistance(markCollerPoint[1],
						markCollerPoint[2])) {
			baceMarkerPoint = markCollerPoint[2];
			// System.out.println("通過１");
		} else if (getDistance(markCollerPoint[0], markCollerPoint[2]) > getDistance(markCollerPoint[1],
				markCollerPoint[2])) {
			baceMarkerPoint = markCollerPoint[1];
			// System.out.println("通過２");
		} else {
			baceMarkerPoint = markCollerPoint[0];
			// System.out.println("通過３");
		}
		int minDistance = 1000000;
		for (int i = 0; i < 4; ++i) {
			if (getDistance(baceMarkerPoint, markerOutLinePoint[i]) < minDistance) {
				minDistance = getDistance(baceMarkerPoint, markerOutLinePoint[i]);
				TransformKey = i;
			}
		}
		return TransformKey;
	}

	/**
	 * 画像の傾きを分類したキーを元に射影変換を行い傾きを修正する 変換後の画像を出力サイズにカットし出力。
	 *
	 * @param markerOutLinePoint
	 *            判断画像の頂点リスト
	 * @param srcImage
	 *            入力イメージ
	 * @param datImage
	 *            出力先イメージ
	 * @param TransformKey
	 *            変換キー
	 */
	private void transformMarker(Point[] markerOutLinePoint, Mat srcImage, Mat datImage, int TransformKey) {
		// 変換元座標設定

		float srcPoint[] = new float[8];
		for (int i = 0; i < 4; ++i) {
			srcPoint[i * 2] = (float) markerOutLinePoint[i].x;
			srcPoint[i * 2 + 1] = (float) markerOutLinePoint[i].y;
			// System.out.println(x);
		}
		Mat srcPointMat = new Mat(4, 2, CvType.CV_32F);
		srcPointMat.put(0, 0, srcPoint);
		// 変換後座標設定
		Mat dstPointMat = new Mat(4, 2, CvType.CV_32F);
		float[] dstPoint;
		switch (TransformKey) {
		case 0:// 基準マーカ（マーカの角）はmarkerOutLinePoint[0]である
			dstPoint = new float[] { 0, 0, datImage.cols(), 0, datImage.cols(), datImage.rows(), 0, datImage.rows() };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過1");
			break;
		case 1:// markerOutLinePoint[1]が左上になるように変換
			dstPoint = new float[] { 0, datImage.rows(), 0, 0, datImage.cols(), 0, datImage.cols(), datImage.rows() };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過2");
			break;
		case 2:// markerOutLinePoint[2]が左上になるように変換
			dstPoint = new float[] { datImage.cols(), datImage.rows(), 0, datImage.rows(), 0, 0, datImage.cols(), 0 };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過3");
			break;
		case 3:// markerOutLinePoint[3]が左上になるように変換
			dstPoint = new float[] { datImage.rows(), 0, datImage.rows(), datImage.cols(), 0, datImage.cols(), 0, 0 };
			dstPointMat.put(0, 0, dstPoint);
			// System.out.println("通過4");
			break;
		case 4:// 枠外エラー
			System.out.println("通過5");
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
	 * @param diveCount
	 *            再帰処理を行う回数
	 */
	private void colorDecorde(Mat srcImage, double startX, double startY, double endX, double endY, int diveCount) {
		double[] data = new double[3];// HSV各チャンネル格納用
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		if (diveCount == 0) {
			// System.out.println("x" + startX + "のy" + startY);
			for (int i = 0; i < division; i++) {
				for (int j = 0; j < division; j++) {
					int x = (int) (startX + (j * oneThirdWidth + (j + 1) * oneThirdWidth) / 2);
					int y = (int) (startY + (i * oneThirdHeight + (i + 1) * oneThirdHeight) / 2);
					data = srcImage.get(y, x);// HSV各チャンネルを格納(y,x)なので注意

					// 3色用
					if (data[0] * 2 <= 60 || data[0] * 2 >= 300) {// H（色相）を元に色を判断;
						receiveList.add("0");
					} else if (data[0] * 2 <= 180) {
						receiveList.add("120");
					} else {
						receiveList.add("240");
					}

					// 6色用
					// if (data[0] * 2 <= 30 || data[0] * 2 >= 330) {//
					// H（色相）を元に色を判断;
					// receiveList.add(0);
					// } else if (data[0] * 2 <= 90) {
					// receiveList.add(60);
					// } else if (data[0] * 2 <= 150) {
					// receiveList.add(120);
					// } else if (data[0] * 2 <= 210) {
					// receiveList.add(180);
					// } else if (data[0] * 2 <= 270) {
					// receiveList.add(240);
					// } else if (data[0] * 2 <= 330) {
					// receiveList.add(300);
					// }

					// 12色用
					// if (data[0] * 2 <= 15 || data[0] * 2 >= 345) {//
					// H（色相）を元に色を判断;
					// receiveList.add(0);
					// } else if (data[0] * 2 <= 45) {
					// receiveList.add(30);
					// } else if (data[0] * 2 <= 75) {
					// receiveList.add(60);
					// } else if (data[0] * 2 <= 115) {
					// receiveList.add(90);
					// } else if (data[0] * 2 <= 135) {
					// receiveList.add(120);
					// } else if (data[0] * 2 <= 165) {
					// receiveList.add(150);
					// } else if (data[0] * 2 <= 195) {
					// receiveList.add(180);
					// } else if (data[0] * 2 <= 225) {
					// receiveList.add(210);
					// } else if (data[0] * 2 <= 255) {
					// receiveList.add(240);
					// } else if (data[0] * 2 <= 285) {
					// receiveList.add(270);
					// } else if (data[0] * 2 <= 315) {
					// receiveList.add(300);
					// } else if (data[0] * 2 <= 345) {
					// receiveList.add(330);
					// }
					Imgproc.circle(srcImage, new Point(x, y), 5, new Scalar(255, 255, 255), -1);// 色情報取得点可視化

				}
				// System.out.println("");
			}
		} else {
			for (int i = 0; i < division; i++) {
				for (int j = 0; j < division; j++) {
					if (diveCount == 1) {
						if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0) {
						} else {
							colorDecorde(srcImage, startX + j * oneThirdWidth, startY + i * oneThirdHeight,
									startX + (j + 1) * oneThirdWidth, startY + (i + 1) * oneThirdHeight, diveCount - 1);
						}
					} else {
						colorDecorde(srcImage, startX + j * oneThirdWidth, startY + i * oneThirdHeight,
								startX + (j + 1) * oneThirdWidth, startY + (i + 1) * oneThirdHeight, diveCount - 1);
					}
					// System.out.println(i + "の" + j );
				}
			}
		}

	}

	/**
	 * 引数の輪郭が正方形であるかどうかを判断します。
	 *
	 * @param quadrangle
	 *            チェックされる輪郭データ
	 * @param squareThreshold
	 *            正方形らしさのしきい値
	 * @param areaThreshold
	 *            面積に関するしきい値
	 * @return 正方形であればTrueを返す
	 */
	private boolean squareChecker(MatOfPoint quadrangle, float squareThreshold, int areaThreshold) {
		// 四角形の面積、正方形らしさに関するしきい値の設定変数を引数に追加15/07/17 岩男
		// float squareThreshold = 10;引数化にのためコメントアウト15/07/17 岩男
		Point[] tBox = new Point[4];// 矩形輪郭線を構成する４点のPointを格納

		if (quadrangleChecker(quadrangle, areaThreshold)) {// 輪郭を矩形輪郭のみに絞る
			for (int i = 0; i < 4; i++) {
				tBox[i] = new Point((int) quadrangle.get(i, 0)[0], (int) quadrangle.get(i, 0)[1]);// 輪郭の構成Pointを格納
			}
			int lineDistanceTemp;// 2頂点の長さを一時保存
			int longDistance1st = 0;// 頂点同士を結ぶ線分の中で一番目に長い線分
			int longDistance2nd = 0;// 頂点同士を結ぶ線分の中で二番目に長い線分
			Point[][] longLinePoint = new Point[2][2];// 長い線分を構成する二点のPointを保存longLinePoint[0:一番長い、1:二番目に長い][0:座標A、1:座標B]

			for (int i = 0; i < 3; i++) {// i,jにより4C2の座標の組み合わせを試行して長いものをlongLinePointに格納
				for (int j = i + 1; j < 4; j++) {
					lineDistanceTemp = getDistance(tBox[i], tBox[j]);
					if (longDistance1st <= lineDistanceTemp) {
						longDistance2nd = longDistance1st;
						longLinePoint[1][0] = longLinePoint[0][0];
						longLinePoint[1][1] = longLinePoint[0][1];
						longDistance1st = lineDistanceTemp;
						longLinePoint[0][0] = tBox[i];
						longLinePoint[0][1] = tBox[j];
					} else if (longDistance2nd <= lineDistanceTemp) {
						longDistance2nd = lineDistanceTemp;
						longLinePoint[1][0] = tBox[i];
						longLinePoint[1][1] = tBox[j];
					}
				}

			}
			Point vectorA = getVector(longLinePoint[0][0], longLinePoint[0][1]);// ベクトル生成
			Point vectorB = getVector(longLinePoint[1][0], longLinePoint[1][1]);// ベクトル生成
			double InnerProduct = getInnerProduct(vectorA, vectorB);// ベクトルABの内積を生成

			if (InnerProduct < squareThreshold) {// 正方形らしさとその閾値のチェック
				return true;
			}
		}
		return false;
	}

	/**
	 * 輪郭線が矩形であるかどうかを判断します。
	 *
	 * @param outlinePoint
	 *            チェックされる輪郭データ
	 * @param areaThreshold
	 *            面積に関するしきい値
	 * @return 矩形であればTrueを返す。
	 */
	private boolean quadrangleChecker(MatOfPoint outlinePoint, int areaThreshold) {
		if (outlinePoint.total() == 4) {// 辺の数の確認。矩形判断
			if (Imgproc.contourArea(outlinePoint) > areaThreshold) {// 面積の小さすぎる矩形を除外
				// if (Imgproc.isContourConvex(outlinePoint))
				// {//画像が凸であるかの確認。矩形判断では必要ないのでコメントアウト15/07/29岩男

				return true;
				// }
			}
			// System.out.println(areaThreshold+">"+Imgproc.contourArea(outlinePoint));
		}

		return false;
	}

	/**
	 * カラー・コードを認識するまで受信処理をループ
	 */
	private void receiverLoop() {
		Mat markerImage = new Mat(500, 500, 16);
		List<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();// 読み取った輪郭線を格納
		List<MatOfPoint> dorawOutLineList = new ArrayList<>();// 認識した矩形マーカの輪郭線を格納
		List<Point> pointsList = new ArrayList<Point>();// 認識した各矩形マーカの座標を格納]
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
		Imgproc.cvtColor(webcamImage, hsvImage, Imgproc.COLOR_BGR2HSV);
		List<Mat> hsvList = new ArrayList<Mat>();
		Core.split(hsvImage, hsvList);
		Mat valueImage = hsvList.get(2).clone();
		Imgproc.threshold(valueImage, processedImage, 200, 255, Imgproc.THRESH_BINARY);
		Mat hierarchyData = new Mat();// 読み取った輪郭線の階層情報
		processedImageFrame.setSize(processedImage.width() + 40, processedImage.height() + 60);// ウィンドウサイズを取得画像に合ったサイズに
		hsvImageFrame.setSize(markerImage.width() + 35, markerImage.height() + 55);// ウィンドウサイズを取得画像に合ったサイズに
		Imgproc.findContours(processedImage, contoursList, hierarchyData, Imgproc.RETR_TREE,
				Imgproc.CHAIN_APPROX_SIMPLE);// 画像内の輪郭を検出
		for (int i = 0; i < contoursList.size(); i++) {// 取得した輪郭の総数でループ
			// 処理数低減のため内部輪郭を持たない輪郭をココで弾くように変更。15/07/29岩男
			if (hierarchyData.get(0, i)[2] != -1 && hierarchyData.get(0, (int) (hierarchyData.get(0, i)[2]))[2] != -1) {// 内部輪郭を持たない輪郭を弾く

				// MatOfPoint
				// ptmat = contours.get(i); ptmat.convertTo(ptmat2,
				// CvType.CV_32FC2); Imgproc.approxPolyDP(ptmat2,
				// ptmat2, 10,false); ptmat2.convertTo(approxf1,
				// CvType.CV_32S); contours.set(i,approxf1);
				// 以上の文では正しく輪郭点の変換がされない。15/07/12岩男
				//
				MatOfPoint2f ptmat2Temp = new MatOfPoint2f();// 画像処理の途中でMatOfPoint2fに一時変換するため
				contoursList.get(i).convertTo(ptmat2Temp, CvType.CV_32FC2);// 画像処理のためMatOfPointをMatOfPoint2fに変換
				Imgproc.approxPolyDP(ptmat2Temp, ptmat2Temp, 10, true);// 輪郭を直線に近似する
				ptmat2Temp.convertTo(contoursList.get(i), CvType.CV_32S);// MatOfPoint2fをMatOfPointをに再変換
				if (squareChecker(contoursList.get(i), 500, 10)) {// 輪郭が正方形であるかチェック、ここに入る輪郭は内部輪郭を持つもののみ
					contoursList.get((int) hierarchyData.get(0, i)[2]).convertTo(ptmat2Temp, CvType.CV_32FC2);// 画像処理のためMatOfPointをMatOfPoint2fに変換
					Imgproc.approxPolyDP(ptmat2Temp, ptmat2Temp, 10, true);// 輪郭を直線に近似する
					ptmat2Temp.convertTo(contoursList.get((int) hierarchyData.get(0, i)[2]), CvType.CV_32S);// MatOfPoint2fをMatOfPointをに再変換
					// 処理数低減のため内部輪郭を持たない輪郭の直線近似を行っていなかった。そのためここで改めて輪郭を直線に近似する。15/07/29岩男

					if (squareChecker(contoursList.get((int) hierarchyData.get(0, i)[2]), 500, 10)) {// 輪郭が正方形であるかチェック、ここに入る輪郭は正方形が認められた輪郭の内部輪郭
						dorawOutLineList.add(contoursList.get(i));// マーカであることが確定した輪郭を描画リストに追加
						for (int i1 = 0; i1 < 4; i1++) {
							pointsList.add(
									new Point(contoursList.get(i).get(i1, 0)[0], contoursList.get(i).get(i1, 0)[1]));// 輪郭の構成Pointを収集
							Imgproc.circle(processedImage,
									new Point(contoursList.get(i).get(i1, 0)[0], contoursList.get(i).get(i1, 0)[1]), 2,
									new Scalar(255, 255, 255), -1);// 色情報取得点可視化

						}
					}

				}
			}
		}
		if (dorawOutLineList.size() == 3) {// 認識マーカが三個見つかってるか
			Point[] markCollerPoint = new Point[3];
			for (int i = 0; i < 3; ++i) {
				markCollerPoint[i] = getCenterPoint(pointsList.get(i * 4), pointsList.get(i * 4 + 1),
						pointsList.get(i * 4 + 2), pointsList.get(i * 4 + 3));
			}

			RotatedRect markerOutLine = Imgproc
					.minAreaRect(new MatOfPoint2f((Point[]) pointsList.toArray(new Point[] {})));
			Point[] markerOutLinePoint = new Point[4];// 最小の矩形の頂点座標を保持
			markerOutLine.points(markerOutLinePoint);// 最小の矩形の輪郭データから頂点座標をmarkerOutLinePointに渡す

			for (int i = 0; i < 4; ++i) {// 最終的なマーカを描画
				Imgproc.line(processedImage, markerOutLinePoint[i], markerOutLinePoint[i < 3 ? i + 1 : 0],
						new Scalar(100, 100, 200), 2);
			}
			transformMarker(markerOutLinePoint, hsvImage, markerImage,
					getTransformKey(markerOutLinePoint, markCollerPoint, webcamImage.rows(), webcamImage.cols()));
			colorDecorde(markerImage, 0, 0, markerImage.height(), markerImage.width(), 1);
		}
		Imgproc.drawContours(processedImage, dorawOutLineList, -1, new Scalar(254, 0, 0), 1);// 輪郭画像にマーカ輪郭を表示
		contoursList.clear();// 輪郭リストをクリア
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(webcamImage);// 描画のためmat型からbufferedImage型に変換
		processedImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		processedImagePanel.repaint();// パネルを再描画
		BufferedImage bufferedImageTemp1 = imageDrawing.matToBufferedImage(markerImage);// 描画のためmat型からbufferedImage型に変換
		hsvImagePanel.setimage(bufferedImageTemp1);// 変換した画像をPanelに追加
		hsvImagePanel.repaint();// パネルを再描画

	}
}