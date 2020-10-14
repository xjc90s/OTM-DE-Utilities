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

package org.opentravel.dex.controllers.graphics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.ToggleSwitch;
import org.opentravel.application.common.events.AbstractOtmEvent;
import org.opentravel.common.ImageManager;
import org.opentravel.common.ImageManager.Icons;
import org.opentravel.dex.controllers.DexIncludedControllerBase;
import org.opentravel.dex.controllers.DexMainController;
import org.opentravel.dex.controllers.graphics.sprites.DexSprite;
import org.opentravel.dex.controllers.graphics.sprites.SpriteManager;
import org.opentravel.dex.events.DexEvent;
import org.opentravel.dex.events.DexMemberSelectionEvent;
import org.opentravel.model.OtmObject;
import org.opentravel.model.OtmTypeUser;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;

import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 * Manage the Graphics display
 * 
 * @author dmh
 */
public class GraphicsCanvasController extends DexIncludedControllerBase<OtmObject> {
    private static Log log = LogFactory.getLog( GraphicsCanvasController.class );

    // private static final Font DEFAULT_FONT = new Font( "Arial", 14 );
    public static final Font DEFAULT_FONT = new Font( "Monospaced", 15 );
    private static final Paint DEFAULT_STROKE = Color.BLACK;
    static final Paint DEFAULT_FILL = Color.gray( 0.8 );

    private static final EventType[] subscribedEvents =
        {DexMemberSelectionEvent.MEMBER_SELECTED, DexMemberSelectionEvent.DOUBLE_CLICK_MEMBER_SELECTED};
    private static final EventType[] publishedEvents = {DexMemberSelectionEvent.MEMBER_SELECTED};

    @FXML
    private AnchorPane graphicsPane;
    @FXML
    private VBox graphicsVBox;

    private DexMainController parentController = null;
    private SpriteManager spriteManager;

    private Pane spritePane;
    private ScrollPane scrollPane = null;

    private Canvas backgroundCanvas;
    private GraphicsContext backgroundGC;

    private Canvas doodleCanvas = null;
    private GraphicsContext dgc = null;

    private boolean ignoreEvents = false;

    private boolean isLocked = false;
    private boolean tracking = true;

    public GraphicsCanvasController() {
        super( subscribedEvents, publishedEvents );
    }

    @Override
    public void checkNodes() {
        if (graphicsPane == null)
            throw new IllegalStateException( "Null pane in graphics controller." );
        // private Pane spriteArea;
        if (graphicsVBox == null)
            throw new IllegalStateException( "Null graphics vBox in graphics controller." );
    }

    @Override
    public void clear() {
        // clearCanvas();
        spriteManager.clear();
        backgroundGC.clearRect( 0, 0, backgroundCanvas.getWidth(), backgroundCanvas.getHeight() );
        // spriteManager.clear();
    }

    @Override
    public void configure(DexMainController parent, int viewGroupId) {
        super.configure( parent, viewGroupId );
        eventPublisherNode = graphicsPane;
        parentController = parent;

        createToolBar( graphicsVBox );

        spritePane = createSpritePane( graphicsVBox );

        // Create a background and bind the dimensions when the user resizes the window.
        backgroundCanvas = new Canvas();
        backgroundCanvas.widthProperty().bind( spritePane.widthProperty() );
        backgroundCanvas.heightProperty().bind( spritePane.heightProperty() );
        spritePane.getChildren().add( backgroundCanvas );
        backgroundGC = backgroundCanvas.getGraphicsContext2D();

        backgroundGC.setFont( DEFAULT_FONT );
        backgroundGC.setFill( DEFAULT_FILL );
        backgroundGC.setStroke( DEFAULT_STROKE );
        backgroundGC.setLineWidth( 1 );

        spriteManager = new SpriteManager( spritePane, this, backgroundGC );

        log.debug( "Configured graphics canvas." );
    }

    /**
     * Create sprite pane controlled by scroll pane.
     * <p>
     * fxml only has VBox with toolbar
     * 
     * @param parent
     * @return
     */
    private Pane createSpritePane(VBox parent) {
        // Create panes and add to parent
        spritePane = new Pane();
        scrollPane = new ScrollPane( spritePane );
        parent.getChildren().add( scrollPane );
        // Configure sizes
        VBox.setVgrow( scrollPane, Priority.ALWAYS );
        scrollPane.setMaxWidth( Double.MAX_VALUE );
        return spritePane;
    }

    private ToolBar createToolBar(VBox parent) {
        Button clearB = new Button( "Clear" );
        clearB.setOnAction( this::doClear );

        Button refreshB = new Button( "Refresh" );
        refreshB.setOnAction( this::doRefresh );

        Separator tSep = new Separator( Orientation.VERTICAL );
        ToggleSwitch trackS = new ToggleSwitch( "Track" );
        trackS.selectedProperty().addListener( (v, o, n) -> doTrack( n ) );

        Separator cSep = new Separator( Orientation.VERTICAL );
        ToggleSwitch collapseS = new ToggleSwitch( "Collapse" );
        collapseS.selectedProperty().addListener( (v, o, n) -> doCollapse( n ) );

        Separator lockSep = new Separator( Orientation.VERTICAL );
        ToggleSwitch lockS = new ToggleSwitch( "Lock" );
        ImageView lockI = ImageManager.get( Icons.LOCK );
        lockS.selectedProperty().addListener( (v, o, n) -> doLock( n ) );

        Separator fontSep = new Separator( Orientation.VERTICAL );
        Label fontL = new Label( "Font" );
        Slider fontS = new Slider( 8, 24, 14 ); // Min, max, current
        fontS.valueProperty().addListener( (v, o, n) -> doFont( n ) );

        Separator dSep = new Separator( Orientation.VERTICAL );
        ToggleSwitch doodleS = new ToggleSwitch( "Draw" );
        doodleS.selectedProperty().addListener( (v, o, n) -> doDoodle( n ) );

        ColorPicker colorP = new ColorPicker();
        colorP.setOnAction( this::doColor );

        ToolBar tb = new ToolBar( clearB, refreshB, tSep, trackS, cSep, collapseS, lockSep, lockS, lockI, fontSep,
            fontL, fontS, colorP, dSep, doodleS );
        parent.getChildren().add( tb );
        tb.setStyle( "-fx-background-color: #7cafc2" );
        return tb;
    }

