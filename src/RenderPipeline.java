import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author yuantian (Student ID: 300228072)
 *
 * This class is for rendering the polygons
 *
 */

public class RenderPipeline {
	private List<Polygon> allpolys = new ArrayList<Polygon>();
	private List<Polygon> polygons = new ArrayList<Polygon>();

	private Vector3D lightSource;
	private int[] ambientLight;
	private int imageHeight;
	public float minX, maxX, minY, maxY;
	private Vector3D coordinate;

	public RenderPipeline(List<Polygon> polygon, Vector3D lightSource, int[] ambientLight, int imageHeight) {
		this.allpolys = polygon;
		this.lightSource = lightSource.unitVector();
		this.ambientLight = ambientLight;
		this.imageHeight = imageHeight;
		boundingBox();
		initialTransform();
	}

	//render by z-buffer
	public Color[][] zBuffer() {
		for (Polygon p : allpolys) {
			p.calculateNormal();
		}

		hiddenPolygons(); //check hidden polygons

		Color[][] zBufferC = new Color[imageHeight][imageHeight];
		float[][] zBufferD = new float[imageHeight][imageHeight];

		for (int i = 0; i < zBufferC.length - 1; i++) {
			for (int j = 0; j < zBufferC[i].length - 1; j++) {
				zBufferC[i][j] = Color.lightGray;
				zBufferD[i][j] = Float.POSITIVE_INFINITY;
			}
		}

		for (Polygon p : polygons) {
			float[][] el = edgeLists(p);
			Color shade = computeShade(p);

			for (int y = 0; y < el.length - 1; y++) {
				int x = Math.round(el[y][0]);
				float z = el[y][1];

				float mz = (el[y][3]) - (el[y][1]) / (el[y][2]) - (el[y][0]);

				for (; x <= Math.round(el[y][2]); x++, z += mz) {
					if (x >= 0 && x < imageHeight && z < zBufferD[x][y]) {
						zBufferD[x][y] = z;
						zBufferC[x][y] = shade;
					}
				}
			}
		}
		return zBufferC;
	}

	//edgelist
	public float[][] edgeLists(Polygon p) {
		float[][] edgeList = new float[imageHeight][4];

		for (int y = 0; y < edgeList.length - 1; y++) {
			edgeList[y][0] = Float.POSITIVE_INFINITY;
			edgeList[y][1] = Float.POSITIVE_INFINITY;
			edgeList[y][2] = Float.NEGATIVE_INFINITY;
			edgeList[y][3] = Float.POSITIVE_INFINITY;
		}

		for (int i = 0; i < 3; i++) {
			Vector3D v1 = p.vertices[i];
			Vector3D v2 = p.vertices[(i+1) % 3];

			if (v1.y > v2.y) {
				v2 = v1;
				v1 = p.vertices[(i+1) % 3];
			}

			float mx = (v2.x - v1.x) / (v2.y - v1.y);
			float mz = (v2.z - v1.z) / (v2.y - v1.y);
			float x = v1.x;
			float z = v1.z;

			int j = Math.round(v1.y);
			int maxJ = Math.round(v2.y);

			while (j < maxJ) {
				if (x < edgeList[j][0]) {
					edgeList[j][0] = x;
					edgeList[j][1] = z;
				}
				if (x > edgeList[j][2]) {
					edgeList[j][2] = x;
					edgeList[j][3] = z;
				}
				++j;
				x += mx;
				z += mz;
			}

			edgeList[imageHeight-1][0] = v2.x;
			edgeList[imageHeight-1][1] = v2.z;
			edgeList[imageHeight-1][2] = v2.x;
			edgeList[imageHeight-1][3] = v2.z;
		}

		return edgeList;
	}

	//compute shade
	public Color computeShade(Polygon p) {
		float cosTheta = p.normal.dotProduct(lightSource.unitVector());

		float r = ((ambientLight[0]/255f) + (1f) * cosTheta) * (p.R/255f);
		float g = ((ambientLight[1]/255f) + (1f) * cosTheta) * (p.G/255f);
		float b = ((ambientLight[2]/255f) + (1f) * cosTheta) * (p.B/255f);

		if(r < 0) {
			r = 0;
		} else if (r > 1) {
			r = 1;
		}

		if(g < 0) {
			g = 0;
		} else if(g > 1) {
			g = 1;
		}

		if(b < 0) {
			b = 0;
		} else if(b > 1) {
			b = 1;
		}

		return new Color(r, g, b);
	}

