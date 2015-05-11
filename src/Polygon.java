import java.awt.Rectangle;

/**
 *
 * @author yuantian (Student ID: 300228072)
 *
 * This class is for create a triangular polygon
 *
 */

public class Polygon {
	public Vector3D[] vertices = new Vector3D[3];
	public Vector3D v1, v2, v3;

	public boolean hidden = false;
	public Rectangle boundingBox;
	public int minX, minY, maxX, maxY;

	public Vector3D normal;

	public int R;
	public int G;
	public int B;

	public Polygon(Vector3D v1, Vector3D v2, Vector3D v3, int r, int g, int b) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		vertices[0] = v1;
		vertices[1] = v2;
		vertices[2] = v3;
		this.R = r;
		this.G = g;
		this.B = b;
		calculateNormal();
		setBoundingBox();
	}

	//rectangle bounding box
	private void setBoundingBox() {
		minX = Math.round(Math.min(Math.min(vertices[0].x, vertices[1].x), vertices[2].x));
		maxX = Math.round(Math.max(Math.max(vertices[0].x, vertices[1].x), vertices[2].x));
		minY = Math.round(Math.min(Math.min(vertices[0].y, vertices[1].y), vertices[2].y));
		maxY = Math.round(Math.max(Math.max(vertices[0].y, vertices[1].y), vertices[2].y));

		boundingBox = new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
	}

	public boolean isHidden() {
		if(normal.z > 0) {
			hidden = true;
		}
		else {
			hidden = false;
		}
		return hidden;
	}

	public void calculateNormal() {
		normal = (v2.minus(v1)).crossProduct(v3.minus(v2)).unitVector();
		//System.out.println("Normal: " + normal);
	}

	public Vector3D getNormal(){
		return normal;
	}

	public void hide(){
		hidden = true;
	}
}
