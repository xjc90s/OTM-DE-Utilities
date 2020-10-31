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

package org.opentravel.dex.controllers.graphics.sprites.retangles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

/**
 * Graphics utility for containing regions (x, y, width, height). A rectangle does <b>not</b> have a canvas.
 * <p>
 * Sub-types have contents that can be drawn into the rectangle. These rectangles will compute their size when
 * constructed and when drawn with a null GraphicsContext (GC). A rectangle may be mouse click-able if the parent sprite
 * is passed when constructing the rectangle.
 * 
 * @author dmh
 * @param <O>
 *
 */
public class Rectangle {
    private static Log log = LogFactory.getLog( Rectangle.class );

    /**
     * Render methods that create rectangles may set the event to run if the implement this interface.
     * <p>
     * Example: r.setOnMouseClicked( e -> manager.remove( this ) );
     */
    public abstract interface RectangleEventHandler {
        public void onRectangleClick(MouseEvent e);
    }

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected RectangleEventHandler eventHandler = null;
    protected Point2D connectionPoint = null;

    public Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    /**
     * @param x2
     * @param y2
     * @return
     */
    public boolean contains(double x, double y) {
        return contains( new Point2D( x, y ) );
    }

    public boolean contains(Point2D point) {
        if (point.getX() < x)
            return false;
        if (point.getX() > getMaxX())
            return false;
        if (point.getY() < y)
            return false;
        if (point.getY() > getMaxY())
            return false;
        return true;
    }

    /**
     * Draw around the rectangle.
     * 
     * @param gc
     * @param filled
     */
    public Rectangle draw(GraphicsContext gc, boolean filled) {
        if (gc != null) {
            if (filled)
                gc.fillRect( x, y, width, height );
            else
                gc.strokeRect( x, y, width, height );
        }
        return this;
    }

    public Point2D getConnectionPoint() {
        return connectionPoint;
    }

    public Point2D moveConnectionPoint(double deltaX, double deltaY) {
        log.debug( "Connection point move: " + connectionPoint );
        if (connectionPoint != null)
            connectionPoint = connectionPoint.add( deltaX, deltaY );
        log.debug( "Connection point move: " + connectionPoint );
        return connectionPoint;
    }

    public double getHeight() {
        return height;
    }

    public double getMaxX() {
        return x + width;
    }

    public double getMaxY() {
        return y + height;
    }

    public double getWidth() {
        return width;
    }

    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }

    public final void onMouseClicked(MouseEvent e) {
        if (e != null && eventHandler != null)
            eventHandler.onRectangleClick( e );
        log.debug( "Mouse clicked. " + e.toString() );
    }

    public Rectangle set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Rectangle set(double x, double y, double width) {
        set( x, y );
        this.width = width;
        return this;
    }

    public final void setOnMouseClicked(RectangleEventHandler a) {
        // TEST
        eventHandler = a;
    }

    public String toString() {
        return "x = " + x + " y = " + y + " width = " + width + " height = " + height;
    }

    /**
     * Draw with filled set to false.
     * 
     * @param gc
     * @return
     */
    public Rectangle draw(GraphicsContext gc) {
        return draw( gc, false );
    }

}
