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

package org.opentravel.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.dex.action.manager.DexActionManager;
import org.opentravel.dex.action.manager.DexMinorVersionActionManager;
import org.opentravel.dex.action.manager.DexReadOnlyActionManager;
import org.opentravel.dex.controllers.DexStatusController;
import org.opentravel.dex.controllers.popup.DialogBoxContoller;
import org.opentravel.dex.tasks.TaskResultHandlerI;
import org.opentravel.dex.tasks.model.TypeResolverTask;
import org.opentravel.dex.tasks.model.ValidateModelManagerItemsTask;
import org.opentravel.model.otmContainers.OtmBuiltInLibrary;
import org.opentravel.model.otmContainers.OtmLibrary;
import org.opentravel.model.otmContainers.OtmProject;
import org.opentravel.model.otmLibraryMembers.OtmContextualFacet;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMemberFactory;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.model.otmLibraryMembers.OtmXsdSimple;
import org.opentravel.schemacompiler.ic.ModelIntegrityChecker;
import org.opentravel.schemacompiler.model.AbstractLibrary;
import org.opentravel.schemacompiler.model.BuiltInLibrary;
import org.opentravel.schemacompiler.model.LibraryMember;
import org.opentravel.schemacompiler.model.TLLibrary;
import org.opentravel.schemacompiler.model.TLModel;
import org.opentravel.schemacompiler.model.TLModelElement;
import org.opentravel.schemacompiler.repository.Project;
import org.opentravel.schemacompiler.repository.ProjectItem;
import org.opentravel.schemacompiler.repository.ProjectManager;
import org.opentravel.schemacompiler.repository.RepositoryManager;
import org.opentravel.schemacompiler.repository.impl.BuiltInProject;
import org.opentravel.schemacompiler.saver.LibrarySaveException;
import org.opentravel.schemacompiler.version.VersionChain;
import org.opentravel.schemacompiler.version.VersionChainFactory;
import org.opentravel.schemacompiler.version.VersionSchemeException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;

/**
 * Manage access to all objects in scope.
 * 
 * @author dmh
 *
 */
public class OtmModelManager implements TaskResultHandlerI {
    private static Log log = LogFactory.getLog( OtmModelManager.class );

    // Open projects - projectName and otmProject
    private Map<String,OtmProject> projects = new HashMap<>();

    // Map of base namespaces with all managed libraries in that namespace
    private Map<String,VersionChain<TLLibrary>> baseNSManaged = new HashMap<>();
    private Map<String,OtmLibrary> baseNSUnmanaged = new HashMap<>();

    // Open libraries - Abstract Libraries are built-in and user
    private Map<AbstractLibrary,OtmLibrary> libraries = new HashMap<>();

    // All members - Library Members are TLLibraryMembers and contextual facets
    private Map<LibraryMember,OtmLibraryMember> members = new HashMap<>();

    private DexActionManager readOnlyActionManager = new DexReadOnlyActionManager();
    private DexActionManager minorActionManager;
    private DexActionManager fullActionManager;

    private DexStatusController statusController;
    private ProjectManager projectManager;
    // private RepositoryManager repositoryManager;
    private TLModel tlModel = null;

    public static final String OTA_LIBRARY_NAMESPACE = "http://www.opentravel.org/OTM/Common/v0";
    public static final String OTA_EMPTY_NAME = "Empty";

    private DialogBoxContoller dialogBox = null;
    private boolean showingError = false;



    /**
     * Create a model manager.
     * 
     * @param fullActionManager edit-enabled action manager to assign to all members. If null, read-only action manager
     *        will be used.
     * @param controller
     */
    public OtmModelManager(DexActionManager fullActionManager, RepositoryManager repositoryManager) {
        // Create a TL Model to manager
        try {
            tlModel = new TLModel();
        } catch (Exception e) {
            log.info( "Exception creating new model: " + e.getLocalizedMessage() );
        }
        // Tell model to track changes to maintain its type integrity
        tlModel.addListener( new ModelIntegrityChecker() );

        // Create a master project manager
        if (repositoryManager != null)
            projectManager = new ProjectManager( tlModel, true, repositoryManager );
        else
            projectManager = new ProjectManager( tlModel );
        // this.repositoryManager = repositoryManager;

        // Main controller will pass one if enabled by settings
        if (fullActionManager == null) {
            this.fullActionManager = new DexReadOnlyActionManager();
            minorActionManager = new DexReadOnlyActionManager();
        } else {
            this.fullActionManager = fullActionManager;
            minorActionManager = new DexMinorVersionActionManager( fullActionManager );
        }
        // Set main controller for warnings if one is provided.
        readOnlyActionManager.setMainController( this.fullActionManager.getMainController() );
        minorActionManager.setMainController( this.fullActionManager.getMainController() );

        // log.debug( "Model Manager constructor complete." );
    }


