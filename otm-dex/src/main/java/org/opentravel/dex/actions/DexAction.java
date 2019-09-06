/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.dex.actions;

import org.opentravel.dex.events.DexChangeEvent;
import org.opentravel.model.OtmObject;
import org.opentravel.schemacompiler.validate.ValidationFindings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * Actions are invoked by the view controllers to perform <i>actions</i> on the model.
 * <p>
 * They are designed to be set as listeners to FX Observable objects. When the observable value changes, the associated
 * action handler is fired.
 * 
 * @param <T> is the data type consumed by the action to make the change to the object
 * 
 * @author dmh
 *
 */
public interface DexAction<T> {


    // /**
    // *
    // * @param data value to be applied in the action, must be of type defined in the generic parameter
    // */
    // public void doIt();

    // /**
    // * @deprecated - all actions NO-OP this method Run the action.
    // */
    // @Deprecated
    // public void doIt(Object data);

    // All implementations must implement, but the signatures will be different.
    // public T doIt(ObservableValue<? extends T> observable, T oldValue, T newValue);

    /**
     * Return the event object if the action defines an event to throw when action is done or undone.
     * 
     * @see {@link DexActions#getEvent(DexActions)}
     * 
     * @return event object or null
     */
    public DexChangeEvent getEvent();

    /**
     * @return
     */
    public ObservableValue<? extends T> getObservable();

    /**
     * Get the OTM object that is or will be acted upon.
     * 
     * @return
     */
    public OtmObject getSubject();

    // /**
    // * @return true if the requested change is allowed for object in this application and user.
    // */
    // public boolean isAllowed(T value);

    // /**
    // * Is the action enabled for this subject?
    // *
    // * @return true if change is enabled for this application and user.
    // */
    // public boolean isEnabled();

    public DexActions getType();

    /**
     * Test to see if the change is un-doable. Veto findings will be set if action can not be undone and therefore
     * rejected.
     * 
     * @return null or veto findings
     */
    ValidationFindings getVetoFindings();

    /**
     * @return true if change already made is valid for this object for this application and user.
     */
    public boolean isValid();

    public void removeChangeListener();

    /**
     * @param changeListener
     */
    public void setChangeListener(ChangeListener<T> changeListener, ObservableValue<? extends T> observable);

    /**
     * Set the subject for this action to act upon.
     * 
     * @param subject
     * @return false if the subject could not be set
     */
    public boolean setSubject(OtmObject subject);

    /**
     * Use the stored values to undo the change.
     * 
     * @param fireEvent if true, also fire the associated change event
     * 
     * @return
     */
    public T undoIt();


    // public T redo();
    // /**
    // * @return true if change is valid for this object for this application and user.
    // */
    // public boolean wouldBeValid(T value);
}
