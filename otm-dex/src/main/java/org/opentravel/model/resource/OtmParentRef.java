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

package org.opentravel.model.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.common.DexEditField;
import org.opentravel.common.ImageManager;
import org.opentravel.common.ImageManager.Icons;
import org.opentravel.dex.actions.DexActionManager.DexActions;
import org.opentravel.dex.controllers.DexIncludedController;
import org.opentravel.model.OtmModelElement;
import org.opentravel.model.OtmResourceChild;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.schemacompiler.model.TLResourceParentRef;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;

/**
 * OTM Object for Resource Action objects.
 * 
 * @author Dave Hollander
 * 
 */
public class OtmParentRef extends OtmResourceChildBase<TLResourceParentRef> implements OtmResourceChild {
    private static Log log = LogFactory.getLog( OtmParentRef.class );

    public OtmParentRef(TLResourceParentRef tla, OtmResource owner) {
        super( tla, owner );
    }

    @Override
    public String getName() {
        return getParentResource().getName();
    }

    @Override
    public TLResourceParentRef getTL() {
        return (TLResourceParentRef) tlObject;
    }

    @Override
    public Icons getIconType() {
        return ImageManager.Icons.RESOURCE_PARENTREF;
    }

    private ObservableList<String> getParentCandidates() {
        ObservableList<String> candidates = FXCollections.observableArrayList();
        getOwningMember().getModelManager().getResources( true )
            .forEach( r -> candidates.add( r.getNameWithPrefix() ) );
        candidates.remove( getOwningMember().getNameWithPrefix() );
        return candidates;
    }

    public OtmResource getParentResource() {
        return (OtmResource) OtmModelElement.get( getTL().getParentResource() );
    }

    /**
     * @return true if the parent resource exists and is first class
     */
    public boolean isParentFirstClass() {
        return getParentResource() != null && getParentResource().isFirstClass();
    }


    /**
     * @return the parameter group used by this parent reference.
     */
    public OtmParameterGroup getParameterGroup() {
        return (OtmParameterGroup) OtmModelElement.get( getTL().getParentParamGroup() );
    }

    public String getParentResourceName() {
        return getParentResource() != null ? getParentResource().getName() : "";
    }

    public void setParentResource(String name) {
        log.error( "FIXME - set parent resource to " + name );
    }

    private Node getParentNode(DexIncludedController<?> ec) {
        StringProperty selection = null;
        if (getParentResource() != null)
            selection = getActionManager().add( DexActions.SETPARENTREFPARENT, getParentResourceName(), this );
        else
            selection = getActionManager().add( DexActions.SETPARENTREFPARENT, "", this );
        ComboBox<String> box = DexEditField.makeComboBox( getParentCandidates(), selection, ec, this );
        return box;
    }

    public ObservableList<String> getParameterGroupCandidates() {
        ObservableList<String> candidates = FXCollections.observableArrayList();
        if (getParentResource() != null)
            getParentResource().getParameterGroups().forEach( pg -> candidates.add( pg.getName() ) );
        return candidates;
    }

    private Node getParameterGroupNode(DexIncludedController<?> ec) {
        StringProperty selection =
            getActionManager().add( DexActions.SETPARENTPARAMETERGROUP, getParameterGroupName(), this );
        return DexEditField.makeComboBox( getParameterGroupCandidates(), selection, ec, this );
    }

    public String getParameterGroupName() {
        return getTL().getParentParamGroupName();
    }

    /**
     * @param path is the string to set or null
     * @return
     */
    public void setPathTemplate(String path) {
        getTL().setPathTemplate( path );
    }

    /**
     * @return
     */
    public String getPathTemplate() {
        return getTL().getPathTemplate();
    }

    public void setParameterGroup(String name) {
        // getTL().getParentParamGroupName();
        log.error( "FIXME - set parameter group name to " + name );
    }

    private Node getPathNode(DexIncludedController<?> ec) {
        StringProperty selection = getActionManager().add( DexActions.SETPARENTPATHTEMPLATE, getPathTemplate(), this );
        return DexEditField.makeTextField( selection, ec, this );
    }

    @Override
    public List<DexEditField> getFields(DexIncludedController<?> ec) {
        List<DexEditField> fields = new ArrayList<>();
        fields.add( new DexEditField( 0, 0, PARENT_LABEL, PARENT_TOOLTIP, getParentNode( ec ) ) );
        fields.add( new DexEditField( 1, 0, PARAM_GROUP_LABEL, PARAM_GROUP_TOOLTIP, getParameterGroupNode( ec ) ) );
        fields.add( new DexEditField( 2, 0, PATH_LABEL, PATH_LABEL, getPathNode( ec ) ) );
        return fields;
    }

    public Tooltip getTooltip() {
        return new Tooltip( TOOLTIP );
    }

    private static final String TOOLTIP = "Specifies a parent reference for a REST resource.";

    private static final String PARAM_GROUP_LABEL = "Parameter Group";
    private static final String PARAM_GROUP_TOOLTIP =
        "The name of the parameter group on the the parent resource with which this parent reference is associated.  The referenced group must be an ID parameter group.";
    private static final String PATH_LABEL = "Path Template";
    private static final String PATH_TOOLTIP =
        "Specifies the path template for the parent resource. This path will be pre-pended to all action path templates when the child resource is treated as a sub-resource (non-first class).";
    private static final String PARENT_LABEL = "Parent";
    private static final String PARENT_TOOLTIP = "Specifies a parent reference for a REST resource.";



}
