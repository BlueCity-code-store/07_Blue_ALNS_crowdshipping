import java.io.*;
import java.awt.*;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

public class Draw {
	int num_customers;
	int width;
	int height;
	String instance;
	BufferedImage bi;
	Graphics2D ig2;
	int consx; 
	int consy;
	//Color [] col = {Color.BLUE,Color.MAGENTA,Color.cyan,Color.PINK,Color.ORANGE,Color.GRAY, Color.RED, Color.yellow,Color.blue,Color.MAGENTA,Color.cyan,Color.PINK,Color.ORANGE,Color.GRAY, Color.RED, Color.yellow,Color.blue,};
	public Draw(String instan, int w, int h, int c)  {
			instance = instan;
			num_customers = Math.min(c, 100);
			width = w; 
			height = h;
			consx = 500;
			consy = 500;
			// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
			// into integer pixels
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			ig2 = bi.createGraphics();
			//Font font = new Font("TimesRoman", Font.BOLD, 20);
			//ig2.setFont(font);
			ig2.setColor(Color.white);
			ig2.fillRect(0, 0, width, height);
			ig2.setColor(Color.black);
		
	}
	public void DrawLine(int x1 , int y1 , int x2, int y2, int type) {		
		this.ig2.setColor(Color.black);
		this.ig2.drawLine(x1 + this.consx, y1 + this.consy, x2 + this.consx, y2 + this.consy);
		
	}
	public void DrawCircle(int x , int y , int r , int c) {
		if(c>= 1) {
			this.ig2.setColor(Color.red);
		}else {this.ig2.setColor(Color.blue);}
		this.ig2.fillOval(x +this.consx - (r/2), y + this.consy - (r/2), r, r);
		this.ig2.setColor(Color.black);
	}
	public void DrawSquare(int x , int y , int r , int c) {
		if(c>= 1) {
			this.ig2.setColor(Color.red);
		}else {this.ig2.setColor(Color.blue);}
		this.ig2.fillRect(x+this.consx, y + this.consy, r, r);
		this.ig2.setColor(Color.black);
	}
	public void DrawTriangle(int x , int y , int r , int c) {
		x = x + this.consx;
		y = y +this.consy;
		int[] xp = {x, x-(r-8), x+(r-8)};
		int[] yp = {y + r, y, y};
		
		if(c>= 1) {
			this.ig2.setColor(Color.red);
		}else {this.ig2.setColor(Color.green);}
		this.ig2.fillPolygon(xp, yp, 3);
		this.ig2.setColor(Color.black);
	}
	public void DrawString(int x , int y ,int r, int c) {
		String s = Integer.toString(c);
		this.ig2.setFont(new Font("TimesRoman", Font.PLAIN, 20)); 
		this.ig2.drawString(s, x + this.consx, y + this.consy + r );
	}
	public void writeimage() {
		try {
			ImageIO.write(this.bi, "PNG", new File("/Users/fabiantorres/Desktop/image_" +this.instance+"_"+Integer.toString(num_customers) + ".PNG"));
		}catch (IOException ie) {
			ie.printStackTrace();
		}
	}
}	
