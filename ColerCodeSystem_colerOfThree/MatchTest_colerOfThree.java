import java.util.List;

/**
 * 送受信の結果をテストする。 マーカver1,ver2ともに利用可能
 *
 * @author Ogi
 * @version 1.0
 */
public class MatchTest_colerOfThree {
	/**
	 * 2つのリストの内容を比較してミス数を表示する。
	 *
	 * @param receiveList
	 *            受信データリスト
	 * @param transmissionList
	 *            送信データリスト
	 */
	public void startTest(List<String> receiveList, List<String> transmissionList) {
		int missCount = 0;
		if (receiveList.isEmpty() || transmissionList.isEmpty()) {
			System.out.println("送受信が行われていません");
		} else if (transmissionList.size() != receiveList.size()) {
			System.out.println("送受信の設定が間違っています");
			System.out.println("受信データ数" + receiveList.size());
			System.out.println("送信データ数" + transmissionList.size());
		} else {
			for (int i = 0; i < transmissionList.size(); i++) {
				if (!transmissionList.get(i).equals(receiveList.get(i))) {
					missCount++;
					System.out.println(i+"つめの送信dataが"+transmissionList.get(i)+"に対して"+"受信されたもの値が"+receiveList.get(i)+"だったため取得ミスです");
				}
			}
			System.out.println(transmissionList.size() + "中 " + missCount + "がミス");
		}
	}
}
