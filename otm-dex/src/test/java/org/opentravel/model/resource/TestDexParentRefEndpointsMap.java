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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentravel.model.OtmModelManager;
import org.opentravel.model.otmFacets.OtmIdFacet;
import org.opentravel.model.otmLibraryMembers.OtmBusinessObject;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.model.otmLibraryMembers.TestBusiness;
import org.opentravel.model.otmLibraryMembers.TestResource;
import org.opentravel.schemacompiler.model.TLParamLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Test class for the map handler.
 * <p>
 * Note: the GUI uses action.getEndpointURL()
 */
public class TestDexParentRefEndpointsMap extends TestOtmResourceBase<OtmAction> {
    private static Log log = LogFactory.getLog( TestDexParentRefEndpointsMap.class );

    @BeforeClass
    public static void beforeClass() {
        staticModelManager = new OtmModelManager( null, null );
        baseObject = TestBusiness.buildOtm( staticModelManager );
        testResource = TestResource.buildOtm( staticModelManager );

        subject = null;
        log.debug( "Before class ran." );
    }


    @Test
    public void testConstructor() {
        DexParentRefsEndpointMap endpoints = new DexParentRefsEndpointMap( testResource );
        endpoints.print();

        OtmResource resource = TestResource.buildFullOtm( "MySubjectPath", "MySubject", staticModelManager );
        endpoints = new DexParentRefsEndpointMap( resource );
        endpoints.print();
    }

    @Test
    public void testSystemContribution() {
        String path1 = "/MyCollection";
        String subjectName = "MySubject";
        OtmResource resource = TestResource.buildFullOtm( path1, subjectName, staticModelManager );
        String rc = DexParentRefsEndpointMap.getResourceContribution( resource );

        assertTrue( !DexParentRefsEndpointMap.getSystemContribution( null ).isEmpty() );
    }

    @Test
    public void testActionGetEndpointURL() {
        // The GUI uses action to present paths. One for the action and one for each parent refs
        // Action concatenates the results of two calls:
        // path.append( DexParentRefsEndpointMap.getResourceBaseURL( this.getOwningMember() ) );
        // path.append( DexParentRefsEndpointMap.getActionContribution( this ) );

        // Givens
        String subPath1 = "/Passengers";
        String subName = "Passenger";
        // String parentPathTemplate = "/PaxPathTemplate";
        OtmResource resource = TestResource.buildFullOtm( subPath1, subName, staticModelManager );

        // Create parent resource
        String subjectName = "Reservation";
        OtmResource parent = TestResource.buildParentResource( resource, subjectName, staticModelManager );

        List<OtmAction> actions = resource.getActions();
        assertTrue( "Given: ", !actions.isEmpty() );

        String rbURL = DexParentRefsEndpointMap.getResourceBaseURL( resource );
        assertTrue( "Must not end in /", !rbURL.endsWith( "/" ) );

        String ac;
        String aURL;

        for (OtmAction a : actions) {
            a.getRequest().setPathTemplate( "/{id}", false );
            ac = DexParentRefsEndpointMap.getActionContribution( a );
            aURL = rbURL + ac;
            log.debug( aURL );
            assertTrue( "Must not have double /.", !aURL.substring( 8 ).contains( "//" ) );
            // FIXME - make the base path on resource the collection and do NOT add to action contribution
        }
    }

