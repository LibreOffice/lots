package de.muenchen.allg.itd51.wollmux;

public class TextFragmentNotDefinedException extends Exception {

	/**
	 * Abgefragte ID des nicht vorhandenen Textfragments.
	 */
	private String id;

	private static final long serialVersionUID = -7265020323269743988L;

	public TextFragmentNotDefinedException(String id) {
		this.id = id;
	}

	public String toString() {
		return "Das abgefragte Textfragment mit der id \"" + id
				+ "\" ist nicht definiert.";
	}
}
