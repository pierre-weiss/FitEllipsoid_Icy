package kovac.gui.res;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This class is a customized IntegerField which only accepts numbers, and
 * customizes their appearance. It also can be used to link two instances of
 * this object, one as the minimum and the other as the maximum, to ease
 * verifications
 * 
 * @author bastien.kovac
 *
 */
public class IntegerField extends JTextField {

	/**
	 * To avoid warnings
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new IntegerField
	 */
	public IntegerField() {
		super();
	}
	
	public IntegerField(String defaultText) {
		super(defaultText);
	}

	@Override
	protected Document createDefaultModel() {
		return new UpperCaseDocument();
	}

	/**
	 * The DocumentClass to handle only number inputs
	 * 
	 * @author bastien.kovac
	 *
	 */
	private class UpperCaseDocument extends PlainDocument {

		/**
		 * To avoid warnings
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

			if (str == null) {
				return;
			}

			char[] chars = str.toCharArray();
			boolean ok = true;
			boolean comma = (IntegerField.this.getText().contains("."));

			for (int i = 0; i < chars.length; i++) {

				try {
					if (comma || chars[i] != ',' && chars[i] != '.') {
						Integer.parseInt(String.valueOf(chars[i]));
					}
					if (!comma) {
						if (chars[i] == ',') {
							chars[i] = '.';
							comma = true;
						}
						if (chars[i] == '.') {
							comma = true;
						}
					}
				} catch (NumberFormatException exc) {
					ok = false;
					break;
				}
			}

			if (ok) {
				super.insertString(offs, new String(chars), a);
			}

		}
	}

}
