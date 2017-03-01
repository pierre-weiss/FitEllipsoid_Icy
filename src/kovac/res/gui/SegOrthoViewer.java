package kovac.res.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.canvas.IcyCanvasEvent;
import icy.canvas.Layer;
import icy.gui.component.IcySlider;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.plugin.interface_.PluginCanvas;
import icy.preferences.CanvasPreferences;
import icy.roi.ROI;
import icy.roi.ROI.ROIPainter;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.system.thread.SingleProcessor;
import icy.system.thread.ThreadUtil;
import icy.type.point.Point3D;
import icy.type.point.Point5D;
import icy.util.EventUtil;
import icy.util.GraphicsUtil;
import kovac.res.Points;
import kovac.res.Points.PointInSpace;
import kovac.res.enums.Methods;
import kovac.res.util.LinkedViewersUtil;
import kovac.res.util.PaintersUtil;
import kovac.shapes.AxisOverlay;
import plugins.weiss.fitellipsoid.fitellipsoid;

public class SegOrthoViewer implements PluginCanvas {

	// Most of this classe's behavior has been copied from
	// OrthoViewer class originally developed by Alexandre Dufour, it has been
	// implemented to solve
	// accessibility problems when trying to override
	// the original class' functionalities

	private boolean isNotDeleting = true;
	
	@Override
	public String getCanvasClassName() {
		return SegOrthoViewer.class.getName();
	}

	@Override
	public IcyCanvas createCanvas(Viewer viewer) {
		return new SegOrthoCanvas(viewer);
	}

	@SuppressWarnings("serial")
	public class SegOrthoCanvas extends IcyCanvas2D {

		private final JPanel orthoViewPanel;
		private final SegOrthoView xy, zy, xz;

		private double xScale = 1, yScale = 1;

		private final IcySlider zoomSlider = new IcySlider(IcySlider.HORIZONTAL, 1, 1000, 100);

		private double zoom = 1;
		private boolean crossHairVisible = true, isLocked = false;

		private Overlay currentAxis;
		
		private JCheckBox lock;
		
		public void setLock(boolean val){
			isLocked=val;
		}
		
