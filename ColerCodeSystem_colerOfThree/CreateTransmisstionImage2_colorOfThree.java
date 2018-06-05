import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @see VisibleLightReceiver2_colorOfThree
 * @author Ogi
 * @version 1.0
 */
public class CreateTransmisstionImage2_colorOfThree extends Thread {

	private JFrame transmisstionImageFrame;
	private BufferedImage readImage;
	private Mat[] markerImage;
	// private Mat markerImage2;
	// private Mat markerImage3;
	// private Mat markerImage4;
	// private Mat markerImage5;
	private int dimensionSetSize_Rows;
	private int dimensionSetSize_cols;
	private int codeNo;
	private File imgFileIn;
	private File imgFileOut;
	private String format = (Constants_colorOfThree.FORMAT);
	private String inputFileName = (Constants_colorOfThree.IN_IMG_PATH);
	private String outputFileName = (Constants_colorOfThree.OUT_IMG_PATH);
	private ImageDrawing_colorOfThree imageDrawing;
	private ImageDrawing_colorOfThree transmisstionImagePanel;
	private List<String> transmissionList;
	private HashMap<Integer, String> colorPatternMap;
	private byte[] outImgBytes;// 画像のバイナリデータ
	private int division; /*
							 * division:ブロックの個数をデータの大きさに基づいて設定(colorEncodeSize(
							 * byte[] outBytes))している
							 * 現在のところ設定できるのが、ミス率なども考慮しdivision = 3～33 ほどである)
							 */
	private boolean codeMakeCheck;
	private boolean key = true;

	public CreateTransmisstionImage2_colorOfThree() {
		// Opencvの利用のため
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// 加工画像用ウィンドウフレーム,設定
		transmisstionImageFrame = new JFrame("送信画像ver2");
		transmisstionImageFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		// 各種画像を載せるパネル
		transmisstionImagePanel = new ImageDrawing_colorOfThree();
		transmisstionImageFrame.setContentPane(transmisstionImagePanel);
		imgFileIn = new File(inputFileName);
		imgFileOut = new File(outputFileName);
		imageDrawing = new ImageDrawing_colorOfThree();
		outImgBytes = new byte[fileChecker(imgFileIn)];

		codeMakeCheck = true;
		// ファイルの有無をチェック
		if (!imgFileIn.exists()) {
			System.out.println("入力される画像ファイルは存在しません");
		}
		if (!imgFileOut.exists()) {
			System.out.println("出力ファイルは存在しません");
		} else if (imgFileIn.exists()) {
			System.out.println("出力ファイル、入力ファイルが共に確認出来ました");
			System.out.println("入力データの指定されているファイルの長さは" + outImgBytes.length + "B");
		}
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
		// ブロックでのカラーパターンマップを記憶
		colorPatternMap = new HashMap<Integer, String>();
		Pattern(colorPatternMap);
		// コードサイズ設定
		colorEncodeSize(outImgBytes);
		// 四隅のマーカ導入
		markerImage = new Mat[5];
		codeNo = 0;
		for (int i = 0; i <= 4; i++) {
			markerImage[i] = Imgcodecs.imread(Constants_colorOfThree.MARK2);
			dimensionSetSize_Rows = (markerImage[i].rows() + Constants_colorOfThree.ROW_MARGIN);
			dimensionSetSize_cols = (markerImage[i].cols() + Constants_colorOfThree.COL_MARGIN);
		}
		transmisstionImageFrame.setSize(new Dimension(dimensionSetSize_Rows, dimensionSetSize_cols));
		if ((division = colorEncodeSize(outImgBytes)) != 0) {
			// System.out.println("今回のブロックの数は" + (outImgBytes.length *
			// Constants_colorOfThree.BLOCK_OF_BYTE) + "個ですので"
			// + division + "×" + division + "のカラーコードのサイズに設定します");
			System.out.println("今回のブロックの数は" + (outImgBytes.length * Constants_colorOfThree.BLOCK_OF_BYTE) + "個ですので"
					+ "今回は固定で行うため11×11のコードを5枚でサイズを固定しております。");
		} else {
			System.out.println("サイズ設定でのエラーが発生したため、サイズの設定をキャンセルしました");
		}
		// System.out.println(outImgBytes.length);
		// System.out.println((Integer.toBinaryString(outImgBytes[0] & 0xff)));
		// System.out.println((Integer.parseInt(Integer.toHexString((outImgBytes[0]
		// & 0xff)))));
	}

