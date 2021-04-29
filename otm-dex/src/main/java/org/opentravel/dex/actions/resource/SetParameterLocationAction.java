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

package org.opentravel.dex.actions.resource;

import org.opentravel.dex.actions.string.DexStringAction;
import org.opentravel.model.OtmObject;
import org.opentravel.model.resource.OtmParameter;

public class SetParameterLocationAction extends DexStringAction {
    // private static Log log = LogFactory.getLog( SetAbstractAction.class );

    /**
     * @param subject
     * @return
     */
    public static boolean isEnabled(OtmObject subject) {
        if (subject instanceof OtmParameter)
            return subject.isEditable();
        return false;
    }

    public SetParameterLocationAction() {
        // Constructor for reflection
    }

    public String get() {
        return getSubject().getLocation() != null ? getSubject().getLocation().toString() : "";
    }

    @Override
    public OtmParameter getSubject() {
        return (OtmParameter) otm;
    }

    public void set(String value) {
        getSubject().setLocationString( value );
        getSubject().getParent().refresh();
    }

    @Override
    public boolean setSubject(OtmObject subject) {
        if (!(subject instanceof OtmParameter))
            return false;
        otm = subject;
        return true;
    }

    @Override
    public String toString() {
        return "Action facet set to " + get();
    }

}