		public JCheckBox getLock() {
			if (lock == null) {
				this.lock = new JCheckBox("Lock", false);
				lock.setFocusable(false);
				lock.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						isLocked = lock.isSelected();
						if (!isLocked)
							Points.saveCurrentList();
						refresh();
					}
				});
			}
			return lock;
		}

		public SegOrthoCanvas(Viewer viewer) {
			super(viewer);
			orthoViewPanel = new JPanel(null);
			orthoViewPanel.addMouseWheelListener(new MouseWheelListener() {

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					int newValue = zoomSlider.getValue();
					int steps = e.getWheelRotation() * 10;
					if (steps < 0) {
						newValue += -steps;
						if (newValue == zoomSlider.getMaximum()) {
							newValue = zoomSlider.getMaximum();
						}
					} else {
						newValue -= steps;
					}
					zoomSlider.setValue(newValue);
					SegOrthoCanvas.this.repaint();
				}
			});

			xy = new SegOrthoView(DimensionId.Z);
			xz = new SegOrthoView(DimensionId.Y);
			zy = new SegOrthoView(DimensionId.X);

			xScale = getSequence().getPixelSizeZ() / getSequence().getPixelSizeX();
			if (Double.isNaN(xScale) || xScale == 0)
				xScale = 1.0;

			yScale = getSequence().getPixelSizeZ() / getSequence().getPixelSizeY();
			if (Double.isNaN(yScale) || yScale == 0)
				yScale = 1.0;

			posX = getSequence().getSizeX() / 2;
			posY = getSequence().getSizeY() / 2;
			posZ = getSequence().getSizeZ() / 2;

			orthoViewPanel.add(xy);
			orthoViewPanel.add(zy);
			orthoViewPanel.add(xz);

			setZoom(1.0);

			JScrollPane scroll = new JScrollPane(orthoViewPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			final JScrollBar vertical = scroll.getVerticalScrollBar();
			final JScrollBar horizontal = scroll.getHorizontalScrollBar();

			InputMap mapVert = vertical.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			mapVert.put(KeyStroke.getKeyStroke("DOWN"), "actionWhenDown");
			vertical.getActionMap().put("actionWhenDown", new AbstractAction("keyDownAction") {

				@Override
				public void actionPerformed(ActionEvent e) {
					int currentValue = vertical.getValue();
					vertical.setValue(currentValue + (vertical.getMaximum() / 100));
				}
			});
			mapVert.put(KeyStroke.getKeyStroke("UP"), "actionWhenUp");
			vertical.getActionMap().put("actionWhenUp", new AbstractAction("keyUpAction") {

				@Override
				public void actionPerformed(ActionEvent e) {
					int currentValue = vertical.getValue();
					vertical.setValue(currentValue - (vertical.getMaximum() / 100));
				}
			});

			InputMap mapHor = horizontal.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			mapHor.put(KeyStroke.getKeyStroke("RIGHT"), "actionWhenRight");
			horizontal.getActionMap().put("actionWhenRight", new AbstractAction("keyRightAction") {

				@Override
				public void actionPerformed(ActionEvent e) {
					int currentValue = horizontal.getValue();
					horizontal.setValue(currentValue + (horizontal.getMaximum() / 100));
				}
			});
			mapHor.put(KeyStroke.getKeyStroke("LEFT"), "actionWhenLeft");
			horizontal.getActionMap().put("actionWhenLeft", new AbstractAction("keyLeftAction") {

				@Override
				public void actionPerformed(ActionEvent e) {
					int currentValue = horizontal.getValue();
					horizontal.setValue(currentValue - (horizontal.getMaximum() / 100));
				}
			});

			add(scroll, BorderLayout.CENTER);

			updateTNav();
			updateZNav();

			getMouseImageInfosPanel().setInfoColorVisible(false);
			getMouseImageInfosPanel().setInfoCVisible(false);
			getMouseImageInfosPanel().setInfoXVisible(false);
			getMouseImageInfosPanel().setInfoYVisible(false);
			getMouseImageInfosPanel().setInfoDataVisible(false);

			xy.imageChanged();
			xz.imageChanged();
			zy.imageChanged();

			LinkedViewersUtil.removeAllLinesOverlayFromVTK();
			currentAxis = new AxisOverlay("Axis", imageCoordinates());
			LinkedViewersUtil.addOverlayToVTK(currentAxis);

			invalidate();
		}

		/**
		 * @return The current observed position within the image
		 */
		public double[] imageCoordinates() {
			return new double[] { posX, posY, posZ };
		}

		/**
		 * @return The current rotation of the canvas
		 */
		public double[] imageRotation2D() {
			return new double[] { getRotationX(), getRotationY() };
		}

		/**
		 * Refreshes the coordinates of the displayed axis
		 */
		public void refreshAxis() {
			LinkedViewersUtil.removeAllLinesOverlayFromVTK();
			currentAxis = new AxisOverlay("Axis", imageCoordinates());
			LinkedViewersUtil.addOverlayToVTK(currentAxis);
		}

		public SegOrthoView getXYView() {
			return xy;
		}

		public SegOrthoView getXZView() {
			return xz;
		}

		public SegOrthoView getZYView() {
			return zy;
		}

		@Override
		public void customizeToolbar(JToolBar toolBar) {
			super.customizeToolbar(toolBar);
			toolBar.removeAll(); // To get a minimalist toolbar

			final JCheckBox showCrossHair = new JCheckBox("Crosshair", true);
			showCrossHair.setFocusable(false);
			showCrossHair.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					crossHairVisible = showCrossHair.isSelected();
					refresh();
				}
			});
			toolBar.add(showCrossHair);

			final JCheckBox lock = new JCheckBox("Lock", false);
			lock.setFocusable(false);
			lock.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					isLocked = lock.isSelected();
					if (!isLocked)
						Points.saveCurrentList();
					refresh();
				}
			});
			toolBar.add(getLock());

			final JLabel sizeLabel = new JLabel("  Zoom:");
			final JLabel zoomValueLabel = new JLabel(zoomSlider.getValue() + "%");

			zoomSlider.setFocusable(false);
			zoomSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					setZoom(zoomSlider.getValue() / 100.0);
					zoomValueLabel.setText(zoomSlider.getValue() + "%");
				}
			});

			toolBar.add(sizeLabel);
			toolBar.add(zoomSlider);
			toolBar.add(zoomValueLabel);
		}

		private void setZoom(double newZoom) {
			this.zoom = newZoom;

			// adjust the main (XY) panel size
			int xyWidth = (int) Math.round(newZoom * getSequence().getSizeX());
			int xyHeight = (int) Math.round(newZoom * getSequence().getSizeY());

			xy.setBounds(0, 0, xyWidth, xyHeight);
			zy.setBounds(xyWidth + 5, 0, (int) Math.round(newZoom * getSequence().getSizeZ() * xScale), xyHeight);
			xz.setBounds(0, xyHeight + 5, xyWidth, (int) Math.round(newZoom * getSequence().getSizeZ() * yScale));
			orthoViewPanel.setPreferredSize(
					new Dimension(xy.getWidth() + 5 + zy.getWidth(), xy.getHeight() + 5 + xz.getHeight()));
		}

		@Override
		public java.awt.geom.Point2D.Double canvasToImage(Point point) {
			return super.canvasToImage(point);
		}

		@Override
		public void changed(IcyCanvasEvent event) {
			super.changed(event);

			switch (event.getType()) {
			case POSITION_CHANGED: {
				if (event.getDim() == DimensionId.Z) {
					xy.imageChanged();
					refresh();
				} else if (event.getDim() == DimensionId.T) {
					xy.imageChanged();
					zy.imageChanged();
					xz.imageChanged();
					refresh();
				}
				break;
			}
			default:
			}
		}

		@Override
		protected void lutChanged(int component) {
			super.lutChanged(component);

			try {
				if (xy != null)
					xy.imageChanged();
				if (zy != null)
					zy.imageChanged();
				if (xz != null)
					xz.imageChanged();
			} catch (NullPointerException npE) {
				// as silly as it seems, this may happen...
			}

			refresh();
		}

		protected void mousePositionChanged(DimensionId dim, int x, int y) {
			x = (int) Math.max(0, x / zoom);
			y = (int) Math.max(0, y / zoom);

			int maxWidth = getSequence().getSizeX() - 1;
			int maxHeight = getSequence().getSizeY() - 1;
			int maxDepth = getSequence().getSizeZ() - 1;

			switch (dim) {
			case Z: {

				// adjust X
				if (x > maxWidth)
					x = maxWidth;
				if (x != posX) {
					setPositionXInternal(x);
					zy.imageChanged();
				}

				// adjust Y
				if (y > maxHeight)
					y = maxHeight;
				if (y != posY) {
					setPositionYInternal(y);
					xz.imageChanged();
				}

				break;
			}
			case Y: {

				// adjust X
				if (x > maxWidth)
					x = maxWidth;
				if (x != posX) {
					setPositionXInternal(x);
					zy.imageChanged();
				}

				// adjust Z
				double scale = getSequence().getPixelSizeZ() / getSequence().getPixelSizeX();
				if (Double.isNaN(scale) || scale == 0)
					scale = 1.0;

				y /= scale;
				if (y > maxDepth)
					y = maxDepth;
				if (y != posZ) {
					setPositionZInternal(y);
					xy.imageChanged();
				}

				break;
			}
			case X: {

				// adjust Y
				if (y > maxHeight)
					y = maxHeight;
				if (y != posY) {
					setPositionYInternal(y);
					xz.imageChanged();
				}

				// adjust Z
				double scale = getSequence().getPixelSizeZ() / getSequence().getPixelSizeY();
				if (Double.isNaN(scale) || scale == 0)
					scale = 1.0;

				x /= scale;
				if (x > maxDepth)
					x = maxDepth;
				if (x != posZ) {
					setPositionZInternal(x);
					xy.imageChanged();
				}

				break;
			}
			default:
			}
			
		
			refreshAxis();
			refresh();
		}

		@Override
		public void refresh() {
			getMouseImageInfosPanel().updateInfos(this);
			repaint();
		}

		@Override
		public BufferedImage getRenderedImage(int t, int z, int c, boolean canvasView) {
			Dimension size = orthoViewPanel.getPreferredSize();
			BufferedImage snap = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = snap.createGraphics();

			int oldZ = getPositionZ();
			int oldT = getPositionT();

			setPositionZInternal(z);
			setPositionTInternal(t);
			xy.imageCache.run();

			orthoViewPanel.paintAll(g);

			setPositionZInternal(oldZ);
			setPositionZInternal(oldT);

			return snap;
		}

		public IcyBufferedImage getCurrentImage(DimensionId fixedDim) {
			Sequence seq = getSequence();
			int sizeX = seq.getSizeX();
			int sizeY = seq.getSizeY();
			int sizeZ = seq.getSizeZ(posT);
			int sizeC = seq.getSizeC();

			if (sizeZ == 0)
				return null;

			switch (fixedDim) {
			case Z:
				return super.getImage(posT, posZ, -1);

			case Y: {
				if (posY == -1)
					return null;

				// create the XZ side view
				IcyBufferedImage xzImage = new IcyBufferedImage(sizeX, sizeZ, sizeC, seq.getDataType_());

				int inY = sizeX * posY;
				int out_offset = 0;

				// field only used for debugging purposes
				int inSize = 0, outSize = 0;

				try {
					Object in_z_c_xy = seq.getDataXYCZ(posT);
					Object out_c_xy = xzImage.getDataXYC();

					for (int z = 0; z < sizeZ; z++) {
						Object in_c_xy = Array.get(in_z_c_xy, z);

						// handle missing slices
						if (in_c_xy == null)
							continue;

						out_offset = z * sizeX;

						for (int c = 0; c < sizeC; c++) {
							Object in_xy = Array.get(in_c_xy, c);
							inSize = Array.getLength(in_xy);
							Object out_xy = Array.get(out_c_xy, c);
							outSize = Array.getLength(out_xy);
							System.arraycopy(in_xy, inY, out_xy, out_offset, sizeX);
						}
					}
					return xzImage;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new ArrayIndexOutOfBoundsException("cannot copy from [size=" + inSize + ",off=" + inY
							+ "] to [size=" + outSize + ",off=" + out_offset + "] with size " + sizeX);
				}
			}
			case X: {
				// create the ZY side view
				IcyBufferedImage zyImage = new IcyBufferedImage(sizeZ, sizeY, sizeC, seq.getDataType_());

				Object in_z_c_xy = seq.getDataXYCZ(posT);
				Object out_c_xy = zyImage.getDataXYC();

				// wait for buffers
				ThreadUtil.sleep(20);

				for (int z = 0; z < sizeZ; z++) {
					Object in_c_xy = Array.get(in_z_c_xy, z);

					// handle missing slices
					if (in_c_xy == null)
						continue;

					for (int c = 0; c < sizeC; c++) {
						Object in_xy = Array.get(in_c_xy, c);
						Object out_xy = Array.get(out_c_xy, c);

						for (int y = 0, in_offset = posX, out_off = z; y < sizeY; y++, in_offset += sizeX, out_off += sizeZ) {
							Object pixelIN = Array.get(in_xy, in_offset);
							Array.set(out_xy, out_off, pixelIN);
						}
					}
				}

				return zyImage;
			}

			default:
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public int getPositionC() {
			return -1;
		}

		@Override
		public void repaint() {
			super.repaint();
			if (xy != null)
				xy.repaint();
			if (zy != null)
				zy.repaint();
			if (xz != null)
				xz.repaint();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			super.keyReleased(e);
			repaint();
		}

		@Override
		public Component getViewComponent() {
			return null;
		}

		public class SegOrthoView extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {

			private Timer refreshTimer;

			public class ImageCache implements Runnable {
				/**
				 * image cache
				 */
				private BufferedImage imageCache;

				/**
				 * processor
				 */
				private final SingleProcessor processor;
				/**
				 * internals
				 */
				private boolean needRebuild;

				public ImageCache() {
					super();

					processor = new SingleProcessor(true, "OrthoView renderer");
					// we want the processor to stay alive for sometime
					processor.setKeepAliveTime(5, TimeUnit.MINUTES);

					imageCache = null;
					needRebuild = true;
					// build cache
					processor.submit(this);
				}

				public void invalidCache() {
					needRebuild = true;
				}

				public boolean isValid() {
					return !needRebuild;
				}

				public boolean isProcessing() {
					return processor.isProcessing();
				}

				public void refresh() {
					if (needRebuild)
						// rebuild cache
						processor.submit(this);

					// just repaint
					repaint();
				}

				public BufferedImage getImage() {
					return imageCache;
				}

				@Override
				public void run() {
					// important to set it to false at beginning
					needRebuild = false;

					final IcyBufferedImage img = getCurrentImage(currentDimension);

					if (img != null)
						imageCache = IcyBufferedImageUtil.getARGBImage(img, getLut(), imageCache);
					else
						imageCache = null;

					// repaint now
					repaint();
				}
			}

			/**
			 * Image cache
			 */
			final ImageCache imageCache;

			/**
			 * internals
			 */
			private final Font font;

			private final DimensionId currentDimension;

			private Point2D dragStart, dragEnd, crossPosition;

			private final Point5D.Double mousePosition = new Point5D.Double(getPositionX(), getPositionY(),
					getPositionZ(), getPositionT(), getPositionC());

			public SegOrthoView(DimensionId dim) {
				super();
				this.currentDimension = dim;
				imageCache = new ImageCache();
				font = new Font("Arial", Font.BOLD, 16);
				addMouseListener(this);
				addMouseMotionListener(this);
				addMouseWheelListener(this);

				// Key Binding
				this.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released DELETE"),
						"Start_Delete");
				this.getActionMap().put("Start_Delete", new AbstractAction() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						if (isLocked) {
							isNotDeleting = false;
						}
					}
				});
			}

			@SuppressWarnings("unused")
			@Override
			protected void paintComponent(Graphics g) {
				// pre-paint
				super.paintComponent(g);

				// check if the image data exists

				Sequence seq = getSequence();

				double sizeZ = seq.getSizeZ(posT);
				double scaleX = seq.getPixelSizeZ() / seq.getPixelSizeX();
				if (Double.isNaN(scaleX) || scaleX == 0)
					scaleX = 1.0;
				double scaleY = seq.getPixelSizeZ() / seq.getPixelSizeY();
				if (Double.isNaN(scaleY) || scaleY == 0)
					scaleY = 1.0;

				final BufferedImage img = imageCache.getImage();

				if (img != null) {
					// paint the image data

					final Graphics2D g2 = (Graphics2D) g.create();

					g2.scale(zoom, zoom);

					if (CanvasPreferences.getFiltering()) {
						if (getScaleX() < 4d && getScaleY() < 4d) {
							g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
									RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						} else {
							g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
									RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
						}
					}

					AffineTransform trans = new AffineTransform();
					if (currentDimension == DimensionId.X) {
						trans.scale(scaleX, 1.0);
					} else if (currentDimension == DimensionId.Y) {
						trans.scale(1.0, scaleY);
					}

					g2.drawImage(img, trans, null);

					// paint the layers

					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					final ArrayList<Layer> layers = getVisibleLayers();

					// draw them in inverse order to have first painter event at
					// top
					// every layer but the first (i.e. no image, we draw it
					// ourselves)
					for (int i = layers.size() - 2; i >= 0; i--) {
						final Layer layer = layers.get(i);

						if (!layer.isVisible())
							continue;

						final float alpha = layer.getOpacity();

						if (alpha != 1f)
							g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
						else
							g2.setComposite(AlphaComposite.SrcOver);

						if (currentDimension == DimensionId.Z) {
							layer.getOverlay().paint(g2, seq, SegOrthoCanvas.this);
						} else {
							// on side views, check if ROI can be painted
							ROI roi = layer.getAttachedROI();

							if (roi != null && roi instanceof ROI2D) {
								Color color = ((ROIPainter) layer.getOverlay()).getDisplayColor();
								double stroke = ((ROIPainter) layer.getOverlay()).getStroke();

								Rectangle rect = ((ROI2D) roi).getBounds();
								g2.setColor(color);
								g2.setStroke(new BasicStroke(
										(float) ROI.getAdjustedStroke(SegOrthoCanvas.this, stroke + 2d)));

								if (currentDimension == DimensionId.X) {
									// YZ view
									rect.x = 0;
									rect.width = (int) (getWidth() * LinkedViewersUtil.getScale()[2]);
									rect.width /= zoom;
								} else {
									// XZ view
									rect.y = 0;
									rect.height = (int) (getHeight() * LinkedViewersUtil.getScale()[2]);
									rect.height /= zoom;
								}

								// draw border black line
								g2.setStroke(new BasicStroke((float) ROI.getAdjustedStroke(SegOrthoCanvas.this,
										stroke + (roi.isSelected() ? 2d : 1d))));

								g2.setColor(Color.black);
								g2.draw(rect);

								// draw internal border
								g2.setColor(color);
								g2.setStroke(new BasicStroke((float) ROI.getAdjustedStroke(SegOrthoCanvas.this,
										stroke + (roi.isSelected() ? 1d : 0d))));
								g2.draw(rect);

							}
						}
					}

					// draw cross hair

					if (crossHairVisible) {
						g2.setStroke(new BasicStroke((float) (zoom > 1 ? 1.0 / zoom : zoom)));
						g2.setColor(Color.white);
						g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
						switch (currentDimension) {
						case Z:
							if (zoom > 1) {
								g2.draw(new Rectangle2D.Double(posX, 0, 1, seq.getHeight()));
								g2.draw(new Rectangle2D.Double(0, posY, seq.getWidth(), 1));
							} else {
								g2.drawLine(posX, 0, posX, seq.getHeight());
								g2.drawLine(0, posY, seq.getWidth(), posY);
							}
							
							break;

						case X:
							if (zoom > 1) {
								int x = (int) Math.round(posZ * scaleX);
								g2.draw(new Rectangle2D.Double(0, posY, (int) (seq.getSizeZ() * scaleX), 1));
								g2.draw(new Rectangle2D.Double(x, 0, scaleX, seq.getHeight()));
							} else {
								g2.drawLine(0, posY, (int) (seq.getSizeZ() * scaleX), posY);
								int x = (int) Math.round(posZ * scaleX + scaleX * 0.5);
								g2.setStroke(new BasicStroke((float) (scaleX * zoom)));
								g2.drawLine(x, 0, x, seq.getHeight());
							}
							break;

						case Y:
							if (zoom > 1) {
								int y = (int) Math.round(posZ * scaleY);
								g2.draw(new Rectangle2D.Double(0, y, seq.getWidth(), scaleY));
								g2.draw(new Rectangle2D.Double(posX, 0, 1,
										seq.getHeight() * LinkedViewersUtil.getScale()[2]));
							} else {
								g2.drawLine(posX, 0, posX, (int) (seq.getSizeZ() * scaleX));
								int y = (int) Math.round(posZ * scaleY + scaleY * 0.5);
								g2.setStroke(new BasicStroke((float) (scaleY * zoom)));
								g2.drawLine(0, y, seq.getWidth(), y);
							}
							break;

						default:
							break;
						}
					}

					PaintersUtil.paintPoints(g2, currentDimension, SegOrthoCanvas.this.imageCoordinates());

					if (!isNotDeleting) {
						PaintersUtil.paintEraser(g2, currentDimension, dragStart, dragEnd, crossPosition);
					}
					
					g2.setStroke(new BasicStroke(1.0f));
					PaintersUtil.paintIntersection(g2, currentDimension, SegOrthoCanvas.this.imageCoordinates());

					g2.dispose();

				} else {
					final Graphics2D g2 = (Graphics2D) g.create();

					g2.setFont(font);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

					if (SegOrthoCanvas.this.getCurrentImage() != null)
						// cache not yet built
						drawTextCenter(g2, "Loading...", 0.8f);
					else
						// no image
						drawTextCenter(g2, " No image ", 0.8f);
				}

				// image or layers changed during repaint --> refresh again

				if (!isCacheValid())
					refresh();

				// if (!isCacheValid())
				// refresh();
				// cache is being rebuild --> refresh to show progression
				// else if (imageCache.isProcessing()) refreshLater(100);
			}

			public void drawTextBottomRight(Graphics2D g, String text, float alpha) {
				final Rectangle2D rect = GraphicsUtil.getStringBounds(g, text);
				final int w = (int) rect.getWidth();
				final int h = (int) rect.getHeight();
				final int x = getWidth() - (w + 8 + 2);
				final int y = getHeight() - (h + 8 + 2);

				g.setColor(Color.gray);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g.fillRoundRect(x, y, w + 8, h + 8, 8, 8);

				g.setColor(Color.white);
				g.drawString(text, x + 4, y + 2 + h);
			}

			public void drawTextTopRight(Graphics2D g, String text, float alpha) {
				final Rectangle2D rect = GraphicsUtil.getStringBounds(g, text);
				final int w = (int) rect.getWidth();
				final int h = (int) rect.getHeight();
				final int x = getWidth() - (w + 8 + 2);
				final int y = 2;

				g.setColor(Color.gray);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g.fillRoundRect(x, y, w + 8, h + 8, 8, 8);

				g.setColor(Color.white);
				g.drawString(text, x + 4, y + 2 + h);
			}

			public void drawTextCenter(Graphics2D g, String text, float alpha) {
				final Rectangle2D rect = GraphicsUtil.getStringBounds(g, text);
				final int w = (int) rect.getWidth();
				final int h = (int) rect.getHeight();
				final int x = (getWidth() - (w + 8 + 2)) / 2;
				final int y = (getHeight() - (h + 8 + 2)) / 2;

				g.setColor(Color.gray);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g.fillRoundRect(x, y, w + 8, h + 8, 8, 8);

				g.setColor(Color.white);
				g.drawString(text, x + 4, y + 2 + h);
			}

			@Override
			public void repaint() {
				super.repaint();
			}

			public void refresh() {
				imageCache.refresh();
			}

			/**
			 * Refresh in sometime
			 */
			public void refreshLater(int milli) {
				refreshTimer.setInitialDelay(milli);
				refreshTimer.start();
			}

			public void imageChanged() {
				imageCache.invalidCache();
			}

			public void layersChanged() {

			}

			public boolean isCacheValid() {
				return imageCache.isValid();
			}

			public double[] getClickCoordinates(DimensionId dim, int x, int y) {
				double xClick = 0, yClick = 0, zClick = 0;
				switch (dim) {
				case X:
					xClick = imageCoordinates()[0];
					yClick = (y * getSequence().getSizeY()) / getZYView().getBounds().getHeight();
					zClick = (x * getSequence().getSizeZ()) / getZYView().getBounds().getWidth();
					break;
				case Z:
					xClick = (x * getSequence().getSizeX()) / getXYView().getBounds().getWidth();
					yClick = (y * getSequence().getSizeY()) / getXYView().getBounds().getHeight();
					zClick = imageCoordinates()[2];
					break;
				case Y:
					xClick = (x * getSequence().getSizeX()) / getXZView().getBounds().getWidth();
					yClick = imageCoordinates()[1];
					zClick = (y * getSequence().getSizeZ()) / getXZView().getBounds().getHeight();
					break;
				default:
					break;
				}
				return new double[] { xClick, yClick, zClick };
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// send mouse event to painters first
				for (Layer layer : getVisibleLayers())
					layer.getOverlay().mouseClick(e, mousePosition, SegOrthoCanvas.this);
				if (isLocked) {
					return;
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {

				// send mouse event to painters first
				for (Layer layer : getVisibleLayers())
					layer.getOverlay().mousePressed(e, mousePosition, SegOrthoCanvas.this);
				if (isLocked) {
					if (!isNotDeleting) {
						dragStart = PaintersUtil.translatePoint(
								new Point3D.Double(getClickCoordinates(currentDimension, e.getX(), e.getY())),
								currentDimension);
						dragEnd = dragStart;
						repaint();
						e.consume();
						return;
					}
					if (fitellipsoid.getChosenMethod() == Methods.POINTS) {
						if (EventUtil.isRightMouseButton(e)) {
							Points.removeLastOne();
							return;
						}
						if (fitellipsoid.getChosenMethod() == Methods.POINTS) {
							Point3D pointToDraw = new Point3D.Double(
									getClickCoordinates(currentDimension, e.getX(), e.getY()));
							Points.addPoint(pointToDraw, currentDimension);
						}
					} else {
						// TODO : Ellipse
					}
					return;
				}
				mousePositionChanged(currentDimension, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// send mouse event to painters after
				for (Layer layer : getVisibleLayers())
					layer.getOverlay().mouseReleased(e, mousePosition, SegOrthoCanvas.this);
				if (isLocked) {
					if (!isNotDeleting) {
						for (PointInSpace pt : Points.getAllPoints()) {
							if (pt.getDim() == currentDimension)
								if (PaintersUtil.isInsideEraser(pt.getPoint(), currentDimension)) {
									Points.remove(pt);
								}
						}
						PaintersUtil.eraseRectangle();
						repaint();
						dragStart = null;
						dragEnd = null;
						isNotDeleting = true;
						e.consume();
						return;
					}
					if (fitellipsoid.getChosenMethod() == Methods.ELLIPSES)
						// TODO : Ellipse
						return;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				Point2D.Double p = canvasToImage(e.getPoint());
				Point5D.Double p5 = new Point5D.Double(p.x / zoom, p.y / zoom, mousePosition.z, mousePosition.t,
						mousePosition.c);
				// mousePosition.y = p.y;
				// send mouse event to painters after
				for (Layer layer : getVisibleLayers())
					layer.getOverlay().mouseDrag(e, p5, SegOrthoCanvas.this);
				if (isLocked) {
					if (!isNotDeleting) {
						dragEnd = PaintersUtil.translatePoint(
								new Point3D.Double(getClickCoordinates(currentDimension, e.getX(), e.getY())),
								currentDimension);
						repaint();
						return;
					}
					if (fitellipsoid.getChosenMethod() == Methods.ELLIPSES) {
						// TODO : Ellipse
					}
					return;
				}
				mousePositionChanged(currentDimension, e.getX(), e.getY());
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int newValue = zoomSlider.getValue();
				int steps = e.getWheelRotation() * 10;
				if (steps < 0) {
					newValue += -steps;
					if (newValue == zoomSlider.getMaximum()) {
						newValue = zoomSlider.getMaximum();
					}
				} else {
					newValue -= steps;
				}
				zoomSlider.setValue(newValue);
				SegOrthoCanvas.this.repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				Point2D.Double p = canvasToImage(e.getPoint());
				mousePosition.x = p.x / zoom;
				mousePosition.y = p.y / zoom;
				// send mouse event to painters first
				for (Layer layer : getVisibleLayers())
					layer.getOverlay().mouseMove(e, mousePosition, SegOrthoCanvas.this);
				if (isLocked) {
					if (!isNotDeleting) {
						Point3D point3d = new Point3D.Double(getClickCoordinates(currentDimension, e.getX(), e.getY()));
						crossPosition = PaintersUtil.translatePoint(point3d, currentDimension);
						repaint();
						return;
					}
					if (EventUtil.isShiftDown(e)) {
						Point3D pointToDraw = new Point3D.Double(
								getClickCoordinates(currentDimension, e.getX(), e.getY()));
						Points.addPoint(pointToDraw, currentDimension);
						ThreadUtil.sleep(100);
					}
				}

				repaint();
			}

		}
	}

}
