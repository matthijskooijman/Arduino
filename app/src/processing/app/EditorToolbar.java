/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import javax.swing.*;

import processing.app.helpers.SimpleAction;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static processing.app.I18n.tr;

/**
 * Toolbar for the IDE.
 */
public class EditorToolbar extends JPanel {
  /** The amount of space between groups of buttons on the toolbar. */
  private static final int BUTTON_GAP = 5;

  /** Amount of space before the first button */
  private static final int LEFT_PADDING = 3;

  /** Amount of space after the last button */
  private static final int RIGHT_PADDING = 14;

  private final Editor editor;

  public EditorToolbar(Editor editor) {
    this.editor = editor;

    setBackground(Theme.getColor("buttons.bgcolor"));
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    // Create two labels to hold the tooltips for the left and right
    // buttons
    JLabel tooltipLeft = new JLabel("");
    tooltipLeft.setFont(Theme.getFont("buttons.status.font"));
    tooltipLeft.setForeground(Theme.getColor("buttons.status.color"));
    JLabel tooltipRight = new JLabel("");
    tooltipRight.setFont(Theme.getFont("buttons.status.font"));
    tooltipRight.setForeground(Theme.getColor("buttons.status.color"));

    // Create the buttons
    add(Box.createRigidArea(new Dimension(LEFT_PADDING, 0)));
    addLeftButtons(tooltipLeft);
    add(Box.createRigidArea(new Dimension(BUTTON_GAP, 0)));
    add(tooltipLeft);
    add(Box.createHorizontalGlue());
    add(tooltipRight);
    add(Box.createRigidArea(new Dimension(BUTTON_GAP, 0)));
    addRightButtons(tooltipRight);
    add(Box.createRigidArea(new Dimension(RIGHT_PADDING, 0)));

    // Sniff all keys before they are dispatched, and inform all buttons
    // of the current shift state.
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent e) -> {
      if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
        for (Component c : getComponents()) {
          if (c instanceof ToolbarButton)
            ((ToolbarButton) c).shiftChanged(e.isShiftDown());
        }
      }
      // Return false to continue processing this keyEvent
      return false;
    });
  }

  private void addLeftButtons(JLabel tooltipLabel) {
    add(new ToolbarButton(editor.verifyAction, tooltipLabel));
    add(new ToolbarButton(editor.uploadAction, editor.uploadUsingProgrammerAction, tooltipLabel));
    add(Box.createRigidArea(new Dimension(BUTTON_GAP, 0)));
    add(new ToolbarButton(editor.newSketchAction, tooltipLabel));
    add(new ToolbarButton(openSketchAction, tooltipLabel));
    add(new ToolbarButton(editor.saveSketchAction, editor.saveSketchAsAction, tooltipLabel));
  }

  private void addRightButtons(JLabel tooltipLabel) {
    add(new ToolbarButton(editor.toggleSerialMonitorAction, tooltipLabel));
  }

  /**
   * Show a "open sketch" popup menu at the mouse position, used for the
   * "open" toolbar button.
   */
  public final SimpleAction openSketchAction = new SimpleAction(tr("Open"))
      .deriveIcons(Base.getThemeImageFile("open.png")).listener(this::handleOpen);

  /** Handle the openSketchAction */
  void handleOpen() {
    JPopupMenu popup = Editor.toolbarMenu.getPopupMenu();
    Point p = MouseInfo.getPointerInfo().getLocation();
    popup.show(this, p.x, p.y);
  }

  /**
   * Minimal button, intended to be shown in the toolbar.
   *
   * Compared to a normal JButton, this removes all of the decorations,
   * extra spacing and button text, leaving just the icon. JButton takes
   * that icon from the Action passed in, but this class additionally
   * takes a "selected" and "rollover" icon from the action (from
   * non-standard properties). Displaying these icons is handled by
   * JButton normally, though this class also takes the "selected" state
   * from the Action (using the standard property for that).
   * AbstractButton already supports this for e.g. JRadioButton, but
   * enabling that support requires overriding a package-private method
   * we do not have access to.
   *
   * Furthermore, this class supports two different actions: one to use
   * normally and one when shift is pressed, This is implemented by,
   * whenever shift is pressed, changing the action set in JButton (and
   * thus also the icon, if it is different). However, when one of these
   * actions is marked as selected, it will always be the active action,
   * regardless of the state of the shift key.
   */
  private static class ToolbarButton extends JButton {
    /** The action for this button when shift is not pressed */
    private final Action normalAction;
    /**
     * The action for this button when shift is pressed. Can be null to
     * always use the normal action.
     */
    private final Action shiftAction;
    /** Is shift currently pressed? */
    private boolean shiftPressed;
    /** The label in which to show our name on mouseover */
    private JLabel tooltipLabel;
    /** Is our name currently showing in the tooltip? */
    private boolean tooltipShowing;

    /** Create a toolbar button with a single action */
    ToolbarButton(Action action, JLabel tooltipLabel) {
      this(action, null, tooltipLabel);
    }

    /**
     * Create a toolbar button that uses one of two actions, depending
     * on the shift key.
     */
    ToolbarButton(Action normalAction, Action shiftAction, JLabel tooltipLabel) {
      super(normalAction);
      this.normalAction = normalAction;
      this.shiftAction = shiftAction;
      this.tooltipLabel = tooltipLabel;

      // Hide regular button decorations
      setContentAreaFilled(false);
      setBorderPainted(false);
      setBorder(null);
      setMargin(new Insets(0, 0, 0, 0));
      // Prevent the button from taking focus after clicking
      setFocusable(false);

      // Handle tooltips
      addMouseListener(mouseListener);

      // If we have two actions, make sure to call updateAction to
      // (possibly) switch the current action whenever either action's
      // selected state changes.
      if (shiftAction != null) {
        PropertyChangeListener listener = (PropertyChangeEvent evt) -> {
          if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
            updateAction();
          }
        };
        normalAction.addPropertyChangeListener(listener);
        shiftAction.addPropertyChangeListener(listener);
      }
    }

    /** Returns the "selected" state of the given action. */
    private boolean isSelected(Action action) {
      Object value = action.getValue(Action.SELECTED_KEY);
      return value != null && (Boolean) value;
    }

    /**
     * Figures out what action should be the current one, based on the
     * shift status and the actions' selected statuses.
     */
    private void updateAction() {
      // If we have just one action, do not bother
      if (shiftAction == null)
        return;

      // When both are unselected, or both are selected, the shift key
      // decides which one to use. If just one is selected, that one is
      // used.
      if (isSelected(normalAction) == isSelected(shiftAction))
        setAction(shiftPressed ? shiftAction : normalAction);
      else if (isSelected(normalAction))
        setAction(normalAction);
      else
        setAction(shiftAction);
    }

    /**
     * Mouse listener that takes care of setting the tooltip when
     * hovering. Only a single instance of the listener is needed
     * because it uses getSource() to figure out what ToolbarButton to
     * update.
     */
    static final MouseListener mouseListener = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        ToolbarButton b = (ToolbarButton) e.getSource();
        b.tooltipLabel.setText((String) b.getAction().getValue(Action.NAME));
        b.tooltipShowing = true;
      }

      @Override
      public void mouseExited(MouseEvent e) {
        ToolbarButton b = (ToolbarButton) e.getSource();
        b.tooltipLabel.setText("");
        b.tooltipShowing = false;
      }
    };

    /**
     * Should be called by EditorToolbar whenever shift is pressed or released.
     */
    public void shiftChanged(boolean newValue) {
      shiftPressed = newValue;
      updateAction();
    }

    /**
     * This is called by AbstractAction whenever the current Action
     * changes (e.g. through setAction) and copies some additional
     * properties from the Action.
     */
    @Override
    protected void configurePropertiesFromAction(Action action) {
      super.configurePropertiesFromAction(action);
      setSelectedIcon((Icon) action.getValue(SimpleAction.SELECTED_ICON));
      setRolloverIcon((Icon) action.getValue(SimpleAction.ROLLOVER_ICON));
      setSelected(isSelected(action));
      // Hide the text if an icon was set. Normally, an icon should be
      // used, but this makes adding a button easier during development.
      if (getIcon() != null)
        setText("");
      if (tooltipShowing)
        tooltipLabel.setText((String) action.getValue(Action.NAME));
    }

    /**
     * This is called by AbstractAction whenever a property of the
     * current Action changes and copies some additional properties from
     * the Action.
     */
    @Override
    protected void actionPropertyChanged(Action action, String propertyName) {
      // It can happen that the propertyChangeListener registered in our
      // constructor changes the action, before the
      // propertyChangeListener that AbstractButton registers (and that
      // calls us) has fired. If this happens, just ignore the latter
      // here.
      if (action != getAction())
        return;

      super.actionPropertyChanged(action, propertyName);

      // Of these, only selected is expected to change, but check them
      // all for completeness
      if (propertyName == SimpleAction.SELECTED_ICON) {
        setSelectedIcon((Icon) action.getValue(SimpleAction.SELECTED_ICON));
      } else if (propertyName == SimpleAction.ROLLOVER_ICON) {
        setRolloverIcon((Icon) action.getValue(SimpleAction.ROLLOVER_ICON));
      } else if (propertyName == Action.SELECTED_KEY) {
        setSelected(isSelected(action));
      } else if (propertyName == Action.NAME && tooltipShowing)
        tooltipLabel.setText((String) action.getValue(Action.NAME));
      }
  }
}
