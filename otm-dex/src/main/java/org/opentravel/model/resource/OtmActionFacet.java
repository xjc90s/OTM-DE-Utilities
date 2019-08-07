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
import org.opentravel.model.OtmModelElement;
import org.opentravel.model.OtmObject;
import org.opentravel.model.OtmResourceChild;
import org.opentravel.model.otmFacets.OtmContributedFacet;
import org.opentravel.model.otmFacets.OtmFacet;
import org.opentravel.model.otmFacets.OtmQueryFacet;
import org.opentravel.model.otmLibraryMembers.OtmBusinessObject;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.schemacompiler.model.NamedEntity;
import org.opentravel.schemacompiler.model.TLActionFacet;
import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemacompiler.model.TLModelElement;
import org.opentravel.schemacompiler.model.TLReferenceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;

/**
 * OTM Object for Resource objects.
 * 
 * @author Dave Hollander
 * 
 */
public class OtmActionFacet extends OtmResourceChildBase<TLActionFacet> implements OtmResourceChild {
    private static Log log = LogFactory.getLog( OtmActionFacet.class );

    public OtmActionFacet(TLActionFacet tla, OtmResource parent) {
        super( tla, parent );

        // tla.getReferenceFacetName(); -
        // tla.getBasePayloadName();
        // tla.getReferenceRepeat();
        // tla.getReferenceType();
    }

    @Override
    public Icons getIconType() {
        return ImageManager.Icons.FACET;
    }

    public OtmActionFacet(String name, OtmResource parent) {
        super( new TLActionFacet(), parent );
        setName( name );
    }

    @Override
    public String setName(String name) {
        getTL().setName( name );
        isValid( true );
        return getName();
    }

    @Override
    public TLActionFacet getTL() {
        return (TLActionFacet) tlObject;
    }


    // @Override
    public Collection<OtmObject> getChildrenHierarchy() {
        Collection<OtmObject> ch = new ArrayList<>();
        ch.add( OtmModelElement.get( (TLModelElement) getTL().getBasePayload() ) );
        return ch;
    }

    // public static final String SUBGRP = "Substitution Group";

    public String getReferenceFacetName() {
        return getTL().getReferenceFacetName() != null ? getTL().getReferenceFacetName() : "";
    }

    /**
     * If set to null, then the object is used instead of facet (substitution group)
     * 
     * @param facet
     */
    public void setReferenceFacet(OtmFacet<?> facet) {
        if (facet == null)
            getTL().setReferenceFacetName( OtmResource.SUBGROUP );
        else {
            TLFacetType type = facet.getTL().getFacetType();
            getTL().setReferenceFacetName( type.getIdentityName() );
        }

        // getTL().setReferenceFacetName( facet.getName() );
    }

    public TLReferenceType getReferenceType() {
        return getTL().getReferenceType() != null ? getTL().getReferenceType() : TLReferenceType.NONE;
    }

    public void setReferenceType(TLReferenceType type) {
        if (type == null)
            type = TLReferenceType.NONE;
        getTL().setReferenceType( type );
    }

    public OtmObject getRequestPayload() {
        // For a request, the "payloadType" indicates which action facet.
        // This action facet defines the root element.
        OtmObject rqPayload = null;
        String rfName = getReferenceFacetName();
        OtmBusinessObject subject = getOwningMember().getSubject();
        OtmLibraryMember base = getBasePayload(); // creates wrapper object

        // Early Exit Conditions
        if (subject == null)
            return rqPayload; // Error, resource is not complete
        if (rfName.equals( OtmResource.SUBGROUP ))
            return subject; // The whole object is the substitution group
        if (getReferenceType() == TLReferenceType.NONE)
            return null; // User set to have No request payload

        // FIXME - support base payloads

        if (getReferenceType() != TLReferenceType.NONE) {
            rqPayload = subject;
            // Try to find the matching facet
            for (OtmObject c : subject.getChildren()) {
                if (c instanceof OtmContributedFacet) {
                    // TODO - use a better way to find the query facet name?
                    String cfName = ((OtmContributedFacet) c).getTL().getName();
                    if (((OtmContributedFacet) c).getContributor() instanceof OtmQueryFacet)
                        cfName = "Query_" + cfName;
                    if (cfName.equals( rfName )) {
                        rqPayload = c;
                        break;
                    }
                }
                String type = ((OtmFacet<?>) c).getTL().getFacetType().getIdentityName();
                if (type.equals( rfName )) {
                    rqPayload = c;
                    break;
                }
            }
        }
        // if ((getTL().getReferenceType() != TLReferenceType.NONE) && !facetSelection.getFacetNames().isEmpty()) {
        // TLBusinessObject boRef = getTL().getOwningResource().getBusinessObjectRef();
        // }
        return rqPayload;
    }

