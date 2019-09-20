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
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentravel.model.OtmModelManager;
import org.opentravel.model.otmLibraryMembers.TestBusiness;
import org.opentravel.model.otmLibraryMembers.TestResource;
import org.opentravel.schemacompiler.model.TLProperty;
import org.opentravel.schemacompiler.model.TLPropertyOwner;

/**
 * Test class for Property Type Enumeration
 * <p>
 */
public class TestElement extends TestOtmPropertiesBase<OtmElement> {
    private static Log log = LogFactory.getLog( TestElement.class );

    @BeforeClass
    public static void beforeClass() {
        staticModelManager = new OtmModelManager( null, null );
        baseObject = TestBusiness.buildOtm( staticModelManager );
        testResource = TestResource.buildOtm( staticModelManager );

        log.debug( "Before class ran." );
    }

    @Test
    public void testChildren() {}



    /**
     * **********************************************************************************
     * 
     */
    /**
     * Build an action with one request and response.
     * 
     * @param resource
     * @return
     */
    public static OtmElement buildOtm() {
        return null;
    }

    public static TLProperty buildTL(TLPropertyOwner owner) {
        return null;
    }

    /**
     * Build an action with request that has parameter group added to resource
     * 
     * @param resource
     */
    public static OtmElement buildFullOtm(TLPropertyOwner owner) {
        return null;
    }

}
