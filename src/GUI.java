import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author yuantian (Student ID: 300228072)
 *
 * This is GUI class contains basic GUI
 *
 * It based on Tony Butler-Yeoman's "GUI.java"
 */

public class GUI {
	public static final int CANVAS_WIDTH = 600;
	public static final int CANVAS_HEIGHT = 600;

	private JFrame frame;
	//private JTextArea textOutputArea;

	private final JSlider red = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
	private final JSlider green = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
	private final JSlider blue = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);

	private static final Dimension DRAWING_SIZE = new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
	private static final Dimension CONTROLS_SIZE = new Dimension(150, 600);

	private static final Font FONT = new Font("Courier", Font.BOLD, 36);

	private List<Polygon> polygons = new ArrayList<Polygon>();

	private Vector3D lightSource;
	private int[] ambientLight = getAmbientLight();
	//private int imageWidth;
	private int imageHeight = 600;

	//private BufferedImage image;

	private RenderPipeline rp;

	private boolean isInitialised = false;

	public GUI() {
		initialise();
	}

	//build basic GUI
	@SuppressWarnings("serial")
	private void initialise() {
		frame = new JFrame("Render_App developed by yuantian");
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.LINE_AXIS));
		frame.setSize(new Dimension(DRAWING_SIZE.width + CONTROLS_SIZE.width, DRAWING_SIZE.height));
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent drawing = new JComponent() {
			protected void paintComponent(Graphics g) {
				BufferedImage image = render();
				if (image == null) {
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, DRAWING_SIZE.width, DRAWING_SIZE.height);
					g.setColor(Color.BLACK);
					g.setFont(FONT);
					g.drawString("IMAGE IS NULL", 50, DRAWING_SIZE.height - 50);
				} else {
					g.drawImage(image, 0, 0, null);
				}
			}
		};

		//display area
		drawing.setPreferredSize(DRAWING_SIZE);
		drawing.setMinimumSize(DRAWING_SIZE);
		drawing.setMaximumSize(DRAWING_SIZE);
		drawing.setVisible(true);

		//load button
		final JFileChooser fileChooser = new JFileChooser();
		JButton load = new JButton("Load");
		load.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				fileChooser.setCurrentDirectory(new File("."));
				fileChooser.setDialogTitle("Select input file");
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

				if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					loadFile(file);
					redraw();
				}
			}
		});
		JPanel loadpanel = new JPanel(new BorderLayout());
		loadpanel.setMaximumSize(new Dimension(1000, 25));
		loadpanel.setPreferredSize(new Dimension(1000, 25));
		loadpanel.add(load, BorderLayout.PAGE_START);

		//save image button
		JButton save = new JButton("Save Image");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				saveImage("newImage.png");
			}
		});
		JPanel savepanel = new JPanel(new BorderLayout());
		savepanel.setMaximumSize(new Dimension(1000, 25));
		savepanel.setPreferredSize(new Dimension(1000, 25));
		savepanel.add(save, BorderLayout.CENTER);

		//exit button
		JButton exit = new JButton("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				System.exit(0);
			}
		});
		JPanel exitpanel = new JPanel(new BorderLayout());
		exitpanel.setMaximumSize(new Dimension(1000, 25));
		exitpanel.setPreferredSize(new Dimension(1000, 25));
		exitpanel.add(exit, BorderLayout.PAGE_END);

		//sliders
		red.setBackground(new Color(230, 50, 50));
		green.setBackground(new Color(50, 230, 50));
		blue.setBackground(new Color(50, 50, 230));

		JPanel sliderparty = new JPanel();
		sliderparty.setLayout(new BoxLayout(sliderparty, BoxLayout.PAGE_AXIS));
		sliderparty.setBorder(BorderFactory.createTitledBorder("Ambient Light"));

		sliderparty.add(red);
		sliderparty.add(green);
		sliderparty.add(blue);

		//listen the silder change value
		ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				redraw();
			}
		};

		red.addChangeListener(cl);
		blue.addChangeListener(cl);
		green.addChangeListener(cl);

		//mouse wheel function
		drawing.addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() > 0) {
					rp.zoomOut();
					redraw();
				}
				else {
					rp.zoomIn();
					redraw();
				}
			}
		});

		//keyboard function
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent ev) {
				if (ev.getID() == KeyEvent.KEY_PRESSED) {
					onKeyPress(ev);
					redraw();
				}
				return true;
			}
		});

		// make the panel on the right, fix its size, give it a border.
		JPanel controls = new JPanel();
		controls.setPreferredSize(CONTROLS_SIZE);
		controls.setMinimumSize(CONTROLS_SIZE);
		controls.setMaximumSize(CONTROLS_SIZE);
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		Border edge = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		controls.setBorder(edge);

		controls.add(loadpanel);
		controls.add(savepanel);
		controls.add(exitpanel);
		controls.add(Box.createRigidArea(new Dimension(0, 15)));
		controls.add(sliderparty);
		controls.add(Box.createVerticalGlue());

		frame.add(drawing);
		frame.add(controls);

		frame.pack();

		frame.setVisible(true);
	}

	//load data from file
	@SuppressWarnings("resource")
	private void loadFile(File file) {
		try {
			Scanner s = new Scanner(file);
			Scanner sc = new Scanner(s.nextLine());

			//There has two steps to read the file
			//First, read three float numbers which is lightsource
			float lightSourceX = sc.nextFloat();
    		float lightSourceY = sc.nextFloat();
    		float lightSourceZ = sc.nextFloat();
    		lightSource = new Vector3D(lightSourceX, lightSourceY, lightSourceZ);

    		//Second, read next line which contains the vectors and RGB colours
			while(s.hasNextLine()) {
				Scanner scl = new Scanner(s.nextLine());
				Vector3D v1 = new Vector3D(scl.nextFloat(), scl.nextFloat(), scl.nextFloat());
				Vector3D v2 = new Vector3D(scl.nextFloat(), scl.nextFloat(), scl.nextFloat());
				Vector3D v3 = new Vector3D(scl.nextFloat(), scl.nextFloat(), scl.nextFloat());

				int R = scl.nextInt(); //red
				int G = scl.nextInt(); //green
				int B = scl.nextInt(); //blue

				Polygon p = new Polygon(v1, v2, v3, R, G, B); //put data to polygon
				polygons.add(p);
			}
			s.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	protected BufferedImage render() {
		if (!polygons.isEmpty() && !isInitialised) {
			rp = new RenderPipeline(polygons, lightSource, ambientLight, imageHeight);
			Color[][] image = rp.zBuffer();
			isInitialised = true;
			return convertBitmapToImage(image);
		}
		else if (isInitialised) {
			rp.updateAmbientLight(getAmbientLight());
			Color[][] image = rp.zBuffer();
			return convertBitmapToImage(image);
		}
		return null;
	}

	//save to image method
	public void saveImage(String fname) {
		try {
			BufferedImage image = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB );
			File outputfile = new File("image.png");
			ImageIO.write(image, "PNG", outputfile);
			System.out.println("Image saved!");
		}
		catch (IOException e) {
			System.out.println("Image saving failed: " + e);
		}
	}

	//keyboard method
	protected void onKeyPress(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_D) {
			rp.rotateRight();
		}
		else if (ev.getKeyCode() == KeyEvent.VK_A) {
			rp.rotateLeft();
		}
		else if (ev.getKeyCode() == KeyEvent.VK_W) {
			rp.rotateUp();
		}
		else if (ev.getKeyCode() == KeyEvent.VK_S) {
			rp.rotateDown();
		}
		else if (ev.getKeyCode() == KeyEvent.VK_MINUS) {
			rp.zoomOut();
		}
		else if (ev.getKeyCode() == KeyEvent.VK_EQUALS) {
			rp.zoomIn();
		}
	}

	//convert to image
	private BufferedImage convertBitmapToImage(Color[][] bitmap) {
		BufferedImage image = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < imageHeight-2; x++) {
			for (int y = 0; y < imageHeight-1; y++) {
				image.setRGB(x, y, bitmap[x][y].getRGB());
			}
		}
		return image;
	}

	//redraw method
	public void redraw() {
		frame.repaint();
	}

	//get ambient light
	public int[] getAmbientLight() {
		return new int[] { red.getValue(), green.getValue(), blue.getValue() };
	}

	//this is main
	public static void main(String[] args) {
		new GUI();
	}
}