    /**
     * @return all TL Modeled strings or for abstract resources just the first string (none)
     */
    private ObservableList<String> getReferenceTypeCandidates() {
        ObservableList<String> candidates = FXCollections.observableArrayList();
        if (getOwningMember().isAbstract())
            candidates.add( TLReferenceType.values()[0].toString() );
        else
            for (TLReferenceType value : TLReferenceType.values())
                candidates.add( value.toString() );
        return candidates;
    }

    private Node getReferenceTypeNode() {
        String selection = TLReferenceType.values()[0].toString();
        if (getReferenceType() != null)
            selection = getReferenceType().toString();
        ComboBox<String> box = DexEditField.makeComboBox( getReferenceTypeCandidates(), selection, this );
        box.setOnAction( a -> log.debug( "Type Selected" ) );
        return box;
    }

    /**
     * List of facet names on subject and entry for the substitution group
     * 
     * @return list of facets on the subject business object
     */
    private ObservableList<String> getReferenceFacetCandidates() {
        ObservableList<String> candidates = FXCollections.observableArrayList();
        if (getOwningMember() != null)
            getOwningMember().getSubjectFacets().forEach( f -> candidates.add( f.getName() ) );
        candidates.add( OtmResource.SUBGROUP );
        return candidates;
    }

    private Node getReferenceFacetNode() {
        String selection = OtmResource.SUBGROUP;
        if (!getReferenceFacetName().isEmpty())
            selection = getReferenceFacetName();
        ComboBox<String> box = DexEditField.makeComboBox( getReferenceFacetCandidates(), selection, this );
        box.setOnAction( a -> log.debug( "Reference Facet Selected" ) );
        return box;
    }


    private Node getBasePayloadNode() {
        String name = OtmResource.NONE;
        if (getBasePayload() != null)
            name = getBasePayload().getNameWithPrefix();
        Button button = new Button( name );
        button.setDisable( !isEditable() );
        button.setOnAction( a -> log.debug( "Base Payload Button selected" ) );
        return button;
    }

    private Node getRepeatCountNode() {
        Spinner<Integer> spinner = new Spinner<>( 0, 10000, 0 );
        spinner.setDisable( !isEditable() );
        spinner.setOnRotate( a -> log.debug( "Spinner selected" ) );
        return spinner;
    }

    @Override
    public List<DexEditField> getFields() {
        List<DexEditField> fields = new ArrayList<>();
        fields.add( new DexEditField( 0, 0, BASE_PAYLOAD_LABEL, BASE_PAYLOAD_TOOLTIP, getBasePayloadNode() ) );
        fields.add( new DexEditField( 0, 2, null, "Remove base payload.", new Button( "-Remove-" ) ) );
        fields.add( new DexEditField( 1, 0, REFERENCE_TYPE_LABEL, REFERENCE_TYPE_TOOLTIP, getReferenceTypeNode() ) );
        fields.add( new DexEditField( 2, 0, REFERENCE_FACET_LABEL, REFERENCE_FACET_TOOLTIP, getReferenceFacetNode() ) );
        fields.add( new DexEditField( 3, 0, REPEAT_COUNT_LABEL, REPEAT_COUNT_TOOLTIP, getRepeatCountNode() ) );
        return fields;
    }

    public OtmLibraryMember getBasePayload() {
        NamedEntity pl = getTL().getBasePayload();
        // Could be Choice or Core
        // FIXME
        return null;
    }

    public Tooltip getTooltip() {
        return new Tooltip( TOOLTIP );
    }

    private static final String TOOLTIP =
        "            Action facets describe the message payload for RESTful action requests and responses.  In addition to their own payload, they provide basic information about how the resource's business object should be referenced in the message.";

    private static final String REFERENCE_FACET_LABEL = "Reference Facet";
    private static final String REFERENCE_FACET_TOOLTIP =
        "Specifies the name of the business object facet to be referenced in the message.  If the Reference Type value is None this value will be ignored. ";
    private static final String REPEAT_COUNT_LABEL = "Repeat Count";
    private static final String REPEAT_COUNT_TOOLTIP =
        "Specifies the maximum number of times that the business object reference should repeat in the message. Best practices state that this string value should contain a positive number that is greater than or equal to 1. ";
    private static final String REFERENCE_TYPE_LABEL = "Reference Type";
    private static final String REFERENCE_TYPE_TOOLTIP = "Reference type";
    private static final String BASE_PAYLOAD_LABEL = "Base Payload";
    private static final String BASE_PAYLOAD_TOOLTIP =
        " Optional reference to a core or choice object that indicates the basic structure of the message payload. If the 'referenceType' value is NONE, this will indicate the entirity of the message structure.  For reference type values other than NONE, the message structure will include all elements of the base payload, plus reference(s) to the owning resource's business ";

}
