import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
 * @author Ogi
 * @version 1.0
 */
public class CreateTransmisstionImage2 extends Thread {

	private JFrame transmisstionImageFrame;
	private BufferedImage readImage;
	private Mat markerImage;

	private int dimensionSetSize_Rows;
	private int dimensionSetSize_cols;

	private File imgFileIn;
	private File imgFileOut;

	private String format = (Constants.FORMAT);
	private String inputFileName = (Constants.IN_IMG_PATH);
	private String outputFileName = (Constants.OUT_IMG_PATH);

	private ImageDrawing imageDrawing;
	private ImageDrawing transmisstionImagePanel;

	private List<String> transmissionList;

	private byte[] outImgBytes;// 画像のバイナリデータ

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
		// 各種画像を載せるパネル
		transmisstionImagePanel = new ImageDrawing();
		transmisstionImageFrame.setContentPane(transmisstionImagePanel);
		imgFileIn = new File(inputFileName);
		imgFileOut = new File(outputFileName);
		imageDrawing = new ImageDrawing();
		outImgBytes = new byte[fileChecker(imgFileIn)];
		// ファイルの有無をチェック
		if (imgFileIn.exists() && imgFileOut.exists()) {
			System.out.println("入力される画像ファイル、出力ファイル共に存在します");
		} else {
			System.out.println("出力ファイルまたは入力ファイルが確認出来ません");
		}
		System.out.println("入力データの指定されているファイルの長さは" + outImgBytes.length + "B");
		// ファイル読み込み
		try {
			readImage = ImageIO.read(imgFileIn);
			outImgBytes = getBytesFromImage(readImage, format);
		} catch (Exception e) {
			readImage = null;
			outImgBytes = null;
			e.printStackTrace();
		}
		// エンコードを行う情報リスト
		transmissionList = new ArrayList<String>();

		// カラーコードのサイズ設定
		colorEncodeSize(outImgBytes);
		// 四隅のマーカ導入
		markerImage = Imgcodecs.imread(Constants.MARK2);
		dimensionSetSize_Rows = (markerImage.rows() + Constants.ROW_MARGIN);
		dimensionSetSize_cols = (markerImage.cols() + Constants.COL_MARGIN);
		transmisstionImageFrame.setSize(new Dimension(dimensionSetSize_Rows, dimensionSetSize_cols));
		if ((division = colorEncodeSize(outImgBytes)) != 0) {
			System.out.println("今回のブロックの数は" + (outImgBytes.length * Constants.BLOCK_OF_BYTE) + "個なので" + division + "×"
					+ division + "のカラーコードのサイズに設定します");
		} else {
			System.out.println("サイズ設定でのエラーが発生したため、サイズの設定をキャンセルしました");
		}

