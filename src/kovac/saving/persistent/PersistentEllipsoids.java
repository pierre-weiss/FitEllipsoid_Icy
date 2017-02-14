package kovac.saving.persistent;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import kovac.saving.SavingStatic;
import kovac.shapes.Ellipsoid;

public class PersistentEllipsoids implements XMLPersistent {

	private static final String ID_CLASS = "class";
	private static final String VALUE_CLASS = PersistentEllipsoids.class.getName();
	private static final String ID_ELLIPSOID = "ellipsoid";
	private static final String ID_NAME = "name";
	private static final String ID_ID = "id";

	@Override
	public boolean saveToXML(Node node) {

		if (node == null)
			return false;

		Element className = XMLUtil.getElement(node, ID_CLASS);
		if (className == null) {
			className = XMLUtil.addElement(node, ID_CLASS);
		}

		if (className == null) {
			className = XMLUtil.addElement(node, ID_CLASS);
			XMLUtil.setAttributeValue(className, ID_NAME, VALUE_CLASS);
		}

		for (Ellipsoid e : SavingStatic.getAllEllipsoids()) {
			if (e != null)
				e.saveEllipsoidToXML(className);
		}

		return true;
	}

	@Override
	public boolean loadFromXML(Node node) {

		if (node == null)
			return false;

		List<Element> ellipsoids = XMLUtil.getElements(XMLUtil.getElement(node, ID_CLASS), ID_ELLIPSOID);

		for (Element e : ellipsoids) {
			String uniqueID = XMLUtil.getAttributeValue(e, ID_ID, "");
			Ellipsoid ell = new Ellipsoid(uniqueID);
			ell.loadEllipsoidFromXML(e);
		}

		return true;

	}

}