    /**
     * Simply add the member to the maps if it is not already in the map. See {@link OtmLibrary#add(OtmLibraryMember)}
     * to add to both TL library and manager.
     * 
     * @param member
     */
    public void add(OtmLibraryMember member) {
        if (member != null && member.getTL() instanceof LibraryMember && !contains( member.getTlLM() ))
            members.put( member.getTlLM(), member );
    }

    /**
     * Simply remove the member from the map. To delete a member use {@link OtmLibrary#delete(OtmLibraryMember)}
     * 
     * @param member
     */
    public void remove(OtmLibraryMember member) {
        if (member != null && member.getTL() instanceof LibraryMember && contains( member.getTlLM() ))
            members.remove( member.getTlLM(), member );
    }

    // /**
    // * Delete this member. Remove from the maps, children lists and TL library.
    // *
    // * @param member
    // */
    // public void delete(OtmLibraryMember member) {
    // member.getLibrary().delete( member );
    // remove( member );
    // }

    /**
     * Add the TL library to the model if it is not already in the model. Adds all the members.
     * <p>
     * Does <b>not</b> resolve types. Does <b>not</b> validate the objects.
     * <p>
     * Create OtmLibrary facade.
     * 
     * @see #startValidatingAndResolvingTasks()
     * 
     * @param library to add
     * @return newly created OtmLibrary
     */
    public OtmLibrary add(AbstractLibrary absLibrary) {
        return add( absLibrary, getVersionChainFactory() );
    }

    public OtmLibrary addUnmanaged(AbstractLibrary absLibrary) {
        return add( absLibrary, null );
    }

    /**
     * Check all the libraries in the TL Model and add those that have not already been added.
     */
    public void add() {
        // FIXME - make sure all the projects are registered and built
        getProjectManager().getAllProjects();

        // Add will prevent duplicate entries
        getTlModel().getUserDefinedLibraries().forEach( tlLib -> add( tlLib ) );
    }

    // Fixme - should be ok. fixed OtmLibrary to return baseNamespace without PI.
    // Todo - refactor add(*) to simplify
    // Not used on open project task
    protected OtmLibrary add(AbstractLibrary absLibrary, VersionChainFactory versionChainFactory) {
        if (absLibrary == null)
            return null;
        if (contains( absLibrary ))
            return libraries.get( absLibrary );

        OtmLibrary otmLibrary = new OtmLibrary( absLibrary, this );

        libraries.put( absLibrary, otmLibrary );
        // Map of base namespaces with all libraries in that namespace
        if (absLibrary instanceof TLLibrary)
            if (versionChainFactory != null) {
                baseNSManaged.put( otmLibrary.getNameWithBasenamespace(),
                    versionChainFactory.getVersionChain( (TLLibrary) absLibrary ) );
                // log.debug( "Added " + otmLibrary.getNameWithBasenamespace() + otmLibrary.getVersion()
                // + " to base NS managed." );
            } else {
                baseNSUnmanaged.put( otmLibrary.getNameWithBasenamespace(), otmLibrary );
                // log.debug( "Added " + otmLibrary.getNameWithBasenamespace() + otmLibrary.getVersion()
                // + " to base NS UN-managed." );
            }

        // For each named member use the factory to create and add OtmObject
        absLibrary.getNamedMembers().forEach( nm -> OtmLibraryMemberFactory.create( nm, this ) );

        return otmLibrary;
    }

