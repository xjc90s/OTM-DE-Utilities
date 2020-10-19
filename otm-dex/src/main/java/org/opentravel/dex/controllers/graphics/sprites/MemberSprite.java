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
import org.opentravel.dex.controllers.graphics.sprites.SettingsManager.Margins;
import org.opentravel.dex.controllers.graphics.sprites.connections.SuperTypeConnection;
import org.opentravel.dex.controllers.graphics.sprites.connections.TypeConnection;
import org.opentravel.dex.controllers.graphics.sprites.retangles.ColumnRectangle;
import org.opentravel.dex.controllers.graphics.sprites.retangles.PropertyRectangle;
import org.opentravel.dex.controllers.graphics.sprites.retangles.Rectangle;
import org.opentravel.dex.controllers.graphics.sprites.retangles.Rectangle.RectangleEventHandler;
import org.opentravel.model.OtmTypeUser;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;
import org.opentravel.model.otmProperties.OtmProperty;

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
    protected static Log log = LogFactory.getLog( MemberSprite.class );

    private static final double MIN_HEIGHT = 50;
    private static final double MIN_WIDTH = 50;

    private double x;
    private double y;
    protected M member;
    protected ColumnRectangle column;
    private Canvas canvas;
    protected GraphicsContext gc = null;

    private Rectangle boundaries = null;
    private boolean collapsed = false;

    private List<Rectangle> rectangles = new ArrayList<>();
    private SpriteManager manager;

    protected SettingsManager settingsManager;

    /**
     * Initialize member sprite. Create canvas and save GC for parameters.
     * 
     * @param member
     * @param settingsManager, must <b>not</b> be null.
     */
    public MemberSprite(M member, SpriteManager manager, SettingsManager settingsManager) {
        if (settingsManager == null)
            throw new IllegalArgumentException( "Must have settings" );

        this.member = member;
        this.manager = manager;
        this.settingsManager = settingsManager;
        //
        canvas = new Canvas( MIN_WIDTH, MIN_HEIGHT );
        gc = canvas.getGraphicsContext2D();
        setGCParams( settingsManager.getGc() );
        setBoundaries( 0, 0 );
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

    /**
     * Toggle collapsed state.
     */
    public void collapseOrExpand() {
        clear();
        collapsed = !collapsed;
        render();

    }

    /**
     * Return the width
     * 
     * @param compute - if true, return the larger. if false return the width
     * @param width
     * @param rect
     * @param offsetX - added to rectangle's width
     * @return
     */
    public double computeWidth(boolean compute, double width, Rectangle rect, double offsetX) {
        width = rect.getWidth() + offsetX > width ? rect.getWidth() + offsetX : width;
        return width;
    }

    public M getMember() {
        return member;
    }

    @Override
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    @Override
    public Canvas render(Point2D point) {
        set( point );
        return render();
    }

    @Override
    public Canvas render() {
        // log.debug( "Rendering at " + x + " " + y + " sprite for: " + member );
        if (member == null || manager == null)
            return null;

        setBoundaries( 0, 0 );

        // Create Canvas
        Rectangle canvasR = new Rectangle( x, y, boundaries.getWidth() + GraphicsUtils.CANVAS_MARGIN,
            boundaries.getHeight() + GraphicsUtils.CANVAS_MARGIN );
        canvas.setHeight( y + canvasR.getHeight() );
        canvas.setWidth( x + canvasR.getWidth() );
        // log.debug( "Sized canvas: " + canvasR );

        drawMember( true );
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
        Rectangle ms = drawMember( false );
        // Set the true boundaries
        boundaries = new Rectangle( x, y, width == 0 ? ms.getWidth() : width, height == 0 ? ms.getHeight() : height );
        // boundaries.draw( gc, false );
    }

    @Override
    public void refresh() {
        clear();
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

    public Rectangle drawContents(boolean render, final double x, final double y) {
        GraphicsContext dgc = null;
        if (render)
            dgc = gc;
        return drawContents( dgc, gc.getFont(), x, y );
    }

    /**
     * Draw background, label, controls and contents for this member.
     * 
     * @param render draw onto canvas if true, compute sizes if false
     * @return
     */
    public Rectangle drawMember(boolean render) {
        GraphicsContext dgc = null;
        if (render)
            dgc = gc;
        return drawMember( dgc, gc.getFont() );
    }

    /**
     * Draw background, label and controls.
     * 
     * @param gc
     * @param font
     * @return
     */
    private Rectangle drawMember(GraphicsContext gc, Font font) {
        if (member == null)
            return new Rectangle( 0, 0, 0, 0 );

        // Draw background box
        if (gc != null) {
            Rectangle bRect = new Rectangle( boundaries.getX(), boundaries.getY(),
                boundaries.getWidth() + settingsManager.getMargin( Margins.FACET ),
                boundaries.getHeight() + settingsManager.getMargin( Margins.FACET ) );
            bRect.draw( gc, false ); // Outline
            bRect.draw( gc, true ); // Fill
        }

        // Draw the name of the object
        Rectangle mRect =
            GraphicsUtils.drawLabel( member.getName(), member.getIcon(), member.isEditable(), false, gc, font, x, y );
        double width = mRect.getWidth();
        double height = mRect.getHeight();

        // Add the controls
        if (gc == null)
            width += 2 * 18;
        else
            drawControls( boundaries );

        // Draw property for base type if any
        if (!collapsed && member.getBaseType() != null) {
            mRect = new PropertyRectangle( this, member, width );
            mRect.set( boundaries.getMaxX() - mRect.getWidth(), y + height ).draw( gc, true );
            width = computeWidth( gc == null, width, mRect, 0 );
            height += mRect.getHeight();
        }

        // Show content (facets, properties, etc)
        mRect = drawContents( gc, font, boundaries.getX(), y + height );
        // mRect.draw( gc, false );
        if (gc == null && mRect.getWidth() + settingsManager.getMargin( Margins.FACET ) > width)
            width = mRect.getWidth() + settingsManager.getMargin( Margins.FACET );
        height += mRect.getHeight();


        // Handler for canvas layer
        if (manager != null) {
            canvas.setOnMouseDragged( manager::drag );
            canvas.setOnDragDetected( manager::dragStart );
            canvas.setOnMouseReleased( manager::dragEnd );
            // Clicks go to the top most node...so let the pane catch them
            // canvas.setOnMouseClicked( this::mouseClick );
        }
        // log.debug( "Refreshed " + member );
        mRect = new Rectangle( x, y, width, height );
        // mRect.draw( gc, false );
        return mRect;
    }

    @Override
    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public void onRectangleClick(MouseEvent e) {
        // log.debug( "Rectangle click at: " + e.getX() + " " + e.getY() );
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
    public PropertyRectangle find(OtmProperty property) {
        for (Rectangle r : rectangles)
            if (r instanceof PropertyRectangle && ((PropertyRectangle) r).getProperty() == property)
                return ((PropertyRectangle) r);
        return null;
    }

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

    @Override
    public void set(ColumnRectangle column) {
        this.column = column;
    }

    @Override
    @Deprecated
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void set(Point2D p) {
        if (p != null) {
            x = p.getX();
            y = p.getY();
        }
    }

    @Override
    public void setBackgroundColor(Color color) {
        gc.setFill( color );
    }

    @Override
    public void set(Font font) {
        gc.setFont( font );
    }

    @Override
    public void setCollapsed(boolean collapsed) {
        log.debug( "Collapsed = " + collapsed + " " + this );
        this.collapsed = collapsed;
        // resize this sprite
        setBoundaries( 0, 0 );
        // log.debug( " became = " + this );
        manager.updateConnections( this );
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
                // log.debug( "circle clicked" );
            }
        }
    }

    @Override
    public Rectangle getBoundaries() {
        return boundaries;
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public Font getFont() {
        if (gc.getFont() == null)
            return settingsManager.getFont();
        return gc.getFont();
    }

    @Override
    public Font getItalicFont() {
        return settingsManager.getItalicFont();
    }

    @Override
    public ColumnRectangle getColumn() {
        return column;
    }

    @Override
    public DexSprite<?> connect() {
        if (!(getMember().getBaseType() instanceof OtmLibraryMember))
            return null;

        DexSprite<?> baseSprite = manager.get( (OtmLibraryMember) member.getBaseType() );
        if (baseSprite == null) {
            baseSprite = manager.add( (OtmLibraryMember) member.getBaseType(), getColumn() );
        } else
            baseSprite.setCollapsed( !baseSprite.isCollapsed() );
        if (baseSprite != null) {
            manager.addAndDraw( new SuperTypeConnection( baseSprite, this ) );
            baseSprite.getCanvas().toFront();
            baseSprite.refresh();
        }
        return baseSprite;
    }

    @Override
    public DexSprite<?> connect(OtmTypeUser user) {
        // log.debug( "Connecting to " + user );
        if (user == null || user.getAssignedType() == null || !(user instanceof OtmProperty))
            return null;
        if (getColumn() == null)
            return null;

        OtmLibraryMember provider = user.getAssignedType().getOwningMember();
        if (provider == null || provider == user.getOwningMember())
            return null;

        DexSprite<?> toSprite = manager.get( provider );
        if (toSprite == null) {
            // Place the new sprite and connect it
            toSprite = manager.add( provider, getColumn().getNext() );
            connect( (OtmProperty) user, this, toSprite );
        } else {
            toSprite.setCollapsed( !toSprite.isCollapsed() );
        }
        if (toSprite != null) {
            toSprite.getCanvas().toFront();
            toSprite.refresh();
        }
        return toSprite;
    }

    /**
     * Find the property's rectangle and make type connection
     * 
     * @param property
     * @param from
     * @param to
     * @return
     */
    private TypeConnection connect(OtmProperty property, DexSprite<?> from, DexSprite<?> to) {
        TypeConnection c = null;
        Rectangle fRect;
        fRect = from.find( property );
        if (fRect != null) {
            c = new TypeConnection( fRect, from, to );
            manager.addAndDraw( c );
        }
        return c;
    }

    public String toString() {
        return "Sprite for " + getMember() + " at " + getBoundaries();
    }
}
