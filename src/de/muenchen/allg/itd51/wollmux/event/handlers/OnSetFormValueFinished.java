package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;

/**
 * Dieses WollMuxEvent ist das Gegenstück zu handleSetFormValue und wird dann
 * erzeugt, wenn nach einer Änderung eines Formularwertes - gesteuert durch die
 * FormGUI - alle abhängigen Formularwerte angepasst wurden. In diesem Fall ist die
 * einzige Aufgabe dieses Events, den unlockActionListener zu informieren, den
 * handleSetFormValueViaPrintModel() nicht selbst informieren konnte.
 *
 * Das Event wird aus der Implementierung vom OnSetFormValueViaPrintModel.doit()
 * erzeugt, wenn Feldänderungen über die FormGUI laufen.
 *
 * @param unlockActionListener
 *          Der zu informierende unlockActionListener.
 */
public class OnSetFormValueFinished extends BasicEvent
{
	private ActionListener listener;

	public OnSetFormValueFinished(ActionListener unlockActionListener)
	{
		this.listener = unlockActionListener;
	}

	@Override
	protected void doit() throws WollMuxFehlerException
	{
		if (listener != null)
			listener.actionPerformed(null);
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "()";
	}
}