    protected OtmLibrary add(ProjectItem pi, VersionChainFactory versionChainFactory) {
        if (pi == null)
            return null;

        AbstractLibrary absLibrary = pi.getContent();
        if (absLibrary == null)
            return null;
        if (contains( absLibrary ))
            return libraries.get( absLibrary );

        // Create new library
        OtmLibrary otmLibrary = new OtmLibrary( absLibrary, this );
        otmLibrary.add( pi ); // Needed to know base namespace

        // Add new library to the maps
        libraries.put( absLibrary, otmLibrary );
        // Map of base namespaces with all libraries in that namespace
        if (absLibrary instanceof TLLibrary)
            if (versionChainFactory != null) {
                baseNSManaged.put( otmLibrary.getNameWithBasenamespace(),
                    versionChainFactory.getVersionChain( (TLLibrary) absLibrary ) );
                // log.debug( "Added chain for manged base namespace: " + otmLibrary.getNameWithBasenamespace() );
            } else {
                baseNSUnmanaged.put( otmLibrary.getNameWithBasenamespace(), otmLibrary );
                // log.debug( "Added unmanged base namespace: " + otmLibrary.getNameWithBasenamespace() );
            }

        // For each named member use the factory to create and add OtmObject
        absLibrary.getNamedMembers().forEach( nm -> OtmLibraryMemberFactory.create( nm, this ) );

        return otmLibrary;
    }

    /**
     * NOTE - this is slow compared with {@link #contains(tlMember)}
     * 
     * @return true if the member exists as a value in the members map.
     */
    public boolean contains(OtmLibraryMember member) {
        return member != null && members.containsValue( member );
        // return member != null && member.getTL() instanceof LibraryMember && members.containsKey( member.getTlLM() );
    }

    /**
     * @return true if the TL Library Member exists as a key in the members map.
     */
    public boolean contains(LibraryMember tlMember) {
        return members.containsKey( tlMember );
    }

    public boolean contains(AbstractLibrary absLibrary) {
        return absLibrary != null && libraries.containsKey( absLibrary );
    }

    /**
     * Use the TL Model to attempt to get a version chain factory.
     * 
     * @return the factory or null if factory throws exception
     */
    private VersionChainFactory getVersionChainFactory() {
        VersionChainFactory versionChainFactory = null;
        try {
            versionChainFactory = new VersionChainFactory( tlModel );
        } catch (Exception e) {
            if (!showingError) {
                showingError = true;
                Platform.runLater( this::chainError );
            }
            // log.debug( "Exception trying to construct version chain factory: " + e.getLocalizedMessage() );
        }
        return versionChainFactory;
    }

    private static String CHAINERRORMESSAGE =
        "Serious error - a library has an invalid namespace. \nThis will prevent properly presenting libraries in version chains. Examine the library namespaces and either fix the or remove from project.";

    private void chainError() {
        if (dialogBox == null)
            dialogBox = DialogBoxContoller.init();
        dialogBox.show( CHAINERRORMESSAGE );
        dialogBox = null;
        showingError = false;
    }

    /**
     * Add all the libraries in this project to the model manager. Model all libraries and contents. Run validation and
     * type resolver in the background.
     * 
     * @deprecated - use addProjects(). That is all this method did.
     * @param pm
     */
    @Deprecated
    public void addProjects(ProjectManager pm) {
        addProjects();
    }

    /**
     * Get all the projects from the project manager. Create libraries for all project items if they have not already be
     * modeled. Start validation and type resolution task.
     */
    public void addProjects() {
        // log.debug( "AddProjects() with " + getTlModel().getAllLibraries().size() + " libraries" );

        // Add projects to project map
        for (Project project : projectManager.getAllProjects())
            projects.put( project.getName(), new OtmProject( project, this ) );

        // Get the built in libraries, will do nothing if already added
        addBuiltInLibraries( getTlModel() );

        // Get Libraries - Libraries can belong to multiple projects.
        // Map will de-dup the entries based on baseNS and name.
        for (ProjectItem pi : projectManager.getAllProjectItems()) {
            add( pi );
        }

        startValidatingAndResolvingTasks();
        // log.debug( "Model has " + members.size() + " members." );
    }

    public OtmProject getProject(String projectName) {
        return projects.get( projectName );
    }

    /**
     * Start the validation and type resolver tasks. Use this model manager to handle results and its status controller.
     */
    public void startValidatingAndResolvingTasks() {
        // Start a background task to validate the objects
        new ValidateModelManagerItemsTask( this, this, statusController ).go();
        // Start a background task to resolve type relationships
        new TypeResolverTask( this, this, statusController ).go();
    }