    public void doClear(ActionEvent e) {
        clear();
    }

    public void doCollapse(boolean collapse) {
        spriteManager.setCollapsed( collapse );
    }

    public void doColor(ActionEvent e) {
        if (e.getTarget() instanceof ColorPicker && spriteManager != null) {
            spriteManager.update( ((ColorPicker) e.getTarget()).getValue() );
        }
    }

    /**
     * Let user annotate (doodle) on the drawing surface
     * 
     * @param run
     */
    private void doDoodle(boolean run) {
        if (run) {
            log.debug( "Starting doodle." );
            if (doodleCanvas == null) {
                doodleCanvas =
                    new Canvas( graphicsVBox.getWidth(), spritePane.getHeight() > 0 ? spritePane.getHeight() : 1000 );
                spritePane.getChildren().add( doodleCanvas );
            }
            dgc = doodleCanvas.getGraphicsContext2D();
            // if (colorPicker != null && colorPicker.getValue() != null)
            // dgc.setStroke( colorPicker.getValue() );
            // else
            dgc.setStroke( Color.DARKRED );
            dgc.setLineWidth( 4 );

            doodleCanvas.setOnMouseDragged( e -> {
                log.debug( " doodle drag" );
                dgc.lineTo( e.getX(), e.getY() );
                dgc.stroke();
            } );
            doodleCanvas.setOnMousePressed( e -> {
                log.debug( " doodle point" );
                dgc.beginPath();
                dgc.lineTo( e.getX(), e.getY() );
                dgc.stroke();
            } );
        } else {
            if (dgc != null) {
                dgc.setFill( backgroundGC.getFill() );
                dgc.setStroke( backgroundGC.getStroke() );
                dgc.fillRect( 0, 0, doodleCanvas.getWidth(), doodleCanvas.getHeight() );
                dgc.setLineWidth( 1 );
                spritePane.getChildren().remove( doodleCanvas );
                doodleCanvas = null;
            }
        }
    }

    public void doFont(Number v) {
        log.debug( "Change font size to: " + v.intValue() );
        spriteManager.update( v.intValue() );
    }

    public void doLock(boolean lock) {
        isLocked = lock;
    }

    public void doRefresh(ActionEvent e) {
        log.debug( "do refresh." );
        spriteManager.update( Color.gray( 0.9 ) );
    }

    public void doTrack(boolean track) {
        this.tracking = track;
    }

    @Override
    public void handleEvent(AbstractOtmEvent event) {
        // log.debug( "Event received: " + event.getEventType() );
        if (!isLocked) {
            if (!tracking && event.getEventType() == DexMemberSelectionEvent.DOUBLE_CLICK_MEMBER_SELECTED)
                post( ((DexMemberSelectionEvent) event).getMember() );
            else if (tracking && event instanceof DexMemberSelectionEvent)
                post( ((DexMemberSelectionEvent) event).getMember() );
            else
                refresh();
        }
    }

    @Override
    @FXML
    public void initialize() {
        log.debug( "Graphics canvas controller initialized." );
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void post(OtmObject o) {
        log.debug( "Graphics canvas controller posting object: " + o );
        if (o instanceof OtmLibraryMember) {
            OtmLibraryMember member = (OtmLibraryMember) o;
            if (tracking)
                spriteManager.clear();
            DexSprite<?> s = spriteManager.add( member );
            if (s != null) {
                if (tracking) {
                    // Display the type providers
                    double dx = s.getBoundaries().getX() + s.getBoundaries().getWidth() / 2;
                    double dy = s.getBoundaries().getY() + s.getBoundaries().getHeight() / 2;
                    DexSprite<?> newS = null;
                    for (OtmTypeUser user : member.getDescendantsTypeUsers()) {
                        newS = s.connect( user, s, dx, dy );
                        if (newS != null)
                            newS.setCollapsed( true );
                    }
                    scrollPane.setVvalue( 0 );
                    scrollPane.setHvalue( 0 );
                } else {
                    // Scroll to the new sprite's location
                    double dx = s.getBoundaries().getX() / backgroundCanvas.getWidth();
                    double dy = s.getBoundaries().getMaxY() / backgroundCanvas.getHeight();
                    scrollPane.setVvalue( dy );
                    scrollPane.setHvalue( dx );
                }
            }
            spriteManager.refresh();
        }

    }

    @Override
    public void publishEvent(DexEvent event) {
        ignoreEvents = true;
        fireEvent( event );
        ignoreEvents = false;
    }
}