    @Test
    public void testActionContribution() {
        // Givens
        String subPath1 = "/Passengers";
        String subName = "Passenger";
        // String parentPathTemplate = "/PaxPathTemplate";
        OtmResource resource = TestResource.buildFullOtm( subPath1, subName, staticModelManager );
        resource.getActions().forEach( a -> TestAction.check( a ) );

        // Create parent resource
        String subjectName = "Reservation";
        OtmResource parent = TestResource.buildParentResource( resource, subjectName, staticModelManager );
        parent.getActions().forEach( a -> TestAction.check( a ) );

        OtmParameterGroup idGroup = TestParamGroup.buildIdGroup( resource );
        assertTrue( "Given: ", idGroup.isIdGroup() );
        idGroup.getParameters().forEach( p -> p.setLocation( TLParamLocation.PATH ) );
        idGroup.getParameters().forEach( p -> log.debug( "Param = " + p ) );
        OtmParameter pathParam = idGroup.getParameters().get( 0 );
        assertTrue( "Given: ", pathParam.getLocation() == TLParamLocation.PATH );
        String paramName = pathParam.getFieldRefName();

        String ac;
        String cc, ppc;

        // Null action
        ac = DexParentRefsEndpointMap.getActionContribution( null );
        assertTrue( "Must be empty string.", ac.isEmpty() );

        // Null actionRequest - this never happens and will cause failures
        // comment out if you really need to test behavior with no request
        // OtmAction aNoRQ = new OtmAction( new TLAction(), resource );
        // assertTrue( "Given:", aNoRQ.getRequest() == null );
        // ac = DexParentRefsEndpointMap.getActionContribution( aNoRQ );
        // assertTrue( "Must be empty string.", ac.isEmpty() );

        String rqTemplatePrefix = "Fishes"; // Note non-standard plural
        String rqTemplate1 = "/{id}";
        String rqTemplate2 = rqTemplatePrefix + "{id}";
        String rqTemplate3 = "/" + rqTemplatePrefix + "/{id}";

        List<OtmAction> actions = new ArrayList<>( resource.getActions() );
        actions.addAll( parent.getActions() );
        assertTrue( "Given: ", !actions.isEmpty() );
        actions.forEach( a -> TestAction.check( a ) );

        for (OtmAction a : actions) {
            assertTrue( "Given: ", a.getRequest() != null );
            a.getRequest().setParamGroup( idGroup );
            assertTrue( "Given: ", a.getRequest().getParamGroup() == idGroup );

            // When - path template is null
            a.getRequest().setPathTemplate( null, false );
            // Then - collection contribution is plural of subject name
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            assertTrue( "Must be empty:", cc.isEmpty() );
            // assertTrue( "Must not have :", !cc.contains( ":" ) );
            // assertTrue( "Must not have subject name.", !cc.contains( a.getOwningMember().getSubject().getName() ) );

            // When - path template is empty
            a.getRequest().setPathTemplate( "", false );
            // Then - collection contribution is plural of subject name
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            assertTrue( "Must be empty:", cc.isEmpty() );
            // assertTrue( "Must not have subject name.", !cc.contains( a.getOwningMember().getSubject().getName() ) );

            // When - path template is set to / + parameter
            a.getRequest().setPathTemplate( rqTemplate1, false );
            // Then - collection contribution portion of template without parameters
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            assertTrue( "Must be empty:", cc.isEmpty() );
            // assertTrue( "Must not have subject name.", !cc.contains( a.getOwningMember().getSubject().getName() ) );

            // When - path template is set to prefix + parameter
            a.getRequest().setPathTemplate( rqTemplate2, false );
            // Then - collection contribution portion of template without parameters
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            assertTrue( "Must have template prefix.", cc.contains( rqTemplatePrefix ) );

            // When - path template is set to / + prefix + parameter
            a.getRequest().setPathTemplate( rqTemplate3, false );
            // Then - collection contribution portion of template without parameters
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            assertTrue( "Must have template prefix.", cc.contains( rqTemplatePrefix ) );

            ac = DexParentRefsEndpointMap.getActionContribution( a );
            cc = DexParentRefsEndpointMap.getCollectionContribution( a );
            ppc = DexParentRefsEndpointMap.getPathParameterContributions( a.getRequest() );
            assertTrue( ppc.contains( paramName ) );
            assertTrue( "Must start with slash", ac.startsWith( "/" ) );
            assertTrue( "Must not have double /.", !ac.contains( "//" ) );
            //
            log.debug( ac + " = " + cc + " + " + ppc );
        }
    }