    /**
     * Add the built in libraries to the libraries and member maps
     */
    protected void addBuiltInLibraries(TLModel tlModel) {
        for (BuiltInLibrary tlLib : tlModel.getBuiltInLibraries()) {
            if (libraries.containsKey( tlLib )) {
                log.warn( "Trying to add builtin library again." );
            }
            libraries.put( tlLib, new OtmBuiltInLibrary( tlLib, this ) );
            for (LibraryMember tlMember : tlLib.getNamedMembers()) {
                OtmLibraryMemberFactory.create( tlMember, this ); // creates and adds
            }
        }
    }

    /**
     * If the project item is new to this model manager:
     * <ul>
     * <li>create OtmLibrary to represent the abstract TL library
     * <li>add the absLibrary:OtmLibrary pair to the libraries map
     * <li>add the libraryNamespace:library in the baseNS map
     * </ul>
     * <p>
     * base namespaces can have multiple libraries.
     * 
     * @param pi
     */
    private void add(ProjectItem pi) {
        if (pi == null)
            return;
        AbstractLibrary absLibrary = pi.getContent();
        if (absLibrary == null)
            return;
        // log.debug( "Adding project item: " + absLibrary.getName() + " in " + absLibrary.getNamespace() );
        if (contains( absLibrary )) {
            // let the library track project as needed to know if the library is editable
            libraries.get( absLibrary ).add( pi );
        } else {
            // Model and Add newly discovered library to the libraries and baseNS maps
            add( pi, getVersionChainFactory() );
        }
    }

    /**
     * Clear the model. Clears the model manager's data, the TL Model, and Project Manager.
     */
    public void clear() {
        baseNSManaged.clear();
        baseNSUnmanaged.clear();
        libraries.clear();
        members.clear();
        projects.clear();

        tlModel.clearModel();
        projectManager.closeAll();

        // log.debug( "Cleared model. " + tlModel.getAllLibraries().size() );
    }

    public void close(OtmProject oProject) {
        if (oProject != null && oProject.getTL() != null)
            projects.remove( oProject.getTL().getName() );
    }

    /**
     * Examine all members. Return list of owners that have a of a descendant type user that is assigned to provider.
     * 
     * @param provider
     * @return
     */
    public List<OtmLibraryMember> findUsersOf(OtmTypeProvider provider) {
        // Changed 11/5/2019 - why copy list? The list is not changing.
        List<OtmLibraryMember> values = new ArrayList<>( members.values() );
        List<OtmLibraryMember> users = new ArrayList<>();
        for (OtmLibraryMember m : values) {
            if (m.getUsedTypes().contains( provider ))
                users.add( m );
        }
        // if (!users.isEmpty())
        // log.debug("Found " + users.size() + " users of " + p.getNameWithPrefix());
        return users;
    }

    /**
     * Examine all members. Return list of members that use the passed member as a base type.
     * 
     * @param member
     * @return
     */
    public List<OtmLibraryMember> findSubtypesOf(OtmLibraryMember member) {
        // Changed 11/5/2019 - why copy list? The list is not changing.
        List<OtmLibraryMember> values = new ArrayList<>( members.values() );
        List<OtmLibraryMember> subTypes = new ArrayList<>();
        // Contextual facets use base type to define injection point
        for (OtmLibraryMember m : values) {
            if (m.getBaseType() == member && !(m instanceof OtmContextualFacet))
                subTypes.add( m );
        }
        // if (!users.isEmpty())
        // log.debug("Found " + users.size() + " users of " + p.getNameWithPrefix());
        return subTypes;
    }

    /**
     * @param TL Abstract Library
     * @return OtmLibrary associated with the abstract library
     */
    public OtmLibrary get(AbstractLibrary absLibrary) {
        // if (!libraries.containsKey( absLibrary )) {
        // // abstract library may be in the library pi list
        // if (absLibrary != null)
        // log.debug( "Missing library associated with: " + absLibrary.getName() );
        // else
        // log.debug( "Can not get library because TL library is null." );
        // // printLibraries();
        // }
        return libraries.get( absLibrary );
    }

    public OtmLibrary get(String fullName) {
        for (OtmLibrary lib : libraries.values()) {
            // log.debug("testing: " + fullName + " against " + lib.getFullName());
            if (lib.getFullName().equals( fullName ))
                return lib;
        }
        return null;
    }

