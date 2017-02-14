package kovac.saving;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import icy.painter.Overlay;
import kovac.res.util.LinkedViewersUtil;
import kovac.shapes.Ellipsoid;
import kovac.shapes.EllipsoidOverlay;

/**
 * This class handles the Saving of the created ellipsoids and group of
 * ellipsoids
 * 
 * @author bastien.kovac
 *
 */
public class SavingStatic {

	/**
	 * The saved ellipsoids (each must have a different name)
	 */
	private static Map<String, Ellipsoid> savedEllipsoids = new HashMap<String, Ellipsoid>();

	/**
	 * Saves the given ellipsoid with the given name
	 * 
	 * @param e
	 *            The ellipsoid to save
	 * @param name
	 *            The name to give to it
	 */
	public static void saveEllipsoid(Ellipsoid e, String name) {
		if (savedEllipsoids.containsKey(name)) {
			throw new RuntimeException("This ellipsoid's name is already used");
		} else {
			savedEllipsoids.put(name, e);
		}
		Saving.saveCurrentStatic();
		System.out.println(name + " saved");
	}

	/**
	 * Gets the ellipsoid with given name
	 * 
	 * @param name
	 *            The name
	 * @return The corresponding ellipsoid
	 */
	public static Ellipsoid getEllipsoid(String name) {
		if (!savedEllipsoids.containsKey(name)) {
			throw new RuntimeException("No ellipsoid with such name");
		}
		return savedEllipsoids.get(name);
	}

	/**
	 * @return The number of saved ellipsoid
	 */
	public static int getNumberOfEllipsoids() {
		return savedEllipsoids.size();
	}
	
	public static void hide(EllipsoidOverlay e) {
		for (Overlay o : LinkedViewersUtil.getVTKOverlays()) {
			if (!(o instanceof EllipsoidOverlay))
				continue;
			if (!o.equals(e)) {
				LinkedViewersUtil.removeOverlayFromVTK(o);
			}
		}
	}
	
	public static Collection<Ellipsoid> getAllEllipsoids() {
		return savedEllipsoids.values();
	}
	
	public static void regenerate() {
		for (Ellipsoid e : savedEllipsoids.values()) {
			e.regenerate();
		}
	}
	
	public static void clear() {
		savedEllipsoids.clear();
	}
	
	public static void rename(String oldName, String newName) {
		Ellipsoid e = savedEllipsoids.get(oldName);
		savedEllipsoids.remove(oldName);
		savedEllipsoids.put(newName, e);
	}
	
	public static void deleteEllipsoid(String name) {
		savedEllipsoids.remove(name);
	}
	
}