    @Test
    public void testParentContribution() {

        String path1 = "/Reservations";
        String subjectName = "Reservation";
        OtmResource parent = TestResource.buildFullOtm( path1, subjectName, staticModelManager );

        String subPath1 = "/Passengers";
        String subName = "Passenger";
        String parentPathTemplate = "/PaxPathTemplate";
        OtmResource resource = TestResource.buildFullOtm( subPath1, subName, staticModelManager );
        OtmParentRef parentRef = resource.add( null, parent );
        String pt = parentRef.getPathTemplate();

        String pc;
        // Get the class containing the map and access methods
        DexParentRefsEndpointMap mapObject = resource.getParentRefEndpointsMap();
        assertTrue( "Given: ", mapObject != null );

        // Get the actual map from the object
        Map<OtmParentRef,String> parentRefMap = mapObject.get();
        assertTrue( "Given: ", parentRefMap != null );

        // Then - with one parent
        Collection<String> values = parentRefMap.values();
        assertTrue( "Only one parent to contribute.", values.size() == 1 );
        assertTrue( "Must contain parent path.", values.contains( path1 ) );

        // When - parent ref has template value
        parentRef.setPathTemplate( parentPathTemplate );
        values = mapObject.get().values();
        assertTrue( "Must contain parentRef path template.", values.contains( parentPathTemplate ) );

        // Add parent again
        resource.add( resource.getParentRefs().get( 0 ).getTL(), parent );
        assertTrue( "Only one parent to contribute.", mapObject.get().values().size() == 1 );

        // Add a new parent
        String path2 = "/Manifests";
        String subjectName2 = "Manifest";
        OtmResource parent2 = TestResource.buildFullOtm( path2, subjectName2, staticModelManager );
        resource.add( null, parent2 );
        assertTrue( "Only one parent to contribute.", mapObject.get().values().size() == 2 );

        // When - lots of parents, grand-parents and great-grand-parents are added
        ArrayList<OtmParentRef> parents = new ArrayList<>();
        String collection = "Collection";
        String subject = "Subject";
        String gpPathTemplte = "GpPathTemplate";
        for (int i = 1; i < 20; i++) {
            // Add parent
            OtmResource p = TestResource.buildFullOtm( collection + i, subject + i, staticModelManager );
            parents.add( resource.add( null, p ) );
            // Add grandparent with template
            OtmResource gp = TestResource.buildFullOtm( collection + i + "g", subject + i + "g", staticModelManager );
            OtmParentRef gpRef = p.add( null, gp );
            parents.add( gpRef );
            gpRef.setPathTemplate( gpPathTemplte + i );
            assertTrue( mapObject.get( gp ).contains( "/" ) );
            assertTrue( mapObject.get( gp ).contains( gpPathTemplte ) );
            // Add great grandparent
            OtmResource ggp =
                TestResource.buildFullOtm( collection + i + "gg", subject + i + "gg", staticModelManager );
            parents.add( gp.add( null, ggp ) );
            assertTrue( mapObject.get( ggp ).contains( "/" + mapObject.get( gp ) ) );
            assertTrue( mapObject.get( ggp ).contains( gpPathTemplte ) );
        }
        // log.debug( mapObject.get().values() );

        // Then - There will be on parent contribution for each parent ref
        for (OtmParentRef pr : parents) {
            // log.debug( mapObject.get( pr ) );
            assertTrue( mapObject.get( pr ).contains( collection ) );
        }

        // TODO - Remove parents and grandparents
        // TODO - non-first class resources?
        // TODO - ID groups on the parents and grandparents. Should be OK, needs testing of getEndpointPath(parentRef)
    }

    // getEndpointPath is used in constructing parent contributions
    @Test
    public void testGetEndpointPathWithParentRef() {
        // String path1 = "/Reservations";
        // OtmResource parent = TestResource.buildFullOtm( path1, subjectName, staticModelManager );

        String subPath1 = "/Passengers";
        String subName = "Passenger";
        String parentPathTemplate = "/PaxPathTemplate";
        OtmResource resource = TestResource.buildFullOtm( subPath1, subName, staticModelManager );

        // Create parent resource
        String subjectName = "Reservation";
        OtmResource parent = TestResource.buildParentResource( resource, subjectName, staticModelManager );
        OtmBusinessObject parentSubject = parent.getSubject();
        OtmIdFacet parentIdFacet = parentSubject.getIdFacet();
        List<OtmParameterGroup> pGroups = parent.getParameterGroups();
        OtmParameterGroup idGroup = null;
        for (OtmParameterGroup g : pGroups)
            if (g.isIdGroup())
                idGroup = g;
        OtmParentRef parentRef = resource.getParentRefs().get( 0 );
        parentRef.setParameterGroup( null );
        assertTrue( "Given: ", parentIdFacet != null );
        assertTrue( "Given: ", parentSubject != null );
        assertTrue( "Given: ", parentRef != null );
        assertTrue( "Given: ", idGroup != null );

        String ep;

        // When - template is set
        parentRef.setPathTemplate( parentPathTemplate );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );
        assertTrue( "Must have template.", ep.contains( parentPathTemplate ) );
        assertTrue( "Must not have parameters.", !ep.contains( "{" ) );

