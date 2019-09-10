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
import org.opentravel.dex.actions.DexActions;
import org.opentravel.model.OtmModelElement;
import org.opentravel.model.OtmObject;
import org.opentravel.model.OtmResourceChild;
import org.opentravel.model.OtmTypeProvider;
import org.opentravel.model.OtmTypeUser;
import org.opentravel.schemacompiler.model.TLExample;
import org.opentravel.schemacompiler.model.TLExampleOwner;
import org.opentravel.schemacompiler.model.TLMemberField;
import org.opentravel.schemacompiler.model.TLMemberFieldOwner;
import org.opentravel.schemacompiler.model.TLModelElement;
import org.opentravel.schemacompiler.model.TLParamLocation;
import org.opentravel.schemacompiler.model.TLParameter;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;

/**
 * OTM Object for Resource Action objects.
 * 
 * @author Dave Hollander
 * 
 */
public class OtmParameter extends OtmResourceChildBase<TLParameter> implements OtmResourceChild {
    private static Log log = LogFactory.getLog( OtmParameter.class );

    public OtmParameter(TLParameter tla, OtmParameterGroup parent) {
        super( tla, parent );

        // tla.getFieldRefName();
    }

    public OtmObject getFieldRef() {
        return OtmModelElement.get( (TLModelElement) getTL().getFieldRef() );
    }

    /**
     * 
     * @return the type assigned to the field reference or null
     */
    public OtmTypeProvider getFieldAssignedType() {
        return getFieldRef() instanceof OtmTypeUser ? ((OtmTypeUser) getFieldRef()).getAssignedType() : null;
    }
    // /**
    // *
    // * @return the Otm library member that owns the TLFieldRef()
    // */
    // public OtmLibraryMember getFieldOwner() {
    // OtmObject field = OtmModelElement.get( (TLModelElement) getTL().getFieldRef() );
    // return field != null ? field.getOwningMember() : null;
    // }

    @Override
    public Icons getIconType() {
        return ImageManager.Icons.RESOURCE_PARAMETER;
    }

    @Override
    public TLParameter getTL() {
        return (TLParameter) tlObject;
    }

    @Override
    public String getName() {
        return getTL().getFieldRefName();
    }

    @Override
    public List<DexEditField> getFields() {
        List<DexEditField> fields = new ArrayList<>();
        // fields.add( new DexEditField( 0, 0, FIELD_LABEL, FIELD_TOOLTIP, new ComboBox<String>() ) );
        fields.add( new DexEditField( 0, 0, LOCATION_LABEL, LOCATION_TOOLTIP, getLocationsNode() ) );
        return fields;
    }

    public boolean isPathParam() {
        return getTL().getLocation() == TLParamLocation.PATH;
    }

    /**
     * 
     * @return parameter name surrounded by brackets if path parameter or else empty string
     */
    public String getPathContribution() {
        return isPathParam() ? "{" + getName() + "}" : "";
    }

    @Override
    public OtmParameterGroup getParent() {
        return (OtmParameterGroup) parent;
    }

    public boolean isQueryParam() {
        return getTL().getLocation() == TLParamLocation.QUERY;
    }

    public String getQueryContribution(String prefix) {
        StringBuilder contribution = new StringBuilder();
        if (isQueryParam()) {
            contribution.append( prefix + getName() );

            // Add example if possible
            // String ex = "";
            String value = "xyz";
            TLMemberField<TLMemberFieldOwner> src = getTL().getFieldRef();
            List<TLExample> examples = null;
            if (src instanceof TLExampleOwner)
                examples = ((TLExampleOwner) src).getExamples();
            TLExample example = null;
            if (examples != null && !examples.isEmpty())
                example = examples.get( 0 );
            if (example != null)
                value = example.getValue();
            contribution.append( "=" + value );

            // PropertyNode n = null;
            // TLMemberField<?> field = ((TLParameter) param.getTLModelObject()).getFieldRef();
            // if (field instanceof TLModelElement)
            // n = (PropertyNode) getNode( ((TLModelElement) field).getListeners() );

            // List<TLExample> examples = ((TLParameter) param.getTLModelObject()).getExamples();
            // if (examples != null && !examples.isEmpty())
            // ex = examples.get( 0 ).getValue();
            // else if (n != null)
            // ex = n.getExample( null ); // Try to get example from the actual field being referenced.
            // if (ex.isEmpty())
            // ex = "xxx";
            // contribution.append( "=" + value);
        }
        return contribution.toString();
    }


    private ObservableList<String> getLocationCandidates() {
        ObservableList<String> locations = FXCollections.observableArrayList();
        for (TLParamLocation l : TLParamLocation.values())
            locations.add( l.toString() );
        return locations;
    }

    private Node getLocationsNode() {
        StringProperty selection =
            getActionManager().add( DexActions.SETPARAMETERLOCATION, getLocation().toString(), this );
        return DexEditField.makeComboBox( getLocationCandidates(), selection );
        // ComboBox<String> box = new ComboBox<>( getLocationCandidates() );
        // box.getSelectionModel().select( getTL().getLocation().toString() );
        // box.setEditable( getOwningMember().isEditable() );
        // return box;
    }


    public TLParamLocation setLocation(TLParamLocation location) {
        getTL().setLocation( location );
        log.debug( "Set loction to " + location );
        return getLocation();
    }

    public TLParamLocation setLocationString(String value) {
        TLParamLocation location = null;
        for (TLParamLocation c : TLParamLocation.values())
            if (c.toString().equals( value ))
                location = c;
        return setLocation( location );
    }

    public Tooltip getTooltip() {
        return new Tooltip( TOOLTIP );
    }

    private static final String TOOLTIP =
        "Provides a reference to a property, attribute or element that should be used as a parameter in a REST request message.Tip: If you want to return a 404 (not found) error when the parameter value does not correspond to an existing resource then use a PATH parameter.";
    // private static final String FIELD_LABEL = "Field";
    // private static final String FIELD_TOOLTIP = "Name of the field to be used as a REST request parameter. ";
    private static final String LOCATION_LABEL = "Location";
    private static final String LOCATION_TOOLTIP = "Specifies the location of the parameter in the REST request. ";

    public TLParamLocation getLocation() {
        return getTL().getLocation();
    }

    /**
     * @return
     */
    public StringProperty locationProperty() {
        String location = "";
        if (getLocation() != null)
            location = getLocation().toString();
        return new ReadOnlyStringWrapper( location );
    }

    /**
     * @return the object this parameter references
     */
    public StringProperty typeProperty() {
        OtmObject ref = OtmModelElement.get( (TLModelElement) getTL().getFieldRef() );
        String type = "";
        if (ref instanceof OtmTypeUser)
            type = ((OtmTypeUser) ref).getTlAssignedTypeName();
        return new ReadOnlyStringWrapper( type );
    }


}