    /**
     * @param TL Library
     * @return OtmLibrary associated with the TL Library
     */
    public OtmLibrary get(TLLibrary tlLibrary) {
        if (!libraries.containsKey( tlLibrary ))
            log.warn( "Missing library associated with: " + tlLibrary.getName() );
        return libraries.get( tlLibrary );
    }

    /**
     * @return the abstract library associated with the otm library parameter.
     */
    public AbstractLibrary get(OtmLibrary library) {
        for (Entry<AbstractLibrary,OtmLibrary> set : libraries.entrySet())
            if (set.getValue() == library)
                return set.getKey();
        return null;
    }

    /**
     * Return an action manager. Intended only for use by libraries.
     * 
     * @param full if false, only return readOnly action manager
     * @return read-only or full action manager
     */
    public DexActionManager getActionManager(boolean full) {
        return full ? fullActionManager : readOnlyActionManager;
    }

    public DexActionManager getMinorActionManager(boolean full) {
        return full ? minorActionManager : readOnlyActionManager;
    }

    /**
     * @return New arrayList of strings for both managed and unmanaged base namespaces.
     */
    public Set<String> getBaseNamespaces() {
        Set<String> nsList = new HashSet<>( baseNSManaged.keySet() );
        nsList.addAll( baseNSUnmanaged.keySet() );
        return nsList;
        // return baseNSManaged.keySet();
    }

    public Collection<OtmLibrary> getLibraries() {
        return Collections.unmodifiableCollection( libraries.values() );
    }

    /**
     * 
     * @param baseNamespace
     * @return If the namespace is managed, the set contains all the <b>managed</b> libraries in base namespace
     *         (namespace root). If the namespace is unmanged, the set is just the single library with that
     *         namespace+name.
     */
    public Set<OtmLibrary> getLibraryChain(String baseNamespace) {
        Set<OtmLibrary> libs = new LinkedHashSet<>();
        VersionChain<TLLibrary> chain = baseNSManaged.get( baseNamespace );
        if (chain != null) {
            // Null means unmanaged libraries without a chain
            for (TLLibrary tlLib : chain.getVersions())
                if (libraries.get( tlLib ) != null)
                    libs.add( libraries.get( tlLib ) );
            // else
            // log.debug( "OOPS - library in chain is null." );
        } else {
            libs.add( baseNSUnmanaged.get( baseNamespace ) );
        }
        return libs;
    }

    /**
     * Get all libraries in the base namespace with the same major version
     * 
     * @param baseNamespace
     * @return
     */
    public List<OtmLibrary> getVersionChain(OtmLibrary library) {
        List<OtmLibrary> versionChain = new ArrayList<>();
        String baseNS = library.getNameWithBasenamespace();
        // Null means unmanaged libraries without a chain
        VersionChain<TLLibrary> chain = baseNSManaged.get( baseNS );
        if (chain != null) {
            OtmLibrary otmLib;
            for (TLLibrary tlLib : chain.getVersions()) {
                otmLib = libraries.get( tlLib );
                try {
                    if (otmLib != null && otmLib.getMajorVersion() == library.getMajorVersion())
                        versionChain.add( otmLib );
                } catch (VersionSchemeException e) {
                    // if version error, ignore the library
                }
            }
        }
        return versionChain;

        // Set<OtmLibrary> libs = new LinkedHashSet<>();
        // VersionChain<TLLibrary> chain = baseNSManaged.get( baseNamespace );
        // if (chain != null) {
        // // Null means unmanaged libraries without a chain
        // for (TLLibrary tlLib : chain.getVersions())
        // if (libraries.get( tlLib ) != null)
        // libs.add( libraries.get( tlLib ) );
        // else
        // log.debug( "OOPS - library in chain is null." );
        // } else {
        // libs.add( baseNSUnmanaged.get( baseNamespace ) );
        // }
        // return libs;
    }

    /**
     * Get a project that contains this library.
     * <p>
     * Note: OTM-DE used projects to manage write access to libraries. DEX does not.
     * 
     * @param library
     * @return
     */
    public OtmProject getManagingProject(OtmLibrary library) {
        library.getBaseNamespace();
        OtmProject foundProject = null;
        for (OtmProject project : projects.values()) {
            if (project.contains( get( library ) ))
                if (foundProject == null || library.getBaseNamespace().startsWith( project.getTL().getProjectId() ))
                    foundProject = project;
        }
        return foundProject;
    }