	public void initialTransform() {
		Transform t = Transform.identity();
		coordinate = centreVector();

		float scale = 0.7f;
		t = Transform.newScale(scale, scale, scale);
		applyTransform(t);
		boundingBox();

		t = Transform.newTranslation(coordinate.x - maxX/2, coordinate.y - maxY/2 , coordinate.z);
		applyTransform(t);
		boundingBox();
	}

	public void applyTransform(Transform transform) {
		for (Polygon p : allpolys) {
			Vector3D newV1 = transform.multiply(p.v1);
			p.v1 = newV1;
			p.vertices[0] = newV1;

			Vector3D newV2 = transform.multiply(p.v2);
			p.v2 = newV2;
			p.vertices[1] = newV2;

			Vector3D newV3 = transform.multiply(p.v3);
			p.v3 = newV3;
			p.vertices[2] = newV3;
		}
	}

	public void boundingBox() {
		minX = 0f;
		minY = 0f;
		maxX = 0f;
		maxY = 0f;
		for(Polygon p : allpolys){
			for(int i = 0; i < 3; i++) {
				if(p.vertices[i].x > maxX) {
					maxX = p.vertices[i].x;
				}
				if(p.vertices[i].x < minX) {
					minX = p.vertices[i].x;
				}
				if(p.vertices[i].y > maxY) {
					maxY = p.vertices[i].y;
				}
				if(p.vertices[i].y < minY) {
					minY = p.vertices[i].y;
				}
			}
		}
	}

	public Vector3D centreVector() {
		return new Vector3D(imageHeight/2, imageHeight/2, 0f);
	}

	//check hidden polygons
	public void hiddenPolygons() {
		this.polygons = new ArrayList<Polygon>();
		for (Polygon p : allpolys) {
			if (!p.isHidden()) {
				this.polygons.add(p);
			}
		}
	}

	//remove hidden polygons
	public void removeHidden(){
		for (Polygon p : polygons){
		    if (p.getNormal().z >= 0.0){
		    	p.hide();
		    }
		}
	}

	public void updateAmbientLight(int[] ambLight) {
		this.ambientLight = ambLight;
	}

	//rotate left
	public void rotateLeft() {
		Transform t = Transform.newTranslation(-coordinate.x, -coordinate.y, -coordinate.z);
		applyTransform(t);

		t = Transform.newYRotation(0.1f);
		applyTransform(t);

		t = Transform.newTranslation(coordinate);
		applyTransform(t);

		boundingBox();
		System.out.println("Rotate left: " + coordinate.toString());
	}

	//rotate right
	public void rotateRight() {
		Transform t = Transform.newTranslation(-coordinate.x, -coordinate.y, -coordinate.z);
		applyTransform(t);

		t = Transform.newYRotation(-0.1f);
		applyTransform(t);

		t = Transform.newTranslation(coordinate);
		applyTransform(t);

		boundingBox();
		System.out.println("Rotate right: " + coordinate.toString());
	}

	//rotate up
	public void rotateUp() {
		Transform t = Transform.newTranslation(-coordinate.x, -coordinate.y, -coordinate.z);
		applyTransform(t);

		t = Transform.newXRotation(-0.1f);
		applyTransform(t);

		t = Transform.newTranslation(coordinate);
		applyTransform(t);

		boundingBox();
		System.out.println("Rotate up: " + coordinate.toString());
	}

	//rotate down
	public void rotateDown() {
		Transform t = Transform.newTranslation(-coordinate.x, -coordinate.y, -coordinate.z);
		applyTransform(t);

		t = Transform.newXRotation(0.1f);
		applyTransform(t);

		t = Transform.newTranslation(coordinate);
		applyTransform(t);

		boundingBox();
		System.out.println("Rotate down: " + coordinate.toString());
	}

	//zoom out
	public void zoomOut() {
		float scale = 0.9f;

		Transform t = Transform.newScale(scale, scale, scale);
		applyTransform(t);
		boundingBox();
		coordinate = t.multiply(coordinate);

		t = Transform.newTranslation(30f, 30f, 0f);
		applyTransform(t);
		boundingBox();
		coordinate = t.multiply(coordinate);
		System.out.println("Zoom Out");
	}

	//zoom in
	public void zoomIn() {
		float scale = 1.1f;

		Transform t = Transform.newScale(scale, scale, scale);
		applyTransform(t);
		boundingBox();
		coordinate = t.multiply(coordinate);

		t = Transform.newTranslation(-30f, -30f, 0f);
		applyTransform(t);
		boundingBox();
		coordinate = t.multiply(coordinate);
		System.out.println("Zoom In");
	}
}
