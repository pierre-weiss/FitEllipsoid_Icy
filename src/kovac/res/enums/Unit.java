package kovac.res.enums;

public enum Unit {
	
	PIXELS, MILI, MICRON, NANO;
	
	@Override
	public String toString() {
		switch(this) {
		case MICRON: return "Micrometres";
		case MILI: return "Milimetres";
		case NANO: return "Nanometres";
		case PIXELS: return "Pixels";
		default: throw new IllegalArgumentException();	
		}
	}

}
