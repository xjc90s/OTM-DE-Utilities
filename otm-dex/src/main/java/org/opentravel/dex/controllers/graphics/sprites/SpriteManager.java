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

package org.opentravel.dex.controllers.graphics.sprites;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.dex.controllers.DexIncludedController;
import org.opentravel.dex.controllers.graphics.sprites.connections.Connection;
import org.opentravel.dex.controllers.graphics.sprites.retangles.ColumnRectangle;
import org.opentravel.dex.events.DexEvent;
import org.opentravel.dex.events.DexMemberSelectionEvent;
import org.opentravel.model.otmLibraryMembers.OtmBusinessObject;
import org.opentravel.model.otmLibraryMembers.OtmChoiceObject;
import org.opentravel.model.otmLibraryMembers.OtmContextualFacet;
import org.opentravel.model.otmLibraryMembers.OtmCore;
import org.opentravel.model.otmLibraryMembers.OtmEnumeration;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;
import org.opentravel.model.otmLibraryMembers.OtmResource;
import org.opentravel.model.otmLibraryMembers.OtmSimpleObjects;
import org.opentravel.model.otmLibraryMembers.OtmValueWithAttributes;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Manage a collection of Dex Sprites
 * 
 * @author dmh
 */
public class SpriteManager {
    private static Log log = LogFactory.getLog( SpriteManager.class );

    // private static final double COLUMN_START = 10;
    // private static final double COLUMN_WIDTH = 150;
    // private static final int FACET_OFFSET = 5;
    // private static final double MINIMUM_SLOT_HEIGHT = 20;
    // private static final String DEFAULT_FONT_NAME = "Monospaced";

    private SettingsManager settingsManager;
    private Pane spritePane;
    // private List<DexSprite<?>> activeSprites = new ArrayList<>();
    private DexIncludedController<?> parentController = null;
    private DexSprite<?> draggedSprite = null;
    // private int fontSize = 14;

    // private Canvas backgroundCanvas;
    // private GraphicsContext defaultGC;
    private GraphicsContext connectionsGC;
    private List<Connection> connections;
    private Canvas connectionsCanvas;
    private List<ColumnRectangle> columns;

    private Paint backgroundColor = Color.gray( 0.95 );


    /**
     * Initialize the sprite. Create connections canvas and add to pane. Set mouse click handler.
     * 
     * @param spritePane
     * @param owner
     * @param gc
     */
    public SpriteManager(DexIncludedController<?> owner, SettingsManager settingsManager) {
        parentController = owner;
        this.settingsManager = settingsManager;
        this.spritePane = settingsManager.getSpritePane();
        //
        connectionsCanvas = new Canvas( spritePane.getWidth(), spritePane.getHeight() );
        spritePane.getChildren().add( connectionsCanvas );
        connectionsCanvas.widthProperty().bind( spritePane.widthProperty() );
        connectionsCanvas.heightProperty().bind( spritePane.heightProperty() );
        connectionsGC = connectionsCanvas.getGraphicsContext2D();
        connectionsGC.setFill( backgroundColor );
        connectionsGC.fillRect( 0, 0, connectionsCanvas.getWidth(), connectionsCanvas.getHeight() );
        connections = new ArrayList<>();
        //
        createColumns( 3 );
        //
        spritePane.setOnMouseClicked( this::mouseClick );
    }

    private void createColumns(int count) {
        if (columns == null)
            columns = new ArrayList<>();
        ColumnRectangle column = new ColumnRectangle( spritePane, null );
        int i = 0;
        do {
            columns.add( column );
            ColumnRectangle nc = new ColumnRectangle( spritePane, column );
            column.setNext( nc );
            column = nc;
        } while (i++ < count);
    }


    /**
     * Add the sprite to the list and render into pane
     * 
     * @param sprite
     */
    public void add(DexSprite<OtmLibraryMember> sprite, ColumnRectangle column) {
        column.add( sprite );
        // activeSprites.add( sprite );
        // spritePane.getChildren().add( sprite.render() );
    }

    public void setCollapsed(boolean collapsed) {
        getAllSprites().forEach( s -> s.setCollapsed( collapsed ) );
        refresh();
    }

    /**
     * Uses Sprite factory to adds sprite and related sprites to list and FX pane's children. Manager determines the
     * location for the sprite.
     * 
     * @param member
     */
    public DexSprite<OtmLibraryMember> add(OtmLibraryMember member, ColumnRectangle column) {
        return add( member, column, false );
    }

    public MemberSprite<OtmLibraryMember> add(OtmLibraryMember member, ColumnRectangle column, boolean collapsed) {
        if (column == null)
            column = getColumn( 1 );
        MemberSprite<OtmLibraryMember> memberSprite = column.find( member );
        if (memberSprite == null)
            memberSprite = factory( member );
        if (memberSprite != null) {
            memberSprite.setCollapsed( collapsed );
            column.add( memberSprite );
        }
        return memberSprite;
    }

