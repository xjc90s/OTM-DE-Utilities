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

import org.opentravel.model.OtmObject;
import org.opentravel.model.resource.OtmActionRequest;

public class SetRequestParameterGroupAction extends DexStringAction {
    // private static Log log = LogFactory.getLog( SetAbstractAction.class );

    /**
     * @param subject
     * @return
     */
    public static boolean isEnabled(OtmObject subject) {
        if (subject instanceof OtmActionRequest)
            return subject.isEditable();
        return false;
    }

    public SetRequestParameterGroupAction() {
        actionType = DexActions.SETREQUESTPARAMETERGROUP;
    }

    protected String get() {
        return getSubject().getParamGroupName();
    }

    @Override
    public OtmActionRequest getSubject() {
        return (OtmActionRequest) otm;
    }

    protected void set(String value) {
        getSubject().setParamGroupString( value );
    }

    @Override
    public boolean setSubject(OtmObject subject) {
        if (!(subject instanceof OtmActionRequest))
            return false;
        otm = subject;
        return true;
    }

    @Override
    public String toString() {
        return "Parameter group set to " + get();
    }

}
