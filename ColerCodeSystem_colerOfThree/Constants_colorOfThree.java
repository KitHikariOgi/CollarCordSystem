/**
 *
 * @author Ogi
 *
 */
public final class Constants_colorOfThree {
	/*
	 * CreateTransmisstionImage2クラスに使用
	 ******************************************************************************************************/
	// 16進数
	public static final byte HEXADECIMAL_NOTATION = 16;
	// 色の種類
	public static final byte COLOR_VARIATION = 8;
	// ボーダーの太さ
	public static final byte THICKNESS = -5;
	// マーカーの横の余白
	public static final byte ROW_MARGIN = 35;
	// マーカーの縦の余白
	public static final byte COL_MARGIN = 55;
	// webカメラの認識失敗の確認ループ回数
	public static final byte NO_CAPTURED_LEVEL = 10;
	// 個数の最大値（一辺の個数のリミット）
	public static final byte BLOCL＿TOP_LMIT = 33;
	// 個数の最大値（一辺の個数のリミット）
	public static final byte BLOCL＿UNDER_LMIT = 3;
	// Byte配列要素一個(32bit：１６進数二つ)
	public static final byte BLOCK_OF_BYTE = 4;
	// 一つの１６進数、要素一個につき2個のカラーコードを表現
	public static final byte COLORENCODE_LOOP = 2;
	// 一つの１６進数、要素一個の上位4ビット
	public static final byte COLORENCORD_BITS_FIRST_HALF = 0;
	// 一つの１６進数、要素一個の下位4ビット
	public static final byte COLORENCORD_BITS_SECOND_HALF = 2;
	// 余ったカラーコードのスペースの表現色の選択
	public static final byte COLORENCODE_SPACE = 16;
	//表現色のPattern数
	public static final byte COLOR_PATTERN = 16;


	// フォーマットの選択
	public static final String FORMAT = ("png");
	// 入出力のファイルパス(出力は利用やたしかめで利用)
	public static final String IN_IMG_PATH = ("C:/Eclipse/pleiades/workspace/ColorCodeSystem_colorOfThree/imgData/testpic.png");
	public static final String OUT_IMG_PATH = ("C:/Eclipse/pleiades/workspace/ColorCodeSystem_colorOfThree/textData/binarytest.txt");
	public static final String MARK2 = ("C:/Eclipse/pleiades/workspace/ColorCodeSystem_colorOfThree/imgData/mark2.jpg");

	/*
	 * VisibleLightReceiver2クラスに使用
	 ******************************************************************************************************/
	// 辺の数
	public static final byte SIDE_OF_THE_RECTANGLE = 4;
	// 取得点の数
	public static final byte NUMBER＿OF_POINT = 5;
	//マーカーブロック(４隅)
	public static final byte MARKER_BLOCK = 4;
	// 一ブロックの取得点である中心点からのピクセル移動距離
	public static final byte DISTANCE＿OF_POINT = 2;
	// 一つの１６進数、要素一個の上位4ビット
	public static final byte COLORENCORD_BITS_FIRST_HALF_V = 1;
	// 一つの１６進数、要素一個の下位4ビット
	public static final byte COLORENCORD_BITS_SECOND_HALF_V = 3;
	// ４つのブロック(バイトデータ)のうちの1/４～４/４
	public static final byte BLOCK_1_4= 0;
	public static final byte BLOCK_2_4= 1;
	public static final byte BLOCK_3_4= 2;
	public static final byte BLOCK_4_4= 3;
	// HSV各チャンネル格納
	public static final byte HSV_CH = 3;
	public static final byte BLOC_COLOR_OF_RED = 1;
	public static final byte BLOC_COLOR_OF_BULE = 2;
	public static final byte BLOC_COLOR_OF_GREEN = 3;
	public static final byte BLOC_COLOR_OF_YELLOW = 4;
	public static final byte BLOC_COLOR_OF_CYAN = 5;
	public static final byte BLOC_COLOR_OF_MAGENTA = 6;
	public static final byte BLOC_COLOR_OF_ORANGE = 7;
	public static final byte BLOC_COLOR_OF_PURPLE = 8;
	public static final byte BLOC_COLOR_OF_WHITE = 0;

	private Constants_colorOfThree() {
	}
}