    /**
     * Sprite factory.
     * 
     * @param member
     * @return Built sprite or null.
     */
    // sprite factory
    private MemberSprite<OtmLibraryMember> factory(OtmLibraryMember member) {
        MemberSprite<?> newSprite = null;
        if (member instanceof OtmBusinessObject)
            newSprite = new BusinessObjectSprite( (OtmBusinessObject) member, this, settingsManager );
        else if (member instanceof OtmChoiceObject)
            newSprite = new ChoiceObjectSprite( (OtmChoiceObject) member, this, settingsManager );
        else if (member instanceof OtmCore)
            newSprite = new CoreObjectSprite( (OtmCore) member, this, settingsManager );
        else if (member instanceof OtmValueWithAttributes)
            newSprite = new VWASprite( (OtmValueWithAttributes) member, this, settingsManager );
        else if (member instanceof OtmContextualFacet)
            newSprite = new ContextualFacetSprite( (OtmContextualFacet) member, this, settingsManager );
        else if (member instanceof OtmEnumeration)
            newSprite = new EnumerationSprite( (OtmEnumeration<?>) member, this, settingsManager );
        else if (member instanceof OtmSimpleObjects)
            newSprite = new SimpleSprite( (OtmSimpleObjects<?>) member, this, settingsManager );
        else if (member instanceof OtmResource)
            newSprite = new ResourceSprite( (OtmResource) member, this, settingsManager );
        // log.debug( "factory created: " + newSprite );
        return (MemberSprite<OtmLibraryMember>) newSprite;
    }

    /**
     * Add to connections list and redraw the connections canvas
     * 
     * @param c
     */
    public void addAndDraw(Connection c) {
        if (!connections.contains( c )) {
            connections.add( c );
            c.draw( connectionsGC );
        }
    }

    public ColumnRectangle getColumn(int index) {
        for (ColumnRectangle c : columns)
            if (c.getIndex() == index)
                return c;
        return null;
    }

    /**
     * 
     * @return new list of all sprites in all columns
     */
    public List<DexSprite<?>> getAllSprites() {
        List<DexSprite<?>> sprites = new ArrayList<>();
        columns.forEach( c -> sprites.addAll( c.getSprites() ) );
        return sprites;
    }

    /**
     * Remove all sprites and their canvases. Remove all connections.
     */
    public void clear() {
        columns.forEach( ColumnRectangle::clear );
        eraseConnections();
        connections.clear();
    }

    // FIXME - this seems broken!
    @Deprecated
    public boolean contains(OtmLibraryMember member) {
        for (DexSprite<?> s : getAllSprites())
            if (s.getMember() == member)
                return true;
        return false;
    }

    public DexSprite<?> get(OtmLibraryMember member) {
        for (DexSprite<?> s : getAllSprites())
            if (s.getMember() == member)
                return s;
        return null;
    }

    /**
     * clear the dragged sprite then post it at the new location
     */
    public void drag(MouseEvent e) {
        if (draggedSprite != null) {
            // log.debug( "Found Selected Sprite: " + draggedSprite.getMember() );
            draggedSprite.clear();
            draggedSprite.set( e.getX(), e.getY() );
            draggedSprite.render();
            updateConnections( draggedSprite );
        }
        // log.debug( "Dragging sprite." );
    }


    public void dragEnd(MouseEvent e) {
        if (draggedSprite != null)
            updateConnections();
        draggedSprite = null;
        // log.debug( "Drag end." );

        // FIXME - may have to move to different column
    }

    public void dragStart(MouseEvent e) {
        // log.debug( "Drag start. x = " + e.getX() + " y = " + e.getY() );
        draggedSprite = findSprite( new Point2D( e.getX(), e.getY() ) );
        if (draggedSprite != null)
            draggedSprite.getCanvas().toFront();
    }

    public void eraseConnections() {
        // connectionsGC.clearRect( 0, 0, connectionsCanvas.getWidth(), connectionsCanvas.getHeight() );
        connectionsGC.fillRect( 0, 0, connectionsCanvas.getWidth(), connectionsCanvas.getHeight() );
    }

    public DexSprite<?> findSprite(OtmLibraryMember member) {
        DexSprite<?> selectedSprite = null;
        for (ColumnRectangle column : columns) {
            selectedSprite = column.find( member );
            if (selectedSprite != null)
                return selectedSprite;
        }
        // for (DexSprite<?> sprite : getAllSprites())
        // if (sprite.getMember() == member) {
        // return (sprite);
        // }
        return selectedSprite;
    }