    public OtmLibraryMember getMember(TLModelElement tlMember) {
        if (tlMember instanceof LibraryMember)
            return members.get( (tlMember) );
        return null;
    }

    /**
     * Get the member with matching prefix and name
     * 
     * @param nameWithPrefix
     * @return member if found or null
     */
    public OtmLibraryMember getMember(String nameWithPrefix) {
        for (OtmLibraryMember candidate : getMembers())
            if (candidate.getNameWithPrefix().equals( nameWithPrefix ))
                return candidate;
        return null;
    }

    /**
     * @return all the library members being managed in a unmodifiableCollection
     */
    public Collection<OtmLibraryMember> getMembers() {
        return Collections.unmodifiableCollection( members.values() );
    }

    /**
     * @return new list with all the library members in that library
     */
    public List<OtmLibraryMember> getMembers(OtmLibrary library) {
        List<OtmLibraryMember> libraryMembers = new ArrayList<>();
        members.values().forEach( m -> {
            if (m.getLibrary() == library)
                libraryMembers.add( m );
        } );
        return libraryMembers;
    }

    public boolean hasProjects() {
        return !projects.isEmpty();
    }

    public Collection<OtmProject> getProjects() {
        return Collections.unmodifiableCollection( projects.values() );
    }

    /**
     * 
     * @param library
     * @return new list of projects that contain the library or empty list.
     */
    public List<OtmProject> getProjects(AbstractLibrary library) {
        List<OtmProject> list = new ArrayList<>();
        getProjects().forEach( p -> {
            if (p.contains( library ))
                list.add( p );
        } );
        return list;
    }

    /**
     * @return just user projects, not built-in
     */
    public List<OtmProject> getUserProjects() {
        List<OtmProject> projectList = new ArrayList<>();
        projects.values().forEach( p -> {
            if (!(p.getTL() instanceof BuiltInProject))
                projectList.add( p );
        } );
        return projectList;
    }

    /**
     * Get the TL Model used by this model manager.
     * 
     * @return
     */
    public TLModel getTlModel() {
        return tlModel;
    }

    @Override
    public void handleTaskComplete(WorkerStateEvent event) {
        // NO-OP
        // if (event != null && event.getTarget() != null) {
        // if (event.getTarget() instanceof TypeResolverTask)
        // log.debug( "Type Resolver Task complete" );
        // else if (event.getTarget() instanceof ValidateModelManagerItemsTask)
        // log.debug( "Validation Task complete" );
        // } else
        // log.debug( "Task complete" );
    }

    /**
     * Look into the chain and return true if this is the latest version (next version = null)
     * 
     * @param lib
     * @return
     */
    public boolean isLatest(OtmLibrary lib) {
        if (lib == null || lib.getTL() == null)
            return false;
        // String key = lib.getNameWithBasenamespace();
        VersionChain<TLLibrary> chain = baseNSManaged.get( lib.getNameWithBasenamespace() );
        if (chain != null && lib.getTL() instanceof TLLibrary) {
            // List<TLLibrary> versions = chain.getVersions();
            return (chain.getNextVersion( (TLLibrary) lib.getTL() )) == null;
        }
        return true;
    }

