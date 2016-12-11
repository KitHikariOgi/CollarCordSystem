import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
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

	private JFrame transmisstionImageFrame;
	private BufferedImage readImage;

	private File imgFileIn;
	private File imgFileOut;

	private String format = (Constants.FORMAT);
	private String inputFileName = (Constants.IN_IMG_PATH);
	private String outputFileName = (Constants.OUT_IMG_PATH);

	private ImageDrawing imageDrawing;
	private ImageDrawing transmisstionImagePanel;

	private List<String> transmissionList;

	private byte[] imgBytes;
	private byte[] outimgBytes;
	/*
	 * division:ブロックの個数をデータの大きさに基づいて設定(colorEncodeSize(byte[] outBytes))している
	 * 現在のところ設定できるのが、ミス率ミス率なども考慮しdivision = 3～33 ほどである)
	*/
	private int division;

	public CreateTransmisstionImage2() {
		// Opencvの利用のため
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		// 加工画像用ウィンドウフレーム,設定
		transmisstionImageFrame = new JFrame("送信画像ver2");
		transmisstionImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		imgFileIn = new File(inputFileName);
		imgFileOut = new File(outputFileName);
		if (imgFileIn.exists() && imgFileOut.exists()) {
			System.out.println("入力される画像ファイル、出力ファイル共に存在します");
		} else {
			System.out.println("出力ファイルまたは入力ファイルが確認出来ません");
		}
		
		imageDrawing = new ImageDrawing();
		outimgBytes = new byte[fileChecker(imgFileIn)];
		imgBytes = new byte[fileChecker(imgFileIn)];
		division=colorEncodeSize(outimgBytes);
		// 各種画像を載せるパネル
		transmisstionImagePanel = new ImageDrawing();
		transmisstionImageFrame.setContentPane(transmisstionImagePanel);
		// エンコードを行う情報リスト
		transmissionList = new ArrayList<String>();

		/*//*/////////////////////////////////////////////////////////////////////////
		try {
			readImage = ImageIO.read(imgFileIn);
		} catch (Exception e) {
			readImage = null;
			e.printStackTrace();
		}
		try {
			outimgBytes = getBytesFromImage(readImage, format);
		} catch (Exception e) {
			outimgBytes = null;
			e.printStackTrace();
		}
		for (byte b : outimgBytes) {
			System.out.println(Integer.toHexString((b & 0xff)));// &0xf0
		}
		for (int a = 0; a < 9; a++) {
			// System.out.println(Integer.toBinaryString(outimgBytes[a]));
			// System.out.println(outimgBytes[a]);

		}
		System.out.println(outimgBytes.length);// &0xf0

		/*//*/////////////////////////////////////////////////////////////////////////
		// try {
		// RandomAccessFile raf = new RandomAccessFile(imgFileIn, "r");
		// // 入力ストリームの生成
		// BufferedInputStream bis = new BufferedInputStream(new
		// FileInputStream(imgFileIn));
		// // 出力ストリームの生成
		// BufferedOutputStream bos = new BufferedOutputStream(new
		// FileOutputStream(imgFileOut));
		// /*
		// * とりあえず今はあらかじめあるテキストファイルの内容の削除は行わないこと
		// */
		// try {
		// raf.readFully(imgBytes);
		//
		// // ファイルへの読み書き
		// int len = 0;
		// while ((len = bis.read(imgBytes)) != -1) {
		// bos.write(imgBytes, 0, len);
		// }
		//
		// // 後始末
		// bos.flush();
		// bos.close();
		// bis.close();
		// } finally {
		// raf.close();
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		System.out.println("入力データの指定されているファイルの長さは" + imgBytes.length + "B");
		System.out.println("入力データの指定されているファイルの長さは" + imgBytes.length + "B");
		System.out.println("この入力画像データの指定されるパーティションのバイト数は" + imgFileIn.getTotalSpace() + "B");
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
		// 明示的にByte[]imgBytesを0に初期化
		Arrays.fill(imgBytes, (byte) 0);
		transmisstionImageFrame.setVisible(false);
	}

	/**
	 * intの限界値を上限値に制限
	 *
	 * @param imgFileイメージファイル
	 */
	private int fileChecker(File imgFile) {

		int byteSize;
		final long fileSize = imgFile.length();
		// TODO ファイルサイズの上限チェック
		try {
			byteSize = (int) fileSize;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("容量オーバー：データの容量が大きすぎます");
			return 0;
		}
		return byteSize;
	}

	/**
	 * イメージ→バイト列に変換
	 *
	 * @param img
	 *            イメージデータ
	 * @param format
	 *            フォーマット名
	 * @return バイト列
	 */
	public static byte[] getBytesFromImage(BufferedImage img, String format) throws IOException {

		if (format == null) {
			format = "png";
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(img, format, baos);
		return baos.toByteArray();
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
	 * 画像データにおいてのカラーコードの色決め
	 * （二つのブロックでバイナリの16進数一つを表している：ランダムで一方の色を決め、それに応じて二つめのブロックで データを表現している）
	 *
	 * @param outBytes
	 */
	private void colorEncodeCheck(byte[] outBytes) {

	}

	/**
	 * 画像データにおけるサイズの調整を行っている
	 *
	 * @param outBytes
	 */
	private int colorEncodeSize(byte[] outBytes) {
		for (int i = 0; i < Constants.BLOCL＿IMIT; i++) {
			int blocQuantity = i * i;
			if (outBytes.length > (blocQuantity)) {
				return division;
			}
		}
		return division;
		
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
					int randmNumber = (int) (Math.random() * Constants.COLLAR_VARIATION) + 1;// 現在ではここで色の種類設定
					if (counttest > 14) {
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
						paintColorBGR = new Scalar(0, 0, 255);
						break;
					case 7:
						transmissionList.add("30");
						paintColorBGR = new Scalar(255, 255, 255);
						break;
					case 8:
						transmissionList.add("90");
						paintColorBGR = new Scalar(0, 0, 0);
						break;
					case 9:
						transmissionList.add("150");
						paintColorBGR = new Scalar(0, 255, 127);
						break;
					case 10:
						transmissionList.add("210");
						paintColorBGR = new Scalar(127, 255, 0);
						break;
					case 11:
						transmissionList.add("270");
						paintColorBGR = new Scalar(255, 0, 127);
						break;
					case 12:
						transmissionList.add("330");
						paintColorBGR = new Scalar(127, 0, 255);
						break;
					case 13:
						transmissionList.add("370");
						paintColorBGR = new Scalar(255, 255, 255);
						break;
					case 14:
						transmissionList.add("380");
						paintColorBGR = new Scalar(0, 0, 0);
						break;
					case 15:// 枠外エラー
						System.out.println("colorEncodeエラー");
						return;
					}
					Imgproc.rectangle(srcImage, new Point(x1, y1), new Point(x2, y2), paintColorBGR,
							Constants.THICKNESS);
				}
			}
		}
	}

	/**
	 * カラー・コード生成処理をループ
	 */
	private void createImageLoop() {
		Mat markerImage = Imgcodecs.imread("mark2.jpg");
		transmisstionImageFrame.setSize(
				new Dimension(markerImage.rows() + Constants.ROW_MARGIN, markerImage.cols() + Constants.COL_MARGIN));
		colorEncode(markerImage, 42, 42, markerImage.height() - 43, markerImage.width() - 43, division);
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(markerImage);
		transmisstionImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		transmisstionImagePanel.repaint();// パネルを再描画
	}

}
