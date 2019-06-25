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

package org.opentravel.model.otmProperties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.common.ImageManager;
import org.opentravel.common.ImageManager.Icons;
import org.opentravel.model.OtmModelElement;
import org.opentravel.model.otmLibraryMembers.OtmEnumeration;
import org.opentravel.schemacompiler.model.TLAbstractEnumeration;
import org.opentravel.schemacompiler.model.TLEnumValue;

/**
 * @author dmh
 *
 */
public class OtmEnumerationValue extends OtmModelElement<TLEnumValue> {
    private static Log log = LogFactory.getLog( OtmEnumerationValue.class );

    private OtmEnumeration<TLAbstractEnumeration> parent;

    public OtmEnumerationValue(TLEnumValue value, OtmEnumeration<TLAbstractEnumeration> parent) {
        super( value, parent.getActionManager() );
        this.parent = parent;
    }

    @Override
    public Icons getIconType() {
        return ImageManager.Icons.ENUMERATION_VALUE;
    }

    @Override
    public String setName(String name) {
        tlObject.setLiteral( name );
        nameProperty().set( getName() ); // may not fire otm name change listener
        isValid( true );
        log.debug( "Set name to: " + getName() );
        return getName();
    }

    @Override
    public OtmEnumeration<TLAbstractEnumeration> getOwningMember() {
        return parent;
    }

    @Override
    public String getName() {
        return tlObject.getLiteral();
    }

    // /**
    // * WILL NOT WORK
    // * {@inheritDoc} Determined by testing parent's children list.
    // */
    // @Override
    // public boolean isInherited() {
    // return parent != null ? (parent.getChildren().contains(this)) : false;
    // }
    // @Override
    // public TLEnumValue getTL() {
    // return tlObject;
    // }
}