	/**
	 * カラーパターンの色の順序によって数値を振り分け設置
	 *
	 * @palam colorPatternMap2
	 */
	private void Pattern(HashMap<Integer, String> colorPatternMap2) {
		for (int i = 0; i <= Constants_colorOfThree.COLOR_PATTERN; i++) {
			switch (i) {
			case 0:
				colorPatternMap2.put(i, "赤緑");
				break;
			case 1:
				colorPatternMap2.put(i, "赤赤");
				break;
			case 2:
				colorPatternMap2.put(i, "赤青");
				break;
			case 3:
				colorPatternMap2.put(i, "赤白");
				break;
			case 4:
				colorPatternMap2.put(i, "青赤");
				break;
			case 5:
				colorPatternMap2.put(i, "青青");
				break;
			case 6:
				colorPatternMap2.put(i, "青緑");
				break;
			case 7:
				colorPatternMap2.put(i, "青白");
				break;
			case 8:
				colorPatternMap2.put(i, "緑赤");
				break;
			case 9:
				colorPatternMap2.put(i, "緑青");
				break;
			case 10:
				colorPatternMap2.put(i, "緑緑");
				break;
			case 11:
				colorPatternMap2.put(i, "緑白");
				break;
			case 12:
				colorPatternMap2.put(i, "白赤");
				break;
			case 13:
				colorPatternMap2.put(i, "白青");
				break;
			case 14:
				colorPatternMap2.put(i, "白緑");
				break;
			case 15:
				colorPatternMap2.put(i, "白白");
				break;
			case 16:
				colorPatternMap2.put(i, "ss");
				break;
			}
		}
	}

	/**
	 * クラスの並列動作に利用<br>
	 *
	 * @see CreateTransmisstionImage2_colorOfThree#startRunning()
	 * @see CreateTransmisstionImage2_colorOfThree#stopRunning()
	 */
	public void run() {
		while (key) {
			createImageLoop();
		}

	}

	/**
	 * 外部クラスから呼び出しクラスを実行する
	 *
	 * @see CreateTransmisstionImage2_colorOfThree#run()
	 * @see CreateTransmisstionImage2_colorOfThree#stopRunning()
	 */
	public void startRunning() {
		transmisstionImageFrame.setVisible(true);
		new Thread(this).start();
	}

