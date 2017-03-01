package kovac.saving;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;


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
	private static Map<String, Ellipsoid> savedEllipsoids = new LinkedHashMap<String, Ellipsoid>();

	public static void displayAll() {	
	  for(Map.Entry<String, Ellipsoid> e : savedEllipsoids.entrySet()) {
	        System.out.println(e.getKey());
	    }
      System.out.println("");
	}
	
	
	public static void removeGiven(String number) {
		int numerased=0;

	    for(Iterator<Map.Entry<String, Ellipsoid>> it = savedEllipsoids.entrySet().iterator(); it.hasNext(); ) {
	        Map.Entry<String, Ellipsoid> entry = it.next();
			String numberOnly= entry.getValue().getName().replaceAll("[^0-9]", "");

			if (numberOnly.equals(number)){
				it.remove();
				System.out.println("Ellipsoid " + number + " removed");

				Saving.saveCurrentStatic();
				numerased=numerased+1;
			}
	      }
				
		if (LinkedViewersUtil.areSet()) {
			LinkedViewersUtil.removeEllipsoidOverlays();
			LinkedViewersUtil.getOrthCanvas().repaint();
		}
		SavingStatic.regenerate();

		if (numerased==0){
			System.out.println("Ellipsoid " + number + " inexistant.");
		}		
	}
	
	/**
	 * Removes the last ellipsoid
	 * 
	 * @param e
	 *            The ellipsoid to save
	 * @param name
	 *            The name to give to it
	 */
	public static void removeLast() {	
		List<String> list = new ArrayList<String>(savedEllipsoids.keySet());
		if (list.size()>=1){
			savedEllipsoids.remove(list.get(list.size()-1));
			Saving.saveCurrentStatic();
			System.out.println("Last ellipsoid removed. Number of ellipsoids remaining: "+getNumberOfEllipsoids());
			
			if (LinkedViewersUtil.areSet()) {
				LinkedViewersUtil.removeEllipsoidOverlays();
				LinkedViewersUtil.getOrthCanvas().repaint();
			}				
			SavingStatic.regenerate();			
		}
		else{
			System.out.println("No ellipsoid remaining in the list");			
		};
	}
	
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
		System.out.println("Number of regenerated ellipsoids "+savedEllipsoids.size());
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
