/**
 *
 * @author Ogi
 *
 */
public final class Constants {
	public static final int HEXADECIMAL_NOTATION=16;
	// 色の種類
	public static final int COLLAR_VARIATION = 8;
	// ボーダーの太さ
	public static final int THICKNESS = -5;
	// マーカーの横の余白
	public static final int ROW_MARGIN = 35;
	// マーカーの縦の余白
	public static final int COL_MARGIN = 55;
	// webカメラの認識失敗の確認ループ回数
	public static final int NO_CAPTURED_LEVEL = 10;
	// 個数の最大値（一辺の個数のリミット）
	public static final int BLOCL＿TOP_LMIT = 33;
	// 個数の最大値（一辺の個数のリミット）
	public static final int BLOCL＿UNDER_LMIT = 3;
	// Byte配列要素一個(32bit：１６進数二つ)につきカラーコード４つで表現しているため容量はByte配列の総要素数×4となっている
	public static final int BLOCK_OF_BYTE = 4;
	//一つの１６進数、要素一個につき2個のカラーコードを表現
	public static final int COLORENCODE_LOOP = 2;
	//一つの１６進数、要素一個の上位4ビット
	public static final int COLORENCORD_BITS_FIRST_HALF = 0;
	//一つの１６進数、要素一個の下位4ビット
	public static final int COLORENCORD_BITS_SECOND_HALF = 2;
	//余ったカラーコードのスペースの表現色の選択
	public static final int COLORENCODE_SPACE = 13;
	//フォーマットの選択
	public static final String FORMAT = ("png");
	//入出力のファイルパス(出力は利用やたしかめ現時点では内部で、配列で扱っている)
	public static final String IN_IMG_PATH = ("C:/Eclipse/pleiades/workspace/CollarCordSystem/testpic.png");
	public static final String OUT_IMG_PATH = ("C:/Eclipse/pleiades/workspace/CollarCordSystem/binarytest.txt");

	private Constants() {
	}
}
