package kovac.res.enums;

public enum Methods {
	
	POINTS, ELLIPSES;
	
	@Override
	public String toString() {
		switch(this) {
		case ELLIPSES:
			return "Ellipses";
		case POINTS:
			return "Points";
		default:
			throw new IllegalArgumentException();		
		}
	}

}
