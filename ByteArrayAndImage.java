import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

	public class ByteArrayAndImage {

	  public static void main(String[] args) throws IOException {
	    BufferedImage image = new BufferedImage(100, 75, BufferedImage.TYPE_INT_ARGB);
	    byte[] b = getImageBytes(image, "testpic.png");
	    System.out.println(b.length);
	  }

	  /**
	   * 画像オブジェクトを指定した画像フォーマットのバイナリ表現に変換
	   * @param image 画像オブジェクト
	   * @param imageFormat 画像フォーマット
	   * @return バイナリ表現
	   */
	  public static byte[] getImageBytes(BufferedImage image, String imageFormat)  throws IOException {
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    BufferedOutputStream os = new BufferedOutputStream(bos);
	    image.flush();
	    ImageIO.write(image, imageFormat, os);
	    os.flush();
	    os.close();
	    return bos.toByteArray();
	  }

	}

