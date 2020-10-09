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

import org.opentravel.model.OtmObject;
import org.opentravel.model.otmLibraryMembers.OtmContextualFacet;
import org.opentravel.model.otmLibraryMembers.OtmLibraryMember;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

/**
 * Graphics Display Object (Sprite) for containing OTM business object.
 * 
 * @author dmh
 * @param <O>
 *
 */
public class ContextualFacetSprite extends MemberSprite<OtmLibraryMember> implements DexSprite<OtmLibraryMember> {
    // private static Log log = LogFactory.getLog( BusinessObjectSprite.class );

    public ContextualFacetSprite(OtmContextualFacet member, SpriteManager manager, GraphicsContext paramsGC) {
        super( member, manager, paramsGC );
    }

    @Override
    public Rectangle drawContents(GraphicsContext gc, Font font, final double x, final double y) {

        // // super.drawMember( gc, font );
        // double width = getBoundaries().getWidth();
        // // double height = 0;
        Rectangle cRect = new Rectangle( 0, 0, 0, 0 );
        List<OtmObject> kids = getMember().getChildren();

        // cRect = drawFacet( facet, gc, font, x, y, width )
        //
        // // Show facets
        // if (!isCollapsed()) {
        // mRect = drawFacets( getMember(), gc, font, x, y, width );
        // // if (mRect.getWidth() > width)
        // // width = mRect.getWidth();
        // // height = mRect.getHeight();
        // }
        //
        // // log.debug( "Drew contents into " + mRect );
        return cRect;
    }

}