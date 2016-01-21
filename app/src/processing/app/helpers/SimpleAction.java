/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Matthijs Kooijman <matthijs@stdin.nl>
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */

package processing.app.helpers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

/**
 * Class to easily define instances of the Swing Action interface.
 *
 * When using AbstractAction, you have to create a subclass that
 * implements the actionPerformed() method, and sets attributes in the
 * constructor, which gets verbose quickly. This class implements
 * actionPerformed for you, and forwards it to the ActionListener passed
 * to the listener method (intended to be a lambda expression).
 * Additional Action attributes can be set by calling the (chainable)
 * setters.
 *
 * The name of this class refers to the fact that it's simple to create an
 * action using this class, but perhaps a better name can be found for it.
 *
 * @see javax.swing.Action
 */
public class SimpleAction extends AbstractAction {
  private ActionListener listener;
  /** Key for the icon to be displayed when this action is marked as selected */
  public static final String SELECTED_ICON = "ArduinoSelectedIconKey";
  /** Key for the icon to be displayed when the mouse hovers over it */
  public static final String ROLLOVER_ICON = "ArduinoRolloverIconKey";

  /**
   * Version of ActionListener that does not take an ActionEvent as an argument
   * This can be used when you do not care about the event itself, just that it
   * happened, typically for passing a argumentless lambda or method reference
   * to the SimpleAction constructor.
   */
  public interface AnonymousActionListener {
    public void actionPerformed();
  }

  public SimpleAction() {
  }

  public SimpleAction(String name) {
    name(name);
  }

  /*
   * Chainable setter methods. The methods below can be used to set
   * various properties on a SimpleAction. Each returns the object
   * itself, so they can be chained (starting with the constructor,
   * typically, setting all needed properties in a single statement).
   */

  public SimpleAction name(String name) {
    this.putValue(NAME, name);
    return this;
  }

  public SimpleAction accelerator(KeyStroke keystroke) {
    this.putValue(ACCELERATOR_KEY, keystroke);
    return this;
  }

  public SimpleAction listener(ActionListener listener) {
    this.listener = listener;
    return this;
  }

  public SimpleAction listener(AnonymousActionListener listener) {
    this.listener = (ActionEvent) -> listener.actionPerformed();
    return this;
  }

  public SimpleAction icon(Icon icon) {
    this.putValue(LARGE_ICON_KEY, icon);
    return this;
  }

  public SimpleAction selectedIcon(Icon icon) {
    this.putValue(SELECTED_ICON, icon);
    return this;
  }

  public SimpleAction rolloverIcon(Icon icon) {
    this.putValue(ROLLOVER_ICON, icon);
    return this;
  }

  /**
   * This sets the main icon to the filename given. The filename for
   * selected and rollover icons is automatically derived by adding
   * "-selected" and "-rollover" to the filename. If either of these do
   * not exist, it is silently skipped.
   */
  public SimpleAction deriveIcons(File filename) {
    icon(new ImageIcon(filename.getAbsolutePath()));
    FileUtils.SplitFile split = FileUtils.splitFilename(filename.getAbsolutePath());

    String selected = FileUtils.addExtension(split.basename + "-selected", split.extension);
    if (new File(selected).exists())
      selectedIcon(new ImageIcon(selected));

    String rollover = FileUtils.addExtension(split.basename + "-rollover", split.extension);
    if (new File(rollover).exists())
      rolloverIcon(new ImageIcon(rollover));
    return this;
  }

  /* Get the current value of the "selected" property. */
  public boolean getSelected() {
    Object value = getValue(Action.SELECTED_KEY);
    return value != null && (Boolean) value;
  }

  /* Set the "selected" property */
  public void setSelected(boolean selected) {
    putValue(Action.SELECTED_KEY, selected);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    listener.actionPerformed(e);
  }
}
