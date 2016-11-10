
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.opencv.core.Mat;

/**
 * Mat型の画像をパネル上に描画する
 * 
 * @author iwao
 * @version 1.0
 */
public class ImageDrawing extends JPanel {
	private BufferedImage image;

	// Create a constructor method
	public ImageDrawing() {
		super();
	}

	private BufferedImage getimage() {
		return image;
	}

	public void setimage(BufferedImage newimage) {
		image = newimage;
		return;
	}

	/**
	 * Mat型をBufferedImage型に変換します。 Converts/writes a Mat into a BufferedImage.
	 *
	 * @param matrix
	 *            Mat of type CV_8UC3 or CV_8UC1
	 * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	 */
	public BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;
		matrix.get(0, 0, data);
		switch (matrix.channels()) {
		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			// bgr to rgb
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;
		default:
			return null;
		}
		BufferedImage image2 = new BufferedImage(cols, rows, type);
		image2.getRaster().setDataElements(0, 0, cols, rows, data);
		return image2;
	}

	/**
	 * paintComponentは画像の描画に利用
	 */
	protected void paintComponent(Graphics g) {
		BufferedImage temp = getimage();
		g.drawImage(temp, 10, 10, temp.getWidth(), temp.getHeight(), this);
	}
}
