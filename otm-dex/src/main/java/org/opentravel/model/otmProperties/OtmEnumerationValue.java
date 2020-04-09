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
import org.opentravel.model.OtmPropertyOwner;
import org.opentravel.model.otmLibraryMembers.OtmEnumeration;
import org.opentravel.schemacompiler.model.TLAbstractEnumeration;
import org.opentravel.schemacompiler.model.TLEnumValue;

/**
 * @author dmh
 *
 */
public class OtmEnumerationValue extends OtmValueProperty {
    private static Log log = LogFactory.getLog( OtmEnumerationValue.class );

    private OtmEnumeration<TLAbstractEnumeration> parent;

    public OtmEnumerationValue(TLEnumValue value, OtmEnumeration<TLAbstractEnumeration> parent) {
        super( value );
        this.parent = parent;
        if (parent != null)
            parent.add( this );
    }

    @Override
    public TLEnumValue getTL() {
        return (TLEnumValue) tlObject;
    }

    @Override
    public String setName(String name) {
        getTL().setLiteral( name );
        nameProperty().set( getName() ); // may not fire otm name change listener
        isValid( true );
        return getName();
    }

    @Override
    public String getName() {
        return getTL().getLiteral();
    }

    @Override
    public OtmEnumeration<TLAbstractEnumeration> getParent() {
        return parent;
    }

    @Override
    public boolean isInherited() {
        if (getTL() == null || getParent() == null)
            return false;
        return getTL().getOwningEnum() != getParent().getTL();
    }

    @Override
    public OtmPropertyOwner setParent(OtmPropertyOwner parent) {
        if (parent instanceof OtmEnumeration)
            this.parent = (OtmEnumeration<TLAbstractEnumeration>) parent;
        return parent;
    }

    @Override
    public void clone(OtmProperty property) {
        TLEnumValue newTL = new TLEnumValue();
        newTL.setLiteral( getTL().getLiteral() );
        // Create clone added to parent
        new OtmEnumerationValue( newTL, getParent() );
    }
}