	/**
	 * 外部クラスから呼び出しクラス中断する
	 *
	 * @see CreateTransmisstionImage2_colorOfThree#run()
	 * @see CreateTransmisstionImage2_colorOfThree#startRunning()
	 */
	public void stopRunning() {
		key = false;
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

	/**
	 * コードの一辺の個数を返す ※今回、研究としてあらかじめdivisionも引数として受信側に渡すようにしている
	 * 実用段階では受信側ではコード情報から読み取り設定を行わなくてはならない
	 *
	 * @return division
	 */
	public int getDivision() {
		return division;
	}

	/**
	 * 画像データにおけるカラーコードのサイズの調整を行っている(今回は11×11にサイズ固定奥ゆきとしてコードの枚数を増やすようにする為、11×
	 * 11に固定する)
	 *
	 * @param outBytes
	 * @return division
	 */
	private int colorEncodeSize(byte[] outBytes) {
		int division = 11;
		// int blocQuantity;
		// int bytesLength;
		// codeSize: {
		// for (int i = Constants_colorOfThree.BLOCL＿UNDER_LMIT; i <=
		// Constants_colorOfThree.BLOCL＿TOP_LMIT; i++) {
		// blocQuantity = (i * i);
		// bytesLength = outBytes.length * Constants_colorOfThree.BLOCK_OF_BYTE;
		// if (bytesLength < blocQuantity) {
		// division = i;
		// break codeSize;
		// } else if (i > Constants_colorOfThree.BLOCL＿TOP_LMIT) {
		// System.out.println("画像の総容量が大きすぎます\n実験段階のため容量の小さいものにしてください");
		// division = 0;
		// break codeSize;
		// }
		// }
		// }
		return division;
	}

	/**
	 *
	 *
	 * @param outBytes
	 */
	private int[] colorEncodePatternVer2(int byteIndex) {
		int colorEncodeOfBloc[] = new int[2];
		// ここの二つの変数により1つのバイナリから16進数の二つを抜出し
		int firstHalfFourBits = (Integer.parseInt(Integer.toHexString((outImgBytes[byteIndex] & 0xf0) >> 4),
				Constants_colorOfThree.HEXADECIMAL_NOTATION));
		int secondHalfFourBits = (Integer.parseInt(Integer.toHexString((outImgBytes[byteIndex] & 0x0f)),
				Constants_colorOfThree.HEXADECIMAL_NOTATION));
		colorEncodeOfBloc[0] = firstHalfFourBits;
		colorEncodeOfBloc[1] = secondHalfFourBits;
		return colorEncodeOfBloc;
	}

	/**
	 * Blocの色の設定、Blocを生成 画像データおいての色決め 赤、青、緑、白、ｓ＝黒の五色を用いて並びによってパターンを決める。(今回の手法)
	 *
	 * @param srcImage
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param division
	 */
	private void colorEncode(Mat[] srcImage, double startX, double startY, double endX, double endY, int division) {
		int count = 0, loopCount = 0, falfFourBits = 0, byteIndexCounter = 0, mapOfPattern = 0,
				colorEncodeOfBloc[] = null;
		Scalar paintColorBGR = null;
		double oneThirdWidth = (endX - startX) / division;
		double oneThirdHeight = (endY - startY) / division;
		boolean code0 = false;
		boolean code1 = false;
		boolean code2 = false;
		boolean code3 = false;
		boolean code4 = false;
		boolean codeend = false;
		byte codeNumber = 0;
		while (codeend == false) {
			for (int i = 0; i < division; i++) {
				for (int j = 0; j < division; j++) {
					if (i == 0 && j == 0 || i == 0 && j == division - 1 || i == division - 1 && j == 0) {
						// 左上から右上、右下である3隅のマーカー部分
					} else if (i == division - 1 && j == division - 1) {
						// 最後の右下のマーカー部分
						if (codeNumber == 0) {
							code0 = true;
						} else if (codeNumber == 1) {
							code1 = true;
						} else if (codeNumber == 2) {
							code2 = true;
						} else if (codeNumber == 3) {
							code3 = true;
						} else if (codeNumber == 4) {
							code4 = true;
							codeend = true;
						} else {
						}
						codeNumber++;
					} else if (i == 0 && j == 1 || i == 0 && j == 2) {
						// コード情報の埋め込み部分(現在のところはマーカー部分を除いた左上から、コードのNo・・・等々
						// 現行のシステムはブロック二つ分を用いて4×4 計16枚のブロックを判断可能としている)
						int x1 = (int) (startX + (j * oneThirdWidth));
						int y1 = (int) (startY + (i * oneThirdHeight));
						int x2 = (int) (startX + ((j + 1) * oneThirdWidth));
						int y2 = (int) (startY + ((i + 1) * oneThirdHeight));
						if (code0 == false) {
							if (j == 1) {
								transmissionList.add("1");// 赤
								paintColorBGR = new Scalar(0, 0, 255);
							} else if (j == 2) {
								transmissionList.add("1");// 赤
								paintColorBGR = new Scalar(0, 0, 255);
							}
							Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
									Constants_colorOfThree.THICKNESS);
						} else if (code0 && code1 == false) {
							if (j == 1) {
								transmissionList.add("2");// 緑
								paintColorBGR = new Scalar(0, 255, 0);
							} else if (j == 2) {
								transmissionList.add("2");// 緑
								paintColorBGR = new Scalar(0, 255, 0);
							}
							Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
									Constants_colorOfThree.THICKNESS);
						} else if (code0 && code1 && code2 == false) {
							if (j == 1) {
								transmissionList.add("3");// 青
								paintColorBGR = new Scalar(255, 0, 0);
							} else if (j == 2) {
								transmissionList.add("3");// 青
								paintColorBGR = new Scalar(255, 0, 0);
							}
							Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
									Constants_colorOfThree.THICKNESS);
						} else if (code0 && code1 && code2 && code3 == false) {
							if (j == 1) {
								transmissionList.add("no");// 白
								paintColorBGR = new Scalar(255, 255, 255);
							} else if (j == 2) {
								transmissionList.add("no");// 白
								paintColorBGR = new Scalar(255, 255, 255);
							}
							Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
									Constants_colorOfThree.THICKNESS);
						} else if (code0 && code1 && code2 && code3) {
							if (j == 1) {
								transmissionList.add("1");// 赤
								paintColorBGR = new Scalar(0, 0, 255);
							} else if (j == 2) {
								transmissionList.add("2");// 緑
								paintColorBGR = new Scalar(0, 255, 0);
							}
							Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
									Constants_colorOfThree.THICKNESS);
						} else {
						}
					} else {
						int x1 = (int) (startX + (j * oneThirdWidth));
						int y1 = (int) (startY + (i * oneThirdHeight));
						int x2 = (int) (startX + ((j + 1) * oneThirdWidth));
						int y2 = (int) (startY + ((i + 1) * oneThirdHeight));
						if (count >= 4) {
							count = 0;
							byteIndexCounter++;
						}
						if (falfFourBits >= Constants_colorOfThree.COLORENCODE_LOOP) {
							falfFourBits = 0;

						}

						if (loopCount < outImgBytes.length * Constants_colorOfThree.BLOCK_OF_BYTE && count == 0) {
							// ここで一つのバイト配列の値から４つのブロックを決定
							colorEncodeOfBloc = colorEncodePatternVer2(byteIndexCounter);
						} else
							if (loopCount < outImgBytes.length * Constants_colorOfThree.BLOCK_OF_BYTE && count != 0) {
						} else {
							for (int k = 0; k < colorEncodeOfBloc.length; k++) {
								colorEncodeOfBloc[k] = Constants_colorOfThree.COLORENCODE_SPACE;
							}
						}
						if (count == 0 || count == 2) {
							mapOfPattern = colorEncodeOfBloc[falfFourBits];
							falfFourBits++;
						}
						// System.out.println(mapOfPattern);
						if (colorPatternMap.get(mapOfPattern) == null) {

						} else
							switch (colorPatternMap.get(mapOfPattern)) {
							case "赤緑":
								if (count == 0 || count == 2) {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								} else {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								}
								break;
							case "赤赤":
								if (count == 0 || count == 2) {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								} else {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								}
								break;
							case "赤青":
								if (count == 0 || count == 2) {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								} else {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								}
								break;
							case "赤白":
								if (count == 0 || count == 2) {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								} else {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								}
								break;
							case "青赤":
								if (count == 0 || count == 2) {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								} else {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								}
								break;
							case "青青":
								if (count == 0 || count == 2) {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								} else {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								}
								break;
							case "青緑":
								if (count == 0 || count == 2) {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								} else {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								}
								break;
							case "青白":
								if (count == 0 || count == 2) {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								} else {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								}
								break;
							case "緑赤":
								if (count == 0 || count == 2) {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								} else {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								}
								break;
							case "緑青":
								if (count == 0 || count == 2) {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								} else {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								}
								break;
							case "緑緑":
								if (count == 0 || count == 2) {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								} else {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								}
								break;
							case "緑白":
								if (count == 0 || count == 2) {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								} else {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								}
								break;
							case "白赤":
								if (count == 0 || count == 2) {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								} else {
									transmissionList.add("1");// 赤
									paintColorBGR = new Scalar(0, 0, 255);
								}
								break;
							case "白青":
								if (count == 0 || count == 2) {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								} else {
									transmissionList.add("3");// 青
									paintColorBGR = new Scalar(255, 0, 0);
								}
								break;
							case "白緑":
								if (count == 0 || count == 2) {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								} else {
									transmissionList.add("2");// 緑
									paintColorBGR = new Scalar(0, 255, 0);
								}
								break;
							case "白白":
								if (count == 0 || count == 2) {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								} else {
									transmissionList.add("no");// 白
									paintColorBGR = new Scalar(255, 255, 255);// (B,G,R)
								}
								break;
							case "ss":
								transmissionList.add("space");// 黒
								paintColorBGR = new Scalar(0, 0, 0);
								break;
							}
						Imgproc.rectangle(srcImage[codeNumber], new Point(x1, y1), new Point(x2, y2), paintColorBGR,
								Constants_colorOfThree.THICKNESS);
						// BufferedImage bufferedImageTemp =
						// transmisstionImagePanel.matToBufferedImage(srcImage);
						// transmisstionImagePanel.setimage(bufferedImageTemp);//
						// 変換した画像をPanelに追加
						count++;
						loopCount++;
					}
				}
				// transmisstionImagePanel.repaint();// パネルを再描画
			}
			if (code0 == true && code1 == true && code2 == true && code3 == true && code4 == true) {
				System.out.print("コードcomplete");
			}
		}

	}

	/**
	 * カラー・コード生成処理をループ
	 */
	private void createImageLoop() {
		if (codeMakeCheck) {
			colorEncode(markerImage, 42, 42, 457, 457, division);
			codeMakeCheck = false;
		}
		if (codeNo > 4) {
			codeNo = 0;
		}
		BufferedImage bufferedImageTemp = imageDrawing.matToBufferedImage(markerImage[codeNo]);
		transmisstionImagePanel.setimage(bufferedImageTemp);// 変換した画像をPanelに追加
		transmisstionImageFrame.repaint();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		// System.out.println(transmissionList.size());
		codeNo++;
	}

}
