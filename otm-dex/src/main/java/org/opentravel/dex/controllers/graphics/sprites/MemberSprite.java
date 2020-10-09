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
import org.opentravel.common.ImageManager;
import org.opentravel.dex.controllers.graphics.sprites.GraphicsUtils.DrawType;
import org.opentravel.dex.controllers.graphics.sprites.Rectangle.RectangleEventHandler;
import org.opentravel.model.OtmTypeProvider;
import org.opentravel.model.OtmTypeUser;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

/**
 * Graphics Display Object (Sprite) for containing OTM library members.
 * 
 * @author dmh
 * @param <O>
 *
 */
public abstract class MemberSprite<M extends OtmLibraryMember>
    implements DexSprite<OtmLibraryMember>, RectangleEventHandler {
    private static Log log = LogFactory.getLog( MemberSprite.class );

    private static final double MIN_HEIGHT = 50;
    private static final double MIN_WIDTH = 50;

    private double x;
    private double y;
    protected M member;
    private Canvas canvas;
    private GraphicsContext gc = null;

    private Rectangle boundaries = null;
    // private Font propertyFont = new Font( "Arial", 12 );
    private boolean collapsed = false;

    private List<Rectangle> rectangles = new ArrayList<>();
    private SpriteManager manager;

    /**
     * Initialize member sprite. Create canvas and save GC for parameters.
     * 
     * @param member
     * @param paramsGC - source of font and color information ONLY. If null, defaults are used.
     */
    public MemberSprite(M member, SpriteManager manager, GraphicsContext paramsGC) {
        this.member = member;

        this.manager = manager;
        boundaries = new Rectangle( 0, 0, 0, 0 );

        //
        canvas = new Canvas( MIN_WIDTH, MIN_HEIGHT );
        gc = canvas.getGraphicsContext2D();
        setGCParams( paramsGC );
    }

    @Override
    public void add(Rectangle rectangle) {
        if (!rectangles.contains( rectangle ))
            rectangles.add( rectangle );
    }

    @Override
    public void clear() {
        gc.clearRect( 0, 0, canvas.getWidth(), canvas.getHeight() );
        rectangles.clear();
    }

    @Override
    public boolean contains(Point2D point) {
        return boundaries.contains( point );
    }

    public void drawControls(Rectangle boundaries) {
        Image close = ImageManager.getImage( ImageManager.Icons.CLOSE );
        Image collapse = ImageManager.getImage( ImageManager.Icons.COLLAPSE );

        // Start at right edge and work backwards
        double cx = boundaries.getMaxX() - GraphicsUtils.MEMBER_MARGIN - close.getWidth();
        double cy = boundaries.getY() + GraphicsUtils.MEMBER_MARGIN;
        Rectangle r = GraphicsUtils.drawImage( close, DrawType.OUTLINE, gc, cx, cy );
        rectangles.add( r );
        r.setOnMouseClicked( e -> manager.remove( this ) );

        cx = r.getX() - collapse.getWidth();
        r = GraphicsUtils.drawImage( collapse, DrawType.OUTLINE, gc, cx, cy );
        rectangles.add( r );
        r.setOnMouseClicked( e -> collapseOrExpand() );
    }

    public void collapseOrExpand() {
        clear();
        collapsed = !collapsed;
        render();

    }

    public M getMember() {
        return member;
    }

    @Override
    public Canvas render() {
        log.debug( "Rendering at " + x + " " + y + " sprite for: " + member );
        if (member == null || manager == null)
            return null;

        setBoundaries( 0, 0 );

        // Create Canvas
        Rectangle canvasR = new Rectangle( x, y, boundaries.getWidth() + GraphicsUtils.CANVAS_MARGIN,
            boundaries.getHeight() + GraphicsUtils.CANVAS_MARGIN );
        canvas.setHeight( y + canvasR.getHeight() );
        canvas.setWidth( x + canvasR.getWidth() );
        // canvasR.draw( gc, false );
        log.debug( "Sized canvas: " + canvasR );

        drawMember( gc, gc.getFont() );
        manager.updateConnections( this );
        return canvas;
    }

    /**
     * If width or height are 0 then compute new values
     * 
     * @param width if 0, compute
     * @param height if 0, compute
     */
    public void setBoundaries(double width, double height) {
        // Use minimum boundaries
        boundaries = new Rectangle( x, y, MIN_WIDTH, MIN_HEIGHT );
        // Get size of sprite using minimum boundaries
        Rectangle ms = drawMember( null, gc.getFont() );
        // Set the true boundaries
        boundaries = new Rectangle( x, y, width == 0 ? ms.getWidth() : width, height == 0 ? ms.getHeight() : height );
        // boundaries.draw( gc, false );
    }

    @Override
    public void refresh() {
        clear();
        // setBoundaries( 0, 0 );
        // drawMember( gc, gc.getFont() );
        render();
    }

    /**
     * Draw the facets and properties for this member. Start at the passed x, y.
     * 
     * @param gc if null, compute size. If not-null, draw within boundaries.
     * @param font
     * @param x
     * @param y
     * @return rectangle around all contents
     */
    public abstract Rectangle drawContents(GraphicsContext gc, Font font, final double x, final double y);

    /**
     * Draw background, label and controls.
     * 
     * @param gc
     * @param font
     * @return
     */
    public Rectangle drawMember(GraphicsContext gc, Font font) {
        if (member == null)
            return new Rectangle( 0, 0, 0, 0 );

        // Draw background box
        if (gc != null) {
            Rectangle bRect = new Rectangle( boundaries.getX(), boundaries.getY(),
                boundaries.getWidth() + FacetRectangle.FACET_MARGIN,
                boundaries.getHeight() + FacetRectangle.FACET_MARGIN );
            bRect.draw( gc, false ); // Outline
            bRect.draw( gc, true ); // Fill
        }

        // Draw the name of the object
        Rectangle mRect = GraphicsUtils.drawLabel( member.getName(), member.getIcon(), gc, font, x, y );
        double width = mRect.getWidth();
        double height = mRect.getHeight();

        // Add the controls
        if (gc == null)
            width += 2 * 18;
        else
            drawControls( boundaries );

        // Show content (facets, properties, etc)
        mRect = drawContents( gc, font, boundaries.getX(), mRect.getMaxY() );
        // mRect.draw( gc, false );
        if (gc == null && mRect.getWidth() + FacetRectangle.FACET_MARGIN > width)
            width = mRect.getWidth() + FacetRectangle.FACET_MARGIN;
        height += mRect.getHeight();


        // Handler for canvas layer
        if (manager != null) {
            canvas.setOnMouseDragged( manager::drag );
            canvas.setOnDragDetected( manager::dragStart );
            canvas.setOnMouseReleased( manager::dragEnd );
            // Clicks go to the top most node...so let the pane catch them
            // canvas.setOnMouseClicked( this::mouseClick );
        }
        log.debug( "Refreshed " + member );
        mRect = new Rectangle( x, y, width, height );
        // mRect.draw( gc, false );
        return mRect;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public void onRectangleClick(MouseEvent e) {
        log.debug( "Rectangle click at: " + e.getX() + " " + e.getY() );
    }

    // private void mouseClick(MouseEvent e) {
    // log.debug( "Mouse click on " + member + " at: " + e.getX() + " " + e.getY() );
    //
    // // The whole canvas is active, check boundaries
    // if (boundaries.contains( new Point2D( e.getX(), e.getY() ) )) {
    // if (e.getButton() == MouseButton.SECONDARY)
    // log.debug( "TODO - secondary button click." );
    // else if (e.getClickCount() >= 2) {
    // log.debug( "Throw event: " + member );
    // manager.publishEvent( new DexMemberSelectionEvent( member ) );
    // } else
    // findAndRunRectangle( e );
    // }
    // }

    @Override
    public Rectangle find(double x, double y) {
        Rectangle selected = null;
        for (Rectangle r : rectangles)
            if (r.contains( x, y )) {
                selected = r;
                break;
            }
        return selected;
    }

    public void findAndRunRectangle(MouseEvent e) {
        Rectangle selected = find( e.getX(), e.getY() );
        if (selected != null)
            selected.onMouseClicked( e );
    }


    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setBackgroundColor(Color color) {
        gc.setFill( color );
    }

    /**
     * Use defaults
     */
    public void setGCParams() {
        if (gc != null) {
            gc.setFont( new Font( "Arial", 18 ) );
            gc.setFill( Color.gray( 0.85 ) );
            gc.setStroke( Color.DARKSLATEBLUE );
            gc.setLineWidth( 1 );
        }
    }

    public void setGCParams(Font font, Paint fillColor, Paint strokeColor, double lineWidth) {
        if (gc != null) {
            gc.setFont( font );
            gc.setFill( fillColor );
            gc.setStroke( strokeColor );
            gc.setLineWidth( lineWidth );
        }
    }

    public void setGCParams(GraphicsContext sourceGC) {
        if (sourceGC != null)
            setGCParams( sourceGC.getFont(), sourceGC.getFill(), sourceGC.getStroke(), sourceGC.getLineWidth() );
        else
            setGCParams();
    }

    public void todo(GraphicsContext gc) {
        MouseEvent event = null;
        ArrayList<Circle> listOfCircles = new ArrayList<>();
        for (Circle circle : listOfCircles) {
            Point2D point2D = new Point2D( event.getX(), event.getY() );
            if (circle.contains( point2D )) {
                log.debug( "circle clicked" );
            }
        }
    }

    @Override
    public Rectangle getBoundaries() {
        return boundaries;
    }

    // /**
    // * Draw or compute size of facet.
    // *
    // * @param facet
    // * @param gc if null, compute size
    // * @param font
    // * @param x
    // * @param y
    // * @param width set if computing size, not drawing
    // *
    // * @return
    // */
    // public Rectangle drawFacet(OtmFacet<?> facet, GraphicsContext gc, Font font, final double x, final double y,
    // double width) {
    // double height = 0;
    // Rectangle fr = null;
    //
    // if (!facet.getChildren().isEmpty()) {
    // // Get the size of the label
    // Rectangle fRect = GraphicsUtils.drawLabel( facet.getName(), facet.getIcon(), gc, font, x, y );
    // height += fRect.getHeight();
    // double py = y + fRect.getHeight() + GraphicsUtils.PROPERTY_MARGIN;
    // if (gc == null && fRect.getWidth() + GraphicsUtils.PROPERTY_MARGIN > width)
    // width = fRect.getWidth() + GraphicsUtils.PROPERTY_MARGIN;
    //
    // // Draw each property
    // for (OtmObject c : facet.getChildren()) {
    // if (c instanceof OtmProperty) {
    // fRect = GraphicsUtils.drawProperty( (OtmProperty) c, gc, font, x, py, width );
    // height += fRect.getHeight();
    // py += fRect.getHeight();
    // // Set width if computing size and not drawing
    // if (gc == null && fRect.getWidth() > width)
    // width = fRect.getWidth();
    //
    // if (gc != null) {
    // if (c instanceof OtmTypeUser)
    // fRect.setOnMouseClicked( e -> connect( ((OtmTypeUser) c), this, e.getX(), e.getY() ) );
    // rectangles.add( fRect );
    // }
    // }
    // }
    // // Draw vertical line
    // if (gc != null) {
    // gc.strokeLine( x + GraphicsUtils.PROPERTY_OFFSET, y + fRect.getHeight() - GraphicsUtils.PROPERTY_MARGIN,
    // x + GraphicsUtils.PROPERTY_OFFSET,
    // y + height - GraphicsUtils.PROPERTY_MARGIN - GraphicsUtils.LABEL_MARGIN );
    // }
    // }
    // fr = new Rectangle( x, y, width, height );
    // // log.debug( "Drew/sized facet " + facet.getName() + " into " + fr );
    // return fr;
    // }

    /**
     * Adds sprite for the member that provides the type then add connection to new sprite.
     * 
     * @param user
     * @param column which column to put the new sprite into
     */
    // TODO - why not pass in the property rectangle?
    @Override
    public void connect(OtmTypeUser user, DexSprite<?> from, double x, double y) {
        log.debug( "Connecting to " + user );
        if (user != null && user.getAssignedType() instanceof OtmTypeProvider) {
            OtmLibraryMember provider = user.getAssignedType().getOwningMember();

            Point2D p = manager.getNextRightColumn( this );
            DexSprite<?> newSprite = manager.add( provider, p.getX(), p.getY() );

            // Place the new sprite and connect it
            if (newSprite != null) {
                newSprite.refresh();

                // Find the rectangle and make type connection
                Rectangle fRect = from.find( x, y );
                if (fRect != null) {
                    TypeConnection c = new TypeConnection( fRect, from, newSprite );
                    manager.addAndDraw( c );
                }
            } else
                // Manager already has the member displayed, remove it
                manager.remove( provider );
        }
    }


    // public Rectangle drawFacets(OtmLibraryMember member, GraphicsContext gc, Font font, final double x, final double
    // y,
    // double width) {
    //
    // // Save to be restored
    // Paint color = null;
    // if (gc != null) {
    // color = gc.getFill();
    // // gc.setFill( Color.gray( 0.95 ) );
    // gc.setFill( Color.ANTIQUEWHITE );
    // }
    //
    // FacetRectangle rect = null;
    // double fx = x + GraphicsUtils.PROPERTY_OFFSET;
    // double fy = y + GraphicsUtils.FACET_MARGIN;
    // double fWidth =
    // width - GraphicsUtils.PROPERTY_OFFSET - GraphicsUtils.PROPERTY_MARGIN - GraphicsUtils.FACET_MARGIN;
    //
    // if (!collapsed) {
    // for (OtmObject child : member.getChildren())
    // if (child instanceof OtmFacet && !(child instanceof OtmContributedFacet)
    // && !((OtmFacet<?>) child).getChildren().isEmpty()) {
    // // Draw filled box
    // // size and position
    // rect = new FacetRectangle( (OtmFacet<?>) child, this, font );
    // rect.set( fx, fy );
    // rect.draw( gc, true );
    //
    // //
    // // OLD
    // //
    // // rect = drawFacet( (OtmFacet<?>) child, gc, font, fx, fy, fWidth );
    // // rect.draw( gc, false );
    // // rect.draw( gc, true );
    // // // Draw facet label and properties
    // // rect = drawFacet( (OtmFacet<?>) child, gc, font, fx, fy, fWidth );
    // double dx = rect.getX() - x + GraphicsUtils.PROPERTY_OFFSET;
    // if (gc == null && rect.getWidth() + dx > width)
    // width = rect.getWidth() + dx;
    // if (gc == null && rect.getWidth() > width)
    // width = rect.getWidth();
    // fy += rect.getHeight() + GraphicsUtils.FACET_MARGIN;
    // fx += 2;
    // fWidth -= 2;
    // }
    // // Do the contextual facets
    // }
    // if (gc != null)
    // gc.setFill( color );
    //
    // // TODO - track rectangle sizes
    // // TODO - aliases
    // // TODO - contributed and contextual facets
    // return new Rectangle( x, y, width, fy - y );
    // }
}