		// System.out.println((Integer.toBinaryString(outImgBytes[0]&0xff)));
		// System.out.println((byte) 0x89);
		// int g = 0;
		// int co = 0;
		// byte b = 4;
		// byte c = 5;
		// for (int i = 0; i < 4; i++) {
		// if (i != 3) {
		// g += b;
		// } else {
		// g += c;
		// }
		// System.out.println(i + "回目" + g);
		// if (co == 1) {
		// g = (g << 4);
		// }
		// System.out.println(""+(Integer.parseInt(Integer.toHexString(g))));
		// System.out.println(i + "回目" + (byte) g);
		// co++;
		// }

	}

	/**
	 * カラー・コード生成処理をループ
	 */
	private void createImageLoop() {
		colorEncode(markerImage, 42, 42, markerImage.height() - 43, markerImage.width() - 43, division);
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(markerImage);
		transmisstionImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		transmisstionImageFrame.repaint();
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
		// 明示的にByte[]outImgBytesを0に初期化
		// Arrays.fill(outImgBytes, (byte) 0);
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

		// ファイルサイズの上限チェック
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
	public byte[] getBytesFromImage(BufferedImage img, String format) throws IOException {
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

	public int getDivision() {
		return division;
	}

	/**
	 * 画像データにおけるカラーコードのサイズの調整を行っている
	 *
	 * @param outBytes
	 */
	private int colorEncodeSize(byte[] outBytes) {
		int division = 0;
		int blocQuantity;
		int bytesLength;

		codeSize: {
			for (int i = Constants.BLOCL＿UNDER_LMIT; i <= Constants.BLOCL＿TOP_LMIT; i++) {
				blocQuantity = (i * i);
				bytesLength = outBytes.length * Constants.BLOCK_OF_BYTE;
				if (bytesLength < blocQuantity) {
					division = i;
					break codeSize;
				} else if (i > Constants.BLOCL＿TOP_LMIT) {
					System.out.println("画像の総容量が大きすぎます\n実験段階のため容量の小さいものにしてください");
					division = 0;
					break codeSize;
				}
			}
		}
		return division;
	}

	/**
	 * 画像データにおいてのカラーコードの色決め
	 * （二つのブロックでバイナリの16進数一つを表している：ランダムで一方の色を決め、それに応じて二つめのブロックで データを表現している）
	 *
	 * @param outBytes
	 */
	private int[] colorEncodePattern(int byteIndex) {
		int colorEncodeOfBloc[] = new int[Constants.BLOCK_OF_BYTE];
		int firstHalfFourBits = (Integer.parseInt(Integer.toHexString((outImgBytes[byteIndex] & 0xf0) >> 4),
				Constants.HEXADECIMAL_NOTATION));
		int secondHalfFourBits = (Integer.parseInt(Integer.toHexString((outImgBytes[byteIndex] & 0x0f)),
				Constants.HEXADECIMAL_NOTATION));
		int randmColorCordFirstHalfNumber = (int) (Math.random() * (firstHalfFourBits));
		int randmColorCordSecondHalfNumber = (int) (Math.random() * (secondHalfFourBits));

		if (firstHalfFourBits > Constants.COLLAR_VARIATION) {
			int a = firstHalfFourBits - Constants.COLLAR_VARIATION;
			randmColorCordFirstHalfNumber = (int) (Math.random() * ((Constants.COLLAR_VARIATION + 1) - a) + a);
		} else {
		}
		if (secondHalfFourBits > Constants.COLLAR_VARIATION) {
			int b = secondHalfFourBits - Constants.COLLAR_VARIATION;
			randmColorCordSecondHalfNumber = (int) (Math.random() * ((Constants.COLLAR_VARIATION + 1) - b) + b);
		} else {
		}
		for (int i = 0; i <= Constants.COLORENCODE_LOOP; i++) {
			int numberOfArrayElement;
			if (i == Constants.COLORENCORD_BITS_FIRST_HALF) {
				numberOfArrayElement = i;
				colorEncodeOfBloc[i] = randmColorCordFirstHalfNumber;
				colorEncodeOfBloc[++numberOfArrayElement] = (firstHalfFourBits - randmColorCordFirstHalfNumber);
			} else if (i == Constants.COLORENCORD_BITS_SECOND_HALF) {
				numberOfArrayElement = i;
				colorEncodeOfBloc[i] = randmColorCordSecondHalfNumber;
				colorEncodeOfBloc[++numberOfArrayElement] = (secondHalfFourBits - randmColorCordSecondHalfNumber);
			}
		}
		return colorEncodeOfBloc;

	}

	/**
	 * Blocの色の設定、Blocを生成
	 *
	 * @param srcImage
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param division
	 */
	private void colorEncode(Mat srcImage, double startX, double startY, double endX, double endY, int division) {
		int count = 0;
		int loopCount = 0;
		int byteIndexCounter = 0;
		int colorEncodeOfBloc[] = null;
		Scalar paintColorBGR = null;
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;

		for (int i = 0; i < division; i++) {
			for (int j = 0; j < division; j++) {
				if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0
						|| i == division - 1 && j == division - 1) {
				} else {
					int x1 = (int) (startX + (j * oneThirdWidth));
					int y1 = (int) (startY + (i * oneThirdHeight));
					int x2 = (int) (startX + ((j + 1) * oneThirdWidth));
					int y2 = (int) (startY + ((i + 1) * oneThirdHeight));

					if (count >= Constants.BLOCK_OF_BYTE) {
						count = 0;
						byteIndexCounter++;
					}
					if (loopCount < outImgBytes.length * Constants.BLOCK_OF_BYTE && count == 0) {
						// ここで一つのバイト配列の値から４つのブロックを決定
						colorEncodeOfBloc = colorEncodePattern(byteIndexCounter);
					} else if (loopCount < outImgBytes.length * Constants.BLOCK_OF_BYTE && count != 0) {
					} else {
						for (int k = 0; k < colorEncodeOfBloc.length; k++) {
							colorEncodeOfBloc[k] = Constants.COLORENCODE_SPACE;
						}
					}
					switch (colorEncodeOfBloc[count]) {
					case 0:
						transmissionList.add("no");// 白
						paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
						break;
					case 1:
						transmissionList.add("1");// 赤
						paintColorBGR = new Scalar(0, 0, 255);
						break;
					case 2:
						transmissionList.add("2");// 緑
						paintColorBGR = new Scalar(0, 255, 0);
						break;
					case 3:
						transmissionList.add("3");// 青
						paintColorBGR = new Scalar(255, 0, 0);
						break;
					case 4:
						transmissionList.add("4");// 黄色
						paintColorBGR = new Scalar(0, 255, 255);
						break;
					case 5:
						transmissionList.add("5");// シアン
						paintColorBGR = new Scalar(255, 255, 0);
						break;
					case 6:
						transmissionList.add("6");// マゼンタ
						paintColorBGR = new Scalar(255, 50, 255);
						break;
					case 7:
						transmissionList.add("7");// オレンジ
						paintColorBGR = new Scalar(0, 127, 255);
						break;
					case 8:
						transmissionList.add("8");// 紫
						paintColorBGR = new Scalar(255, 0, 190);
						break;
					case 13:
						transmissionList.add("space");// 黒
						paintColorBGR = new Scalar(0, 0, 0);
						break;
					case 14:// 枠外エラー
						System.out.println("colorEncodeエラー");
						return;
					/////////////////////////////////////////////// ここまで使用
					case 9:
						transmissionList.add("90");// 黄緑
						paintColorBGR = new Scalar(0, 255, 127);
						break;
					case 10:
						transmissionList.add("150");// 青緑
						paintColorBGR = new Scalar(127, 255, 0);
						break;
					case 11:
						transmissionList.add("210");//
						paintColorBGR = new Scalar(255, 127, 0);
						break;
					case 12:
						transmissionList.add("330");//
						paintColorBGR = new Scalar(127, 0, 255);
						break;
					}
					Imgproc.rectangle(srcImage, new Point(x1, y1), new Point(x2, y2), paintColorBGR,
							Constants.THICKNESS);
					// System.out.println(transmissionList.get(loopCount));
					count++;
					loopCount++;

				}
			}
		}
	}
}