        // When - template is empty
        parentRef.setPathTemplate( "" );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );
        assertTrue( "Must have path .", ep.contains( subjectName ) );
        assertTrue( "Must not have parameters.", !ep.contains( "{" ) );

        // When - template is null
        parentRef.setPathTemplate( null );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );
        assertTrue( "Must have path.", ep.contains( subjectName ) );

        // When - null template + no subject + no id group
        parent.setSubject( null );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );

        // When - null template + no id group
        parent.setSubject( parentSubject );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );
        assertTrue( "Must have path .", ep.contains( subjectName ) );
        assertTrue( "Must not have parameters.", !ep.contains( "{" ) );

        // When - null template + id group
        parentRef.setParameterGroup( idGroup );
        ep = DexParentRefsEndpointMap.getEndpointPath( parentRef );
        assertTrue( "Must have path .", ep.contains( subjectName ) );
        assertTrue( "Must have parameters.", ep.contains( "{" ) );
        assertTrue( "Must have parameters.", ep.contains( idGroup.getPathParameters().get( 0 ).getName() ) );

        // TODO - no path parameters
    }

    @Test
    public void testResourceContribution() {
        String path1 = "MyCollection";
        String path2 = "/" + path1;
        String path3 = path2 + "/";
        String emptyPath = "/";

        String subjectName = "MySubject";
        OtmResource resource = TestResource.buildFullOtm( path1, subjectName, staticModelManager );
        String rc = DexParentRefsEndpointMap.getResourceContribution( resource );

        // When - path set to null
        resource.setBasePath( null );
        rc = DexParentRefsEndpointMap.getResourceContribution( resource );
        assertTrue( rc.equals( emptyPath ) );
        // When - empty
        resource.setBasePath( "" );
        rc = DexParentRefsEndpointMap.getResourceContribution( resource );
        assertTrue( rc.equals( emptyPath ) );

        // When - variations of starting and ending with slash
        resource.setBasePath( path1 );
        rc = DexParentRefsEndpointMap.getResourceContribution( resource );
        assertTrue( rc.contains( path1 ) );
        testStartsAndEnds( rc, path1 );

        resource.setBasePath( path2 );
        rc = DexParentRefsEndpointMap.getResourceContribution( resource );
        assertTrue( rc.contains( path1 ) );
        testStartsAndEnds( rc, path1 );

        resource.setBasePath( path3 );
        rc = DexParentRefsEndpointMap.getResourceContribution( resource );
        testStartsAndEnds( rc, path1 );
    }

    public static void testStartsAndEnds(String value, String name) {
        assertTrue( "Starts error", value.startsWith( DexParentRefsEndpointMap.PATH_SEPERATOR ) );
        assertTrue( "Ends error", !value.endsWith( DexParentRefsEndpointMap.PATH_SEPERATOR ) );
        assertTrue( "Contains error", value.contains( name ) );
    }

    @Test
    public void testParentRefPaths() {
        // DexActionEndpointMap endpoints = null;
        // Given - one resource
        String resourcePath = "MyCollection";
        String subjectName = "MySubject";
        OtmResource resource = TestResource.buildFullOtm( resourcePath, subjectName, staticModelManager );
        check( resource, 0 );

        // When - multiple levels and multiple parents
        OtmResource parent1 = TestResource.buildParentResource( resource, "Parent1", staticModelManager );
        check( resource, 1 );
        OtmResource gp1 = TestResource.buildParentResource( parent1, "GP_Parent1", staticModelManager );
        check( parent1, 1 );
        check( resource, 2 );
        OtmResource parent2 = TestResource.buildParentResource( resource, "Parent2", staticModelManager );
        OtmResource gp2 = TestResource.buildParentResource( parent2, "GP_Parent2", staticModelManager );
        OtmResource ggp2 = TestResource.buildParentResource( gp2, "GGP_Parent2", staticModelManager );
        check( resource, 5 );
        check( parent2, 2 );
        check( gp2, 1 );
        check( ggp2, 0 );
        OtmResource parent3 = TestResource.buildParentResource( resource, "Parent3", staticModelManager );
        check( resource, 6 );
        // When - an ancestor is made not first class
        gp2.setFirstClass( false );
        // Then - it no longer has its own path
        check( resource, 5 );
        // log.debug( "done" );
    }

    /**
     * NOTE -- use the log print to visually check
     */
    public static void check(OtmResource resource, int expectedPathCount) {
        String pathTemplate = "ThisIsFromParentRefTemplate_";
        String initialTemplate = null;
        DexParentRefsEndpointMap endpoints = resource.getParentRefEndpointsMap();

        // Then - we have the expected number of paths
        log.debug( "***Testing endpoint paths on " + resource );
        endpoints.print();
        assertTrue( resource.getAllParentRefs( true ).size() == expectedPathCount );
        assertTrue( endpoints.size() == expectedPathCount );


        // Then - if there are no parentRefs, then the map is empty
        if (resource.getParentRefs().isEmpty()) {
            assertTrue( endpoints.size() == 0 );
            assertTrue( endpoints.get( resource ).isEmpty() );
        } else {
            // Then - all 1st class parentRefs have an entry in the map
            for (OtmParentRef pr : resource.getAllParentRefs( true )) {
                // for (OtmParentRef pr : resource.getParentRefs()) {
                assertFalse( endpoints.get( pr ).isEmpty() );
                assertFalse( endpoints.get( pr.getParentResource() ).isEmpty() );
                initialTemplate = pr.getPathTemplate();

                // When - the template is empty
                pr.setPathTemplate( null );
                endpoints.build().print();
                // Then - the path contains the parent resource's base path
                assertTrue( endpoints.get( pr ).contains( pr.getParentResource().getBasePath() ) );

                // When - the template is set
                pr.setPathTemplate( pathTemplate + pr.getName() );
                endpoints.build().print();
                // Then - the path contains the resource path
                assertTrue( endpoints.get( pr ).contains( pathTemplate ) );

                pr.setPathTemplate( initialTemplate ); // Restore the template
            }
        }
    }
}
