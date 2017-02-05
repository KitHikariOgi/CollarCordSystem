/**
 *
 * @author Ogi
 *
 */
public final class Constants {
	/*CreateTransmisstionImage2クラスに使用
	******************************************************************************************************/
	//16進数
	public static final byte HEXADECIMAL_NOTATION = 16;
	// 色の種類
	public static final byte COLLAR_VARIATION = 8;
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
	// Byte配列要素一個(32bit：１６進数二つ)につきカラーコード４つで表現しているため容量はByte配列の総要素数×4となっている
	public static final byte BLOCK_OF_BYTE = 4;
	// 一つの１６進数、要素一個につき2個のカラーコードを表現
	public static final byte COLORENCODE_LOOP = 2;
	// 一つの１６進数、要素一個の上位4ビット
	public static final byte COLORENCORD_BITS_FIRST_HALF = 0;
	// 一つの１６進数、要素一個の下位4ビット
	public static final byte COLORENCORD_BITS_SECOND_HALF = 2;
	// 余ったカラーコードのスペースの表現色の選択
	public static final byte COLORENCODE_SPACE = 13;
	// フォーマットの選択
	public static final String FORMAT = ("png");
	// 入出力のファイルパス(出力は利用やたしかめで利用)
	public static final String IN_IMG_PATH = ("C:/Eclipse/pleiades/workspace/CollarCordSystem/imgData/testpic.png");
	public static final String OUT_IMG_PATH = ("C:/Eclipse/pleiades/workspace/CollarCordSystem/textData/binarytest.txt");
	public static final String MARK2 = ("C:/Eclipse/pleiades/workspace/CollarCordSystem/imgData/mark2.jpg");
	
	
	/*VisibleLightReceiver2クラスに使用
	******************************************************************************************************/
	//辺の数
	public static final byte SIDE_OF_THE_RECTANGLE = 4;
	//HSV各チャンネル格納
	public static final byte HSV_CH = 3;
	public static final byte BLOC_COLLAR_OF_RED=1;
	public static final byte BLOC_COLLAR_OF_BULE=2;
	public static final byte BLOC_COLLAR_OF_GREEN=3;
	public static final byte BLOC_COLLAR_OF_YELLOW=4;
	public static final byte BLOC_COLLAR_OF_CYAN=5;
	public static final byte BLOC_COLLAR_OF_MAGENTA=6;
	public static final byte BLOC_COLLAR_OF_ORANGE=7;
	public static final byte BLOC_COLLAR_OF_PURPLE=8;
	public static final byte BLOC_COLLAR_OF_WHITE=0;

	private Constants() {
	}
}