    /**
     * Create a new project.
     * 
     * @param projectFile
     * @param required name the user-displayable name of the project
     * @param optional defaultContextId Assigns the default context ID to use for EXAMPLE generation in this project.
     * @param projectId the ID of the project, typically namespace being worked on. ProjectId must not already be in
     *        use.
     * @param description
     * @return
     * @throws LibrarySaveException
     */
    public OtmProject newProject(File projectFile, String name, String defaultContextId, String projectId,
        String description) throws LibrarySaveException {
        if (projectFile == null || projectId == null || projectId.isEmpty())
            throw new IllegalArgumentException( "Missing required argument(s) to create new project." );
        // Verify the file exists and is writable
        if (!projectFile.canWrite())
            throw new IllegalArgumentException( "Project file is not writable." );

        OtmProject op = null;

        if (projectManager == null) {
            // Try to find one to use - this should be dead code (7/15/2019)
            for (OtmProject o : projects.values())
                projectManager = o.getTL().getProjectManager();
        }
        if (projectManager == null)
            throw new IllegalArgumentException( "Missing required project manager." );

        // log.debug( "Creating new project in file: " + projectFile.getAbsolutePath() );

        Project p = new Project( projectManager );
        try {
            p.setProjectFile( projectFile );
            p.setProjectId( projectId );

            op = new OtmProject( p, this );
            op.setDefaultContextId( defaultContextId );
            op.setDescription( description );
            op.setName( name );

            // Saving project causes file write of contents
            projectManager.saveProject( p );

            // register project in projects map
            projects.put( name, op );
        } catch (IllegalArgumentException e) {
            log.warn( "Exception creating project: " + e.getLocalizedMessage() );
            throw new IllegalArgumentException( "Could not create valid project: " + e.getLocalizedMessage() );
        }
        return op;
    }


    public void printLibraries() {
        libraries.entrySet().forEach( l -> log.debug( l.getValue().getName() ) );
    }

    public void setStatusController(DexStatusController statusController) {
        this.statusController = statusController;
    }

    /**
     * @return
     */
    public ProjectManager getProjectManager() {
        // ReentrantLock bankLock = new ReentrantLock();
        // // FIXME - if in background thread, wait for other threads to be complete
        // if (!Platform.isFxApplicationThread())
        // if (statusController.getQueueSize() > 1)
        // Thread.holdsLock( projectManager )
        // Platform.runLater( this::getProjectManager );

        return projectManager;
    }


    /**
     * @param sort if true, sort the list by member name
     * @return new list of members that are resources
     */
    public List<OtmResource> getResources(boolean sort) {
        List<OtmResource> resources = new ArrayList<>();
        members.values().forEach( m -> {
            if (m instanceof OtmResource)
                resources.add( (OtmResource) m );
        } );
        if (sort)
            resources.sort( (one, other) -> one.getName().compareTo( other.getName() ) );
        return resources;
    }


    /**
     * @return
     */
    public List<OtmLibrary> getEditableLibraries() {
        List<OtmLibrary> libs = new ArrayList<>();
        libraries.values().forEach( l -> {
            if (l.isEditable())
                libs.add( l );
        } );
        return libs;
    }

    /**
     * @return true if any library is editable
     */
    public boolean hasEditableLibraries() {
        for (OtmLibrary l : libraries.values())
            if (l.isEditable())
                return true;
        return false;
    }


    /**
     * @param library
     * @return true if any library except the passed library is editable
     */
    public boolean hasEditableLibraries(OtmLibrary library) {
        for (OtmLibrary l : libraries.values())
            if (l != library && l.isEditable())
                return true;
        return false;
    }

    public static final String XSD_LIBRARY_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    public static final String XSD_ID_NAME = "ID";

    /**
     * Try to find the XSD ID type and return it
     * 
     * @return
     */
    public OtmXsdSimple getIdType() {
        LibraryMember tlId = null;
        for (OtmLibrary lib : getLibraries()) {
            if (lib.getTL() instanceof BuiltInLibrary && lib.getTL().getNamespace().equals( XSD_LIBRARY_NAMESPACE ))
                tlId = lib.getTL().getNamedMember( XSD_ID_NAME );
        }
        OtmObject id = OtmModelElement.get( (TLModelElement) tlId );
        // if (id == null)
        // log.debug( "Missing ID type to return." );
        return id instanceof OtmXsdSimple ? (OtmXsdSimple) id : null;
    }


    /**
     * @return
     */
    public OtmXsdSimple getEmptyType() {
        LibraryMember tlId = null;
        for (OtmLibrary lib : getLibraries()) {
            if (lib.getTL() instanceof BuiltInLibrary && lib.getTL().getNamespace().equals( OTA_LIBRARY_NAMESPACE ))
                tlId = lib.getTL().getNamedMember( OTA_EMPTY_NAME );
            // log.debug( "Library " + lib + " namespace = " + lib.getTL().getNamespace() );
        }
        OtmObject id = OtmModelElement.get( (TLModelElement) tlId );
        return id instanceof OtmXsdSimple ? (OtmXsdSimple) id : null;
    }

}
