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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.dex.controllers.member.MemberAndProvidersDAO;
import org.opentravel.dex.controllers.member.MemberFilterController;
import org.opentravel.dex.controllers.popup.DexPopupControllerBase.Results;
import org.opentravel.dex.controllers.popup.TypeSelectionContoller;
import org.opentravel.model.OtmModelManager;
import org.opentravel.model.OtmObject;
import org.opentravel.model.OtmResourceChild;
import org.opentravel.model.OtmTypeProvider;
import org.opentravel.model.OtmTypeUser;
import org.opentravel.model.otmLibraryMembers.OtmBusinessObject;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMemberType;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.model.resource.OtmParameterGroup;
import org.opentravel.schemacompiler.model.TLParamGroup;

import java.util.List;

import javafx.application.Platform;

public class AssignResourceSubjectAction extends DexRunAction {
    private static Log log = LogFactory.getLog( AssignResourceSubjectAction.class );

    /**
     * Get the users business object selection from the type selection controller.
     * 
     * @return selected business object or null
     */
    public static OtmBusinessObject getUserTypeSelection(OtmModelManager mgr) {
        return getUserTypeSelection( mgr, null );
    }

    /**
     * Get the users business object selection from the type selection controller.
     * 
     * @param currentSubject if not null, the selection filter is set to only minor versions of the subject.
     * 
     * @return selected business object or null
     */
    public static OtmBusinessObject getUserTypeSelection(OtmModelManager mgr, OtmBusinessObject currentSubject) {
        OtmBusinessObject selection = null;
        if (Platform.isFxApplicationThread()) {
            TypeSelectionContoller controller = TypeSelectionContoller.init();
            MemberFilterController filter = controller.getMemberFilterController();
            filter.setTypeFilterValue( OtmLibraryMemberType.BUSINESS );

            if (currentSubject != null)
                controller.getMemberFilterController().setMinorVersionFilter( currentSubject );

            controller.setManager( mgr );
            if (controller.showAndWait( "MSG" ) == Results.OK) {
                MemberAndProvidersDAO selected = controller.getSelected();
                if (selected != null && selected.getValue() instanceof OtmBusinessObject)
                    selection = (OtmBusinessObject) selected.getValue();
            }
        }
        return selection;
    }

    public static boolean isEnabled(OtmObject subject) {
        if (subject instanceof OtmResource) {
            if (subject.isEditable())
                return true;
            if (subject.getLibrary() != null && subject.getLibrary().isChainEditable())
                return subject.getLibrary().getVersionChain().canAssignLaterVersion( (OtmTypeUser) subject );
        }
        return false;
    }

    public static boolean isEnabled(OtmObject subject, OtmObject value) {
        return isEnabled( subject ) && value instanceof OtmBusinessObject;
    }

    private OtmResource resource = null;
    private OtmBusinessObject oldSubject = null;
    private OtmResource newResource = null; // Non-null if new minor version of resource created to assign subject to.
    private List<OtmResourceChild> toBeFixed = null;

    public AssignResourceSubjectAction() {
        // Constructor for reflection
    }

    /**
     * {@inheritDoc} This action will get the data from the user via modal dialog
     */
    public OtmTypeProvider doIt() {
        log.debug( "Ready to set resource " + resource + " assigned subject. " + ignore );
        if (ignore)
            return null;
        if (resource == null)
            return null;
        if (resource.getActionManager() == null)
            return null;
        OtmBusinessObject currentSubject = null;

        // If this resource is in an older minor version
        if (!resource.isEditable() && resource.getLibrary().isChainEditable()) {
            // Limit selection to minor versions of the subject
            currentSubject = resource.getSubject();
            // Create new minor version of resource
            OtmLibraryMember newLM = resource.getLibrary().getVersionChain().getNewMinorLibraryMember( resource );
            if (newLM instanceof OtmResource)
                newResource = resource = (OtmResource) newLM;
            else {
                otm.getActionManager().postWarning( "Error creating minor version of resource " + resource );
                return null;
            }
        }

        // Get the user's selected business object
        OtmBusinessObject selection = getUserTypeSelection( resource.getModelManager(), currentSubject );

        if (selection != null)
            doIt( selection );

        return get();

    }

    /**
     * This action will get the data from the user via modal dialog
     * 
     * @return
     */
    @Override
    public Object doIt(Object data) {
        if (!(data instanceof OtmBusinessObject))
            return doIt();
        if (data == resource.getSubject())
            return null;

        if (resource.getActionManager() == null)
            return null;

        oldSubject = resource.getSubject();
        OtmBusinessObject newSubject = (OtmBusinessObject) data;

        // Set value into model
        OtmBusinessObject result = resource.setSubject( newSubject );

        toBeFixed = resource.getInvalidChildren();
        if (!toBeFixed.isEmpty()) {
            // Parameter group reference facets
            // Fix those that can be and delete the rest. Save originals for undo. Warn user.
            // log.debug( "Fix these: " + toBeFixed );
            // postWarning( "Changing the subject required deleting: " + toBeFixed
            // + " \nYou can undo the action or create new parameter groups to have new reference facets. \nAny
            // parameters and action requests using the parameters will also have to be fixed." );
            // for (OtmResourceChild rc : toBeFixed) {
            // resource.delete( rc );
            // }
            postWarning( "Changing the subject invalidated: " + toBeFixed
                + " \nYou can undo the action or correct parameter groups to have new reference facets. \nAny parameters and action requests using the parameters will also have to be fixed." );
            // TODO - try to make corrections and save for undo
            // if (rc instanceof OtmParameterGroup) {
            // OtmObject r = ((OtmParameterGroup) rc).setReferenceFacetMatching( null );
            // // String name = ((OtmParameterGroup) rc).getReferenceFacetName();
            // // OtmObject r = ((OtmParameterGroup) rc).setReferenceFacetString( name );
            // if (r == null)
            // }

            // Now validate to show user any action requests that have errors.
            resource.isValid( true );
        }

        if (result != newSubject)
            log.debug( "ERROR setting subject." );

        log.debug( "Set resource subject to " + get() );
        return get();
    }

    @Override
    public OtmBusinessObject get() {
        return resource.getSubject();
    }

    @Override
    public OtmResource getSubject() {
        return resource;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean setSubject(OtmObject subject) {
        if (!(subject instanceof OtmResource))
            return false;
        resource = (OtmResource) subject;
        return true;
    }

    @Override
    public String toString() {
        return "Assigned resource subject: " + get();
    }

    @Override
    public OtmBusinessObject undoIt() {
        log.debug( "Undo-ing change" );
        if (oldSubject != null && oldSubject != resource.setAssignedType( oldSubject ))
            resource.getActionManager().postWarning( "Error undoing change." );
        if (newResource != null && newResource.getLibrary() != null)
            newResource.getLibrary().delete( newResource );

        if (!toBeFixed.isEmpty())
            for (OtmResourceChild c : toBeFixed) {
                if (c instanceof OtmParameterGroup) {
                    resource.getTL().addParamGroup( (TLParamGroup) c.getTL() );
                    resource.add( c );
                }
            }

        return get();
    }
}