    public DexSprite<?> findSprite(Point2D point) {
        DexSprite<?> selectedSprite = null;
        for (DexSprite<?> sprite : getAllSprites())
            if (sprite.contains( point )) {
                return (sprite);
            }
        return selectedSprite;
    }

    public GraphicsContext getConnectionsGC() {
        return connectionsGC;
    }

    // public Point2D getNextInColumn(DexSprite<?> sprite) {
    // Point2D bottom = new Point2D( 0, 0 );
    // if (sprite != null) {
    // DexSprite<?> next = sprite;
    // do {
    // bottom = new Point2D( next.getBoundaries().getX(), next.getBoundaries().getMaxY() + 5 );
    // next = findSprite( bottom );
    // } while (next != null && bottom.getY() < spritePane.getHeight());
    // }
    // return bottom;
    // }

    // public Point2D getNextInColumn(double x, double y) {
    // Point2D bottom = new Point2D( x, y );
    // DexSprite<?> next = null;
    // do {
    // next = findSprite( bottom );
    // if (next != null)
    // bottom = new Point2D( x, next.getBoundaries().getMaxY() + 5 );
    // else {
    // bottom = new Point2D( x, bottom.getY() + MINIMUM_SLOT_HEIGHT );// minimum slot size;
    // next = findSprite( bottom );
    // }
    // } while (next != null && bottom.getY() < spritePane.getHeight());
    // return bottom;
    // }

    // public Point2D getNextRightColumn(DexSprite<?> sprite) {
    // // Get the x for the next column
    // double columnX = sprite.getBoundaries().getMaxX() + COLUMN_WIDTH;
    // // Start looking just above the sprite
    // double startY = sprite.getBoundaries().getY() - 20 > 0 ? sprite.getBoundaries().getY() - 20 : 0;
    // Point2D bottom = new Point2D( columnX, startY );
    // DexSprite<?> next = null;
    // do {
    // next = findSprite( bottom );
    // if (next != null)
    // bottom = new Point2D( columnX, next.getBoundaries().getMaxY() + 10 );
    // } while (next != null && bottom.getY() < spritePane.getHeight());
    //
    // return bottom;
    // }

    private void mouseClick(MouseEvent e) {
        // log.debug( "Mouse click on at: " + e.getX() + " " + e.getY() );
        // The whole canvas is active, check boundaries
        // TODO - use find(point)
        DexSprite<?> selected = null;
        for (DexSprite<?> sprite : getAllSprites())
            if (sprite.contains( new Point2D( e.getX(), e.getY() ) )) {
                selected = sprite;
                break;
            }
        if (selected != null) {
            if (e.getButton() != MouseButton.SECONDARY && e.getClickCount() >= 2) {
                // log.debug( "Throw event: " + selected.getMember() );
                publishEvent( new DexMemberSelectionEvent( selected.getMember() ) );
            } else
                selected.findAndRunRectangle( e );
        }
    }

    // /**
    // * Put all sprites onto the parent node
    // */
    // public void post() {
    // // log.debug( "Posting all sprites." );
    // for (DexSprite<?> sprite : activeSprites)
    // if (sprite != null) {
    // sprite.render();
    // spritePane.getChildren().add( sprite.render() );
    // }
    // }

    protected void publishEvent(DexEvent event) {
        if (parentController != null)
            parentController.publishEvent( event );
    }

    public void refresh() {
        for (DexSprite<?> sprite : getAllSprites()) {
            sprite.clear();
            sprite.render();
        }
        updateConnections();
    }

    public void remove(DexSprite<?> sprite) {
        if (sprite != null) {
            removeConnection( sprite );
            sprite.getColumn().clear();
            // sprite.clear();
            // activeSprites.remove( sprite );
        }
    }

    public void remove(OtmLibraryMember member) {
        DexSprite<?> sprite = findSprite( member );
        remove( sprite );
    }

    public void removeConnection(DexSprite<?> sprite) {
        if (sprite != null) {
            List<Connection> list = new ArrayList<>( connections );
            for (Connection c : list)
                if (c.contains( sprite ))
                    connections.remove( c );
            updateConnections();
        }
    }

    public void update(Color color) {
        settingsManager.update( color );
        getAllSprites().forEach( s -> s.setBackgroundColor( color ) );
        refresh();
    }

    /**
     * Update the size then redraw the sprites and connections.
     * 
     * @param size
     */
    public void update(int size) {
        if (settingsManager.updateSize( size )) {
            getAllSprites().forEach( s -> s.set( settingsManager.getFont() ) );
            refresh();
        }
    }

    public void updateConnections() {
        eraseConnections();
        for (Connection c : connections) {
            c.draw( connectionsGC );
        }
        // FIXME - rectangles may have changed if font changed.
    }

    public void updateConnections(DexSprite<?> sprite) {
        for (Connection c : connections) {
            c.update( sprite, connectionsGC, backgroundColor );
        }
    }

}
