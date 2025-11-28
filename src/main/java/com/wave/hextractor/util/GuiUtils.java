package com.wave.hextractor.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Class for gui utilities.
 */
public class GuiUtils {

	/**
	 * Instantiates a new gui utils.
	 */
	private GuiUtils() {
	}

	/**
	 * Shows a confirmation dialog for the title and message passed.
	 * @return true if accepted.
	 */
	public static boolean confirmActionAlert(String title, String message) {
		return JOptionPane.showConfirmDialog(null, message, title,
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}

	/**
	 * Returns a UI scale factor.
	 * Priority: system property 'hextractor.ui.scale' (or env var HEXTRACTOR_UI_SCALE) -> toolkit DPI / 96.0 -> 1.0 fallback
	 */
	public static double getScale() {
		// allow explicit override via system property
		String prop = System.getProperty("hextractor.ui.scale");
		if (prop == null || prop.trim().isEmpty()) {
			prop = System.getenv("HEXTRACTOR_UI_SCALE");
		}
		if (prop != null) {
			try {
				double v = Double.parseDouble(prop.trim());
				if (v > 0) {
					return v;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		try {
			int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
			if (dpi > 0) {
				return dpi / 96.0;
			}
		} catch (Throwable t) {
			// ignore - return default
		}
		return 1.25; // Default to 125% scale
	}

	/** Scale a font by the UI scale factor. */
	public static Font scaleFont(Font f) {
		if (f == null) return null;
		float newSize = (float) (f.getSize2D() * getScale());
		return f.deriveFont(newSize);
	}

	/** Scale a dimension by the UI scale factor. */
	public static Dimension scaleDimension(Dimension d) {
		if (d == null) return null;
		double s = getScale();
		return new Dimension((int) Math.round(d.width * s), (int) Math.round(d.height * s));
	}

	/** Scale an integer size by the UI scale factor. */
	public static int scaleInt(int v) {
		return (int) Math.round(v * getScale());
	}

	/**
	 * Scale an image by provided scale factor (preserves aspect ratio).
	 */
	public static Image scaleImage(Image img, double scale) {
		if (img == null) return null;
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		if (w <= 0 || h <= 0) return img;
		Image scaled = img.getScaledInstance((int) Math.round(w * scale), (int) Math.round(h * scale), Image.SCALE_SMOOTH);
		// ensure we have a BufferedImage (optional)
		BufferedImage bi = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bi.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(scaled, 0, 0, null);
		g2.dispose();
		return bi;
	}

	/**
	 * Scale an Insets by the UI scale factor.
	 * Useful for borders and margins.
	 */
	public static Insets scaleInsets(Insets insets) {
		if (insets == null) return null;
		double s = getScale();
		return new Insets(
				(int) Math.round(insets.top * s),
				(int) Math.round(insets.left * s),
				(int) Math.round(insets.bottom * s),
				(int) Math.round(insets.right * s)
		);
	}

	/**
	 * Apply DPI-aware font to a component and optionally all its children recursively.
	 * This is useful for ensuring consistent font scaling across the entire UI.
	 * 
	 * @param component The component to apply the font to
	 * @param font The base font to scale and apply
	 * @param recursive If true, applies to all child components recursively
	 */
	public static void applyScaledFont(Component component, Font font, boolean recursive) {
		if (component == null || font == null) return;
		Font scaled = scaleFont(font);
		component.setFont(scaled);
		// Java 21 pattern matching for instanceof
		if (recursive && component instanceof Container container) {
			for (Component child : container.getComponents()) {
				applyScaledFont(child, font, true);
			}
		}
	}
